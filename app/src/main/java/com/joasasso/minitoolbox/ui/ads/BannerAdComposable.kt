package com.joasasso.minitoolbox.ui.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.joasasso.minitoolbox.utils.ads.AdsManager

@Composable
fun BannerAd(
    adUnitId: String,
    visible: Boolean,
    onHeightChange: (Int) -> Unit = {}
) {
    if (!visible) {
        LaunchedEffect(Unit) { onHeightChange(0) }
        return
    }

    val context = LocalContext.current
    val density = LocalDensity.current.density
    var adWidthPx by remember { mutableIntStateOf(0) }

    Box(
        Modifier
            .fillMaxWidth()
            .onGloballyPositioned { adWidthPx = it.size.width }
    ) {
        if (adWidthPx > 0) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    AdView(ctx).apply {
                        setAdUnitId(adUnitId)
                        val adWidthDp = (adWidthPx / density).toInt().coerceAtLeast(320)
                        setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, adWidthDp))

                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                onHeightChange(adSize?.getHeightInPixels(ctx) ?: 0)
                            }
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                onHeightChange(0)
                            }
                        }

                        loadAd(AdsManager.buildRequest())
                    }
                },
                update = { adView ->
                    val adWidthDp = (adWidthPx / density).toInt().coerceAtLeast(320)
                    val newSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp)
                    if (adView.adSize != newSize) {
                        adView.setAdSize(newSize)
                        adView.loadAd(AdsManager.buildRequest())
                    }
                }
            )
        }
    }
}


@Composable
fun BottomBannerHost(
    shouldShowAds: Boolean,
    adUnitId: String,
    content: @Composable (PaddingValues) -> Unit
) {
    var bottomInsetPx by remember { mutableIntStateOf(0) }
    val bottomInsetDp = with(LocalDensity.current) { bottomInsetPx.toDp() }

    Scaffold(
        bottomBar = {
            BannerAd(
                adUnitId = adUnitId,
                visible = shouldShowAds,
                onHeightChange = { hPx -> bottomInsetPx = hPx }
            )
        }
    ) { innerPadding ->
        val extraPadding = PaddingValues(
            start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
            top = innerPadding.calculateTopPadding(),
            end = innerPadding.calculateEndPadding(LayoutDirection.Ltr),
            bottom = innerPadding.calculateBottomPadding() + bottomInsetDp
        )
        content(extraPadding)
    }
}
