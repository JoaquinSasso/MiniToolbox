// InterstitialManager.kt
package com.joasasso.minitoolbox.utils.ads

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

object InterstitialManager {
    // Config default
    var showEveryNOpens: Int = 3
    var graceFirstOpens: Int = 2
    var minCooldownMs: Long = 30_000L

    private var adUnitId: String? = null
    private var interstitialAd: InterstitialAd? = null
    private val isLoading = AtomicBoolean(false)

    // --- PERSISTENCIA LIVIANA ---
    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences("ads_pacing", Context.MODE_PRIVATE)

    private fun incToolOpenCount(ctx: Context): Int {
        val p = prefs(ctx)
        val newVal = p.getInt("tool_open_count", 0) + 1
        p.edit { putInt("tool_open_count", newVal) }
        return newVal
    }

    private fun canPassCooldown(ctx: Context): Boolean {
        val p = prefs(ctx)
        val last = p.getLong("last_interstitial_ms", 0L)
        return (System.currentTimeMillis() - last) >= minCooldownMs
    }

    private fun markShownNow(ctx: Context) {
        prefs(ctx).edit { putLong("last_interstitial_ms", System.currentTimeMillis()) }
    }

    fun init(context: Context, adUnit: String) {
        this.adUnitId = adUnit
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
                    // opcional: reintentar en X tiempo
                }
            }
        )
    }

    /**
     * Llamala cada vez que el usuario entra a una tool.
     * Decide si mostrar o no, sin necesidad de pasar contadores externos.
     */
    fun onToolOpened(activity: Activity, shouldShowAds: Boolean) {
        if (!shouldShowAds) return

        val ctx = activity.applicationContext
        val total = incToolOpenCount(ctx)

        if (showEveryNOpens <= 0) return
        if (total < graceFirstOpens) return
        if (total % showEveryNOpens != 0) return
        if (!canPassCooldown(ctx)) return

        val ad = interstitialAd ?: return
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                load(ctx)
            }
            override fun onAdFailedToShowFullScreenContent(err: AdError) {
                interstitialAd = null
                load(ctx)
            }
            override fun onAdShowedFullScreenContent() {
                // Marcar cool-down
                markShownNow(ctx)
                // Si querés registrar la impresión de métricas aquí:
                // adImpression(ctx, "interstitial")
            }
        }
        ad.show(activity)
        interstitialAd = null // consumir referencia
    }
}
