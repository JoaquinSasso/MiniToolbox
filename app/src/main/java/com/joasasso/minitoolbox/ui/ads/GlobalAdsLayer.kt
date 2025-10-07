package com.joasasso.minitoolbox.ui.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity

enum class AdPosition { Top, Bottom }

/**
 * Dibuja un banner global y empuja el contenido con padding
 * para que nada quede tapado. Soporta Top o Bottom.
 */
@Composable
fun GlobalAdsLayer(
    shouldShowAds: Boolean,
    adUnitId: String,
    position: AdPosition = AdPosition.Bottom,
    content: @Composable () -> Unit
) {
    var hPx by remember { mutableIntStateOf(0) }
    val hDp = with(LocalDensity.current) { hPx.toDp() }

    Box(Modifier.fillMaxSize()) {
        // 1) Contenido con padding global según posición
        val padMod = when {
            shouldShowAds && position == AdPosition.Top -> Modifier.padding(top = hDp)
            shouldShowAds && position == AdPosition.Bottom -> Modifier.padding(bottom = hDp)
            else -> Modifier
        }
        Box(Modifier.fillMaxSize().then(padMod)) {
            content()
        }

        // 2) Banner superpuesto
        if (shouldShowAds) {
            val align = if (position == AdPosition.Top) Alignment.TopCenter else Alignment.BottomCenter
            val sysInsets = if (position == AdPosition.Top) WindowInsets.statusBars else WindowInsets.navigationBars

            Box(
                Modifier
                    .align(align)
                    .fillMaxWidth()
                    // Evita solaparse con barra de estado o de navegación
                    .windowInsetsPadding(sysInsets)
            ) {
                BannerAd(
                    adUnitId = adUnitId,
                    visible = true,
                    onHeightChange = { hPx = it } // nos da la altura real en px
                )
            }
        }
    }
}
