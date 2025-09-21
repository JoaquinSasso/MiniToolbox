package com.joasasso.minitoolbox

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.joasasso.minitoolbox.metrics.appOpen
import com.joasasso.minitoolbox.metrics.dailyOpenOnce
import com.joasasso.minitoolbox.ui.ads.AdPosition
import com.joasasso.minitoolbox.ui.ads.GlobalAdsLayer
import com.joasasso.minitoolbox.ui.theme.MiniToolboxTheme
import com.joasasso.minitoolbox.utils.ads.ConsentGateProvider
import com.joasasso.minitoolbox.utils.ads.LocalConsentState
import com.joasasso.minitoolbox.utils.pro.LocalProState
import com.joasasso.minitoolbox.utils.pro.ProStateProvider

class MainActivity : AppCompatActivity() {

    // Ruta inicial opcional enviada por widgets / deep links
    private var startRouteState: String? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        startRouteState = intent.getStringExtra("startRoute")

        setContent {
            ConsentGateProvider {
                LaunchedEffect(Unit) {
                    appOpen(applicationContext)          // suma cada vez que se abre la app
                    dailyOpenOnce(applicationContext)    // 1 vez por día
                }
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
                            MiniToolboxNavGraph(
                                navController = navController,
                                shouldShowAds = shouldShowAds,
                                interstitialAdUnitId = if (BuildConfig.DEBUG)
                                    getString(R.string.admob_interstitial_test)
                                else
                                    getString(R.string.admob_interstitial_prod),
                                rewardedAdUnitId = if (BuildConfig.DEBUG)
                                    getString(R.string.admob_rewarded_test)
                                else
                                    getString(R.string.admob_rewarded_prod)
                            )

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
