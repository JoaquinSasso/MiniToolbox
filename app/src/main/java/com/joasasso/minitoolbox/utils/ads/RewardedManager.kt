package com.joasasso.minitoolbox.utils.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.joasasso.minitoolbox.metrics.adImpression
import java.util.concurrent.atomic.AtomicBoolean

object RewardedManager {

    private const val TAG = "RewardedManager"
    private var rewarded: RewardedAd? = null

    private var adUnitId: String? = null
    private val isLoading = AtomicBoolean(false)
    private val isShowing = AtomicBoolean(false)
    private var lastLoadTs = 0L
    private const val LOAD_COOLDOWN_MS = 5_000L

    /**
     * IMPORTANTE: pasar siempre una Activity (Unity y otros adapters la requieren también para LOAD).
     */
    fun init(context : Context, adUnitId: String) {
        this.adUnitId = adUnitId
        val activity = (context as Activity)
        if (rewarded == null) load(activity)
    }

    /**
     * Carga un Rewarded con Activity (no usar applicationContext).
     */
    fun load(activity: Activity) {
        val id = adUnitId ?: run {
            Log.w(TAG, "No adUnitId set; skipping load()")
            return
        }

        val now = System.currentTimeMillis()
        if (isLoading.get() || now - lastLoadTs < LOAD_COOLDOWN_MS) return
        isLoading.set(true)
        lastLoadTs = now

        val req = AdRequest.Builder().build()

        RewardedAd.load(
            activity,
            id,
            req,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewarded = ad
                    isLoading.set(false)
                    val adapter = ad.responseInfo.mediationAdapterClassName
                    Log.d(TAG, "Rewarded loaded")
                    Log.i("Ads", "Rewarded loaded by adapter: $adapter")

                    // Aseguramos callbacks de pantalla completa (para recarga/metricas)
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            // Métrica de impresión (mantiene tu comportamiento original)
                            adImpression(activity.applicationContext, "rewarded")
                        }
                        override fun onAdDismissedFullScreenContent() {
                            rewarded = null
                            // Precarga el siguiente anuncio usando Activity (no applicationContext)
                            load(activity)
                        }
                        override fun onAdFailedToShowFullScreenContent(err: AdError) {
                            Log.e(TAG, "Rewarded failed to show: code=${err.code} msg=${err.message}")
                            rewarded = null
                            load(activity)
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewarded = null
                    isLoading.set(false)
                    Log.e(TAG, "Rewarded failed to load: code=${error.code} message=${error.message}")
                    // Si fue NO_FILL (3), dejamos que el cooldown regule los reintentos.
                }
            }
        )
    }

    /**
     * Muestra el anuncio si hay uno disponible. Si no, ejecuta onUnavailable().
     * Siempre exige Activity (necesario para show y consistente con load).
     */
    fun show(
        activity: Activity,
        onReward: (RewardItem) -> Unit,
        onUnavailable: (() -> Unit)?
    ): Boolean {
        if (!isShowing.compareAndSet(false, true)) {
            onUnavailable?.invoke()
            return false
        }

        val ad = rewarded
        if (ad == null) {
            isShowing.set(false)
            onUnavailable?.invoke()
            // Reintenta precargar con Activity
            load(activity)
            return false
        }

        InterstitialManager.notifyRewardedShown()
        // (El fullScreenContentCallback ya se setea en onAdLoaded para asegurar consistencia)
        ad.show(activity) { reward ->
            try {
                onReward(reward)
            } catch (t: Throwable) {
                Log.e(TAG, "onReward callback threw", t)
            } finally {
                // El callback de dismissed/failed hará la recarga y limpiará estados.
                isShowing.set(false)
            }
        }

        // Evita dobles shows con la misma instancia
        rewarded = null
        return true
    }
}
