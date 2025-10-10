package com.joasasso.minitoolbox.utils.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.joasasso.minitoolbox.metrics.adImpression
import java.util.concurrent.atomic.AtomicBoolean

object RewardedManager {

    private var rewardedAd: RewardedInterstitialAd? = null
    private var adUnitId: String? = null
    private val isLoading = AtomicBoolean(false)
    private val isShowing = AtomicBoolean(false)
    private var lastLoadTs = 0L
    private const val LOAD_COOLDOWN_MS = 5_000L // evita spamear cargas

    fun init(context: Context, adUnitId: String) {
        this.adUnitId = adUnitId
        if (rewardedAd == null) load(context)
    }

    fun load(context: Context) {
        val id = adUnitId ?: return
        val now = System.currentTimeMillis()
        if (isLoading.get() || now - lastLoadTs < LOAD_COOLDOWN_MS) return
        isLoading.set(true)
        lastLoadTs = now

        RewardedInterstitialAd.load(
            context,
            id,
            AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedAd = ad
                    isLoading.set(false)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoading.set(false)
                    // Si es NO_FILL, reintenta más tarde; no saturar
                }
            }
        )
    }

    /**
     * Intenta mostrar el anuncio. Devuelve true si se intentó mostrar, false si no había anuncio.
     * onUnavailable: para fallback inmediato (ej: permitir usar la tool o mostrar toast).
     */
    fun show(
        activity: Activity,
        onReward: (RewardItem) -> Unit = {},
        onUnavailable: (() -> Unit)? = null
    ): Boolean {
        val ad = rewardedAd
        if (ad == null) {
            // No hay anuncio listo = típico NO_FILL -> regalo el uso
            load(activity.applicationContext)         // reintenta precargar
            onUnavailable?.invoke()                   // PASE LIBRE
            return false
        }
        if (!isShowing.compareAndSet(false, true)) {
            // Evita doble show accidental: doy pase libre para no trabar la UX
            onUnavailable?.invoke()
            return false
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                isShowing.set(false)
                rewardedAd = null
                load(activity.applicationContext)     // precarga próxima
            }
            override fun onAdFailedToShowFullScreenContent(err: AdError) {
                // Falló al abrir (p.ej. crash del creativo/playable) -> pase libre
                isShowing.set(false)
                rewardedAd = null
                load(activity.applicationContext)
                onUnavailable?.invoke()               // PASE LIBRE
            }
            override fun onAdShowedFullScreenContent() {
                adImpression(activity.applicationContext, "rewarded")
            }
        }

        InterstitialManager.notifyRewardedShown()

        ad.show(activity) { rewardItem ->
            onReward(rewardItem)                      // recompensa normal
        }
        rewardedAd = null
        return true
    }
}

