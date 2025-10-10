package com.joasasso.minitoolbox.utils.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

object RewardedManager {

    private const val TAG = "RewardedManager"

    /** Si true → usa Rewarded Interstitial; si false → usa Rewarded clásico */
    var useRewardedInterstitial: Boolean = false

    private var rewarded: RewardedAd? = null
    private var rewardedInterstitial: RewardedInterstitialAd? = null

    private var adUnitId: String? = null
    private val isLoading = AtomicBoolean(false)
    private val isShowing = AtomicBoolean(false)
    private var lastLoadTs = 0L
    private const val LOAD_COOLDOWN_MS = 5_000L

    fun init(context: Context, adUnitId: String, useRI: Boolean) {
        this.adUnitId = adUnitId
        this.useRewardedInterstitial = useRI
        load(context)
    }

    fun load(context: Context) {
        val id = adUnitId ?: return
        val now = System.currentTimeMillis()
        if (isLoading.get() || now - lastLoadTs < LOAD_COOLDOWN_MS) return
        isLoading.set(true)
        lastLoadTs = now

        val req = AdRequest.Builder().build()

        if (useRewardedInterstitial) {
            // REWARDED INTERSTITIAL
            RewardedInterstitialAd.load(
                context, id, req,
                object : RewardedInterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedInterstitialAd) {
                        rewardedInterstitial = ad
                        rewarded = null
                        isLoading.set(false)
                        val name = ad.responseInfo.mediationAdapterClassName
                        Log.d(TAG, "RewardedInterstitial loaded")
                        Log.i("Ads", "RewardedInterstitial loaded by adapter: $name")
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewardedInterstitial = null
                        isLoading.set(false)
                        Log.e(TAG, "RewardedInterstitial failed to load: code=${error.code} message=${error.message}")
                    }
                }
            )
        } else {
            // REWARDED CLÁSICO
            RewardedAd.load(
                context, id, req,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewarded = ad
                        rewardedInterstitial = null
                        isLoading.set(false)
                        val name = ad.responseInfo.mediationAdapterClassName
                        Log.d(TAG, "Rewarded loaded")
                        Log.i("Ads", "Rewarded loaded by adapter: $name")
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewarded = null
                        isLoading.set(false)
                        Log.e(TAG, "Rewarded failed to load: code=${error.code} message=${error.message}")
                    }
                }
            )
        }
    }

    /**
     * Muestra el anuncio si hay uno disponible. Si no, ejecuta onUnavailable().
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

        val onFinally: () -> Unit = {
            isShowing.set(false)
            load(activity.applicationContext)
        }

        if (useRewardedInterstitial) {
            val ad = rewardedInterstitial
            if (ad == null) {
                isShowing.set(false)
                onUnavailable?.invoke()
                load(activity.applicationContext)
                return false
            }
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() { /* track if needed */ }
                override fun onAdDismissedFullScreenContent() {
                    rewardedInterstitial = null
                    onFinally()
                }
                override fun onAdFailedToShowFullScreenContent(err: AdError) {
                    rewardedInterstitial = null
                    onUnavailable?.invoke()
                    onFinally()
                }
            }
            ad.show(activity) { reward -> onReward(reward) }
            rewardedInterstitial = null
            return true
        } else {
            val ad = rewarded
            if (ad == null) {
                isShowing.set(false)
                onUnavailable?.invoke()
                load(activity.applicationContext)
                return false
            }
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() { /* track if needed */ }
                override fun onAdDismissedFullScreenContent() {
                    rewarded = null
                    onFinally()
                }
                override fun onAdFailedToShowFullScreenContent(err: AdError) {
                    rewarded = null
                    onUnavailable?.invoke()
                    onFinally()
                }
            }
            ad.show(activity) { reward -> onReward(reward) }
            rewarded = null
            return true
        }
    }
}
