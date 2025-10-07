package com.joasasso.minitoolbox.utils.ads

import android.app.Activity
import android.content.Context
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
    private var rewardedAd: RewardedAd? = null
    private var adUnitId: String? = null
    private val isLoading = AtomicBoolean(false)

    fun init(context: Context, adUnitId: String) {
        this.adUnitId = adUnitId
        if (rewardedAd == null) load(context)
    }

    fun load(context: Context) {
        val id = adUnitId ?: return
        if (isLoading.get()) return
        isLoading.set(true)
        RewardedAd.load(
            context,
            id,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading.set(false)
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoading.set(false)
                }
            }
        )
    }

    /**
     * Mostrar el rewarded (para pruebas). Si no está listo, desencadena carga.
     * onReward: callback donde podrías acreditar la recompensa (por ahora solo log).
     */
    fun show(activity: Activity, onReward: (RewardItem) -> Unit = {}) {
        val ad = rewardedAd
        if (ad == null) {
            load(activity.applicationContext)
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                load(activity.applicationContext)
            }
            override fun onAdFailedToShowFullScreenContent(err: AdError) {
                rewardedAd = null
                load(activity.applicationContext)
            }
            override fun onAdShowedFullScreenContent() {
                adImpression(activity.applicationContext, "interstitial")
            }
        }
        ad.show(activity) { rewardItem ->
            onReward(rewardItem) // acá podés llamar a AdsMetrics.rewarded(...)
        }
        rewardedAd = null
    }
}
