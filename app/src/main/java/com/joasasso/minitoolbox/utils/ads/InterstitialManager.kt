// InterstitialManager.kt (fragmento clave de gating)
package com.joasasso.minitoolbox.utils.ads

import android.app.Activity
import android.content.Context
import androidx.core.content.edit
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object InterstitialManager {
    private const val PREFS = "interstitial_prefs"
    private const val KEY_LAST_AD_TS = "last_interstitial_ts"

    // Ajustables:
    private const val MIN_OPENS_BETWEEN_ADS = 4          // muestra 1 cada 4 accesos válidos
    private const val GLOBAL_AD_COOLDOWN_MS = 120_000L   // 2 min entre interstitials

    private var interstitial: InterstitialAd? = null
    private var adUnitId: String? = null
    private var isLoading = false

    private var lastRewardedShownTs = 0L

    fun notifyRewardedShown() {
        lastRewardedShownTs = System.currentTimeMillis()
    }

    fun init(context: Context, adUnitId: String) {
        this.adUnitId = adUnitId
        if (interstitial == null) load(context)
    }

    private fun load(context: Context) {
        val id = adUnitId ?: return
        if (isLoading) return
        isLoading = true
        InterstitialAd.load(
            context, id, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    isLoading = false
                }
                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    interstitial = null
                    isLoading = false
                }
            }
        )
    }

    /**
     * Llama esto cuando se registró un "nuevo acceso" de tool (con cooldown por tool ya aplicado).
     * Solo intentará mostrar ad si cumple reglas (aperturas mínimas y cooldown global).
     */
    fun onToolOpened(activity: Activity, shouldShowAds: Boolean) {
        if (!shouldShowAds) return

        val ctx = activity.applicationContext
        val opens = ToolUsageTracker.getGlobalOpenCount(ctx)
        val now = System.currentTimeMillis()
        val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastTs = sp.getLong(KEY_LAST_AD_TS, 0L)

        val openGate = (opens % MIN_OPENS_BETWEEN_ADS) == 0
        val timeGate = (now - lastTs) > GLOBAL_AD_COOLDOWN_MS

        val skipBecauseRewarded = (now - lastRewardedShownTs) < 30_000L // 30 s de margen

        if (skipBecauseRewarded) {
            load(ctx)
            return
        }

        if (openGate && timeGate && interstitial != null) {
            interstitial?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitial = null
                    sp.edit { putLong(KEY_LAST_AD_TS, System.currentTimeMillis()) }
                    load(ctx)
                }
                override fun onAdFailedToShowFullScreenContent(err: com.google.android.gms.ads.AdError) {
                    interstitial = null
                    load(ctx)
                }
                override fun onAdShowedFullScreenContent() {
                    // opcional: métricas/impressions
                }
            }
            interstitial?.show(activity)
            interstitial = null
        } else {
            // Si no pasa el gate o no hay anuncio cargado, asegurá precarga
            load(ctx)
        }
    }
}
