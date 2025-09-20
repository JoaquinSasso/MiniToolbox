package com.joasasso.minitoolbox

import MetricsConsentAfterAdsGate
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.joasasso.minitoolbox.metrics.storage.AggregatesRepository
import com.joasasso.minitoolbox.ui.ads.AdPosition
import com.joasasso.minitoolbox.ui.ads.GlobalAdsLayer
import com.joasasso.minitoolbox.ui.theme.MiniToolboxTheme
import com.joasasso.minitoolbox.utils.ads.ConsentGateProvider
import com.joasasso.minitoolbox.utils.ads.LocalConsentState
import com.joasasso.minitoolbox.utils.pro.LocalProState
import com.joasasso.minitoolbox.utils.pro.ProStateProvider
import isEeaOrUk

class MainActivity : AppCompatActivity() {

    // Ruta inicial opcional enviada por widgets / deep links
    private var startRouteState: String? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        startRouteState = intent.getStringExtra("startRoute")
        val aggregates = AggregatesRepository(applicationContext)

        // IDs de banner (debug/prod). Se usan más abajo en GlobalAdsLayer
        val adUnitIdDebug = getString(R.string.admob_banner_test)
        val adUnitIdProd  = getString(R.string.admob_banner_prod)

        kotlinx.coroutines.runBlocking {
            val fired = aggregates.dailyOpenIfNeeded()
            // si fired == true, aquí podrías también loguear un evento local JSONL (más adelante)
        }


        setContent {
            ConsentGateProvider {
                // Cuando el gate “terminó”, ya podemos consultar UMP y sembrar default
                LaunchedEffect(Unit) {
                    val eea = isEeaOrUk(applicationContext)
                    aggregates.seedMetricsDefaultIfUndecided(isEeaOrUk = eea)
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
                            MetricsConsentAfterAdsGate()
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
