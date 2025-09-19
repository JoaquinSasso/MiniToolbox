package com.joasasso.minitoolbox.utils.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

object InterstitialManager {
    // Config
    var showEveryNOpens: Int = 5 // cámbialo a 3 si querés testear agresivo
    private var adUnitId: String? = null

    // State
    private var interstitialAd: InterstitialAd? = null
    private val isLoading = AtomicBoolean(false)

    fun init(context: Context, adUnitId: String) {
        this.adUnitId = adUnitId
        if (interstitialAd == null) load(context)
    }

    fun load(context: Context) {
        val id = adUnitId ?: return
        if (isLoading.get()) return
        isLoading.set(true)
        InterstitialAd.load(
            context,
            id,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading.set(false)
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading.set(false)
                    // Podés reintentar más tarde automáticamente si querés
                }
            }
        )
    }

    /**
     * Llama esto al abrir una tool. Decide si mostrar según el total y disponibilidad.
     * Devuelve true si mostró, false si no.
     */
    fun maybeShow(activity: Activity, totalToolOpens: Int, shouldShowAds: Boolean): Boolean {
        if (!shouldShowAds) return false
        if (showEveryNOpens <= 0) return false
        if (totalToolOpens <= 0) return false
        if (totalToolOpens % showEveryNOpens != 0) return false

        val ad = interstitialAd ?: return false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                // Preload el siguiente
                load(activity.applicationContext)
            }
            override fun onAdFailedToShowFullScreenContent(err: AdError) {
                interstitialAd = null
                load(activity.applicationContext)
            }
            override fun onAdShowedFullScreenContent() {
                // Nada
            }
        }
        ad.show(activity)
        interstitialAd = null // consumir referencia
        return true
    }
}
