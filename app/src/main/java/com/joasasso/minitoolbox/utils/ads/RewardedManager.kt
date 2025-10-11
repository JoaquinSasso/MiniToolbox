package com.joasasso.minitoolbox.utils.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
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
     * IMPORTANTE: usar siempre una Activity (no applicationContext),
     * porque Unity y algunos adapters la requieren incluso para el load.
     */
    fun init(activity: Activity, adUnitId: String) {
        this.adUnitId = adUnitId
        if (rewarded == null) load(activity)
    }

    fun load(activity: Activity) {
        val id = adUnitId ?: run {
            Log.w(TAG, "No adUnitId set; skipping load()")
            return
        }
        val now = System.currentTimeMillis()
        if (isLoading.get() || now - lastLoadTs < LOAD_COOLDOWN_MS) return

        isLoading.set(true)
        lastLoadTs = now
        val request = AdRequest.Builder().build()

        RewardedAd.load(
            activity,
            id,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewarded = ad
                    isLoading.set(false)
                    val adapter = ad.responseInfo?.mediationAdapterClassName
                    Log.d(TAG, "Rewarded loaded")
                    Log.i("Ads", "Rewarded loaded by adapter: $adapter")

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            adImpression(activity.applicationContext, "rewarded")
                        }

                        override fun onAdDismissedFullScreenContent() {
                            rewarded = null
                            load(activity) // precarga el siguiente
                        }

                        override fun onAdFailedToShowFullScreenContent(err: AdError) {
                            Log.e(TAG, "Failed to show: ${err.code} ${err.message}")
                            rewarded = null
                            load(activity)
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewarded = null
                    isLoading.set(false)
                    Log.e(TAG, "Rewarded failed to load: code=${error.code} message=${error.message}")
                }
            }
        )
    }

    /**
     * Muestra el anuncio si hay uno disponible.
     * Si no hay, ejecuta onUnavailable() y recarga automáticamente.
     */
    fun show(
        activity: Activity,
        onReward: (RewardItem) -> Unit,
        onUnavailable: (() -> Unit)? = null
    ): Boolean {
        if (!isShowing.compareAndSet(false, true)) {
            onUnavailable?.invoke()
            return false
        }

        val ad = rewarded
        if (ad == null) {
            isShowing.set(false)
            onUnavailable?.invoke()
            load(activity)
            return false
        }

        // 🔸 Notifica al InterstitialManager que se mostró un Rewarded
        try {
            InterstitialManager.notifyRewardedShown()
        } catch (e: Throwable) {
            Log.w(TAG, "notifyRewardedShown() failed: ${e.message}")
        }

        ad.show(activity) { reward ->
            try {
                onReward(reward)
            } catch (t: Throwable) {
                Log.e(TAG, "onReward callback error", t)
            } finally {
                isShowing.set(false)
            }
        }

        // Evita doble uso del mismo objeto
        rewarded = null
        return true
    }
}
