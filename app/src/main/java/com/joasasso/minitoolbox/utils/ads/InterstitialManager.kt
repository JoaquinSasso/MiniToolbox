// InterstitialManager.kt
package com.joasasso.minitoolbox.utils.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.joasasso.minitoolbox.metrics.adImpression
import com.joasasso.minitoolbox.utils.pro.ProRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference

object InterstitialManager {
    private const val TAG = "InterstitialManager"

    private const val PREFS = "interstitial_prefs"
    private const val KEY_LAST_AD_TS = "last_interstitial_ts"

    // Ajustables:
    private const val MIN_OPENS_BETWEEN_ADS = 3         // muestra 1 cada 3 accesos válidos
    private const val GLOBAL_AD_COOLDOWN_MS = 90_000L   // 90 seg entre interstitials

    private var interstitial: InterstitialAd? = null
    private var adUnitId: String? = null
    private var isLoading = false

    private var lastRewardedShownTs = 0L

    private var lastActivityRef: WeakReference<Activity>? = null
    private var pendingLoadUntilActivity: Boolean = false

    fun notifyRewardedShown() {
        lastRewardedShownTs = System.currentTimeMillis()
    }

    fun init(context: Context, adUnitId: String) {
        this.adUnitId = adUnitId

        // Si el context es Activity, guardamos referencia y cargamos ya.
        if (context is Activity) {
            lastActivityRef = WeakReference(context)
            load(context) // Activity → safe para Unity y resto
        } else {
            // Application context: deferimos la carga hasta tener una Activity
            pendingLoadUntilActivity = true
            Log.d(TAG, "init() con app Context; difiero carga hasta recibir una Activity.")
        }
    }

    // Mantengo la firma original. Internamente, si no hay Activity, difiere.
    private fun load(context: Context) {
        val id = adUnitId ?: return
        if (isLoading) return

        // Priorizar Activity para evitar errores de Unity.
        val activityForLoad: Activity? = when (context) {
            is Activity -> context.also { lastActivityRef = WeakReference(it) }
            else -> lastActivityRef?.get()
        }

        if (activityForLoad == null) {
            // No tenemos Activity todavía → marcamos pendiente y salimos.
            pendingLoadUntilActivity = true
            Log.d(TAG, "load() diferido: no hay Activity disponible para cargar el interstitial.")
            return
        }

        isLoading = true
        pendingLoadUntilActivity = false

        InterstitialAd.load(
            /* context = */ activityForLoad,
            /* adUnitId = */ id,
            /* adRequest = */ AdRequest.Builder().build(),
            /* loadCallback = */ object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    isLoading = false
                    Log.i(TAG, "Interstitial loaded by adapter: ${ad.responseInfo?.mediationAdapterClassName}")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitial = null
                    isLoading = false
                    Log.e(TAG, "Interstitial failed to load: code=${error.code} message=${error.message}")
                }
            }
        )
    }

    /**
     * Llama esto cuando se registró un "nuevo acceso" de tool (con cooldown por tool ya aplicado).
     * Solo intentará mostrar ad si cumple reglas (aperturas mínimas y cooldown global).
     */
    fun onToolOpened(activity: Activity, shouldShowAds: Boolean) {
        val isPro = runBlocking {
            ProRepository.isProFlow(activity).first()
        }
        if (!shouldShowAds || isPro) return

        // Actualizamos la Activity viva para futuras cargas.
        lastActivityRef = WeakReference(activity)

        // Si había una carga pendiente por falta de Activity, intentamos ahora.
        if (pendingLoadUntilActivity && interstitial == null && !isLoading) {
            load(activity)
        }

        val ctx = activity.applicationContext
        val opens = ToolUsageTracker.getGlobalOpenCount(ctx)
        val now = System.currentTimeMillis()
        val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastTs = sp.getLong(KEY_LAST_AD_TS, 0L)

        val openGate = (opens % MIN_OPENS_BETWEEN_ADS) == 0
        val timeGate = (now - lastTs) > GLOBAL_AD_COOLDOWN_MS

        val skipBecauseRewarded = (now - lastRewardedShownTs) < 60_000L // 60 s de margen

        if (skipBecauseRewarded) {
            load(activity) // precarga con Activity para mantener buffer listo
            return
        }

        if (openGate && timeGate && interstitial != null) {
            interstitial?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitial = null
                    sp.edit { putLong(KEY_LAST_AD_TS, System.currentTimeMillis()) }
                    load(activity) // precarga próxima con Activity
                }
                override fun onAdFailedToShowFullScreenContent(err: AdError) {
                    interstitial = null
                    load(activity)
                }
                override fun onAdShowedFullScreenContent() {
                    adImpression(activity.applicationContext, "interstitial")
                }
            }
            interstitial?.show(activity)
            interstitial = null
        } else {
            // Si no pasa el gate o no hay anuncio cargado, asegurá precarga
            load(activity)
        }
    }
}
