package com.joasasso.minitoolbox

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.compose.rememberNavController
import com.joasasso.minitoolbox.ui.ads.AdPosition
import com.joasasso.minitoolbox.ui.ads.GlobalAdsLayer
import com.joasasso.minitoolbox.ui.theme.MiniToolboxTheme
import com.joasasso.minitoolbox.utils.ConsentGateProvider
import com.joasasso.minitoolbox.utils.LocalConsentState
import com.joasasso.minitoolbox.utils.LocalProState
import com.joasasso.minitoolbox.utils.ProStateProvider

class MainActivity : AppCompatActivity() {

    // Ruta inicial opcional enviada por widgets / deep links
    private var startRouteState: String? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        startRouteState = intent.getStringExtra("startRoute")

        // IDs de banner (debug/prod). Se usan más abajo en GlobalAdsLayer
        val adUnitIdDebug = getString(R.string.admob_banner_test)
        val adUnitIdProd  = getString(R.string.admob_banner_prod)

        setContent {
            ConsentGateProvider {
                ProStateProvider {
                    MiniToolboxTheme {
                        val navController = rememberNavController()

                        val pro = LocalProState.current
                        val consent = LocalConsentState.current
                        val shouldShowAds = !pro.isPro && consent.canRequestAds

                        GlobalAdsLayer(
                            shouldShowAds = shouldShowAds,
                            position = AdPosition.Top,
                            adUnitId = if (BuildConfig.DEBUG)
                                getString(R.string.admob_banner_test)
                            else
                                getString(R.string.admob_banner_prod)
                        ) {
                            MiniToolboxNavGraph(navController = navController)
                        }
                    }
                }
            }

        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        startRouteState = intent.getStringExtra("startRoute")
    }
}
