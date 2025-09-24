package com.joasasso.minitoolbox

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.joasasso.minitoolbox.metrics.appOpen
import com.joasasso.minitoolbox.metrics.dailyOpenOnce
import com.joasasso.minitoolbox.metrics.uploader.UploadConfig
import com.joasasso.minitoolbox.metrics.uploader.UploadScheduler
import com.joasasso.minitoolbox.ui.ads.AdPosition
import com.joasasso.minitoolbox.ui.ads.GlobalAdsLayer
import com.joasasso.minitoolbox.ui.theme.MiniToolboxTheme
import com.joasasso.minitoolbox.utils.ads.ConsentGateProvider
import com.joasasso.minitoolbox.utils.ads.LocalConsentState
import com.joasasso.minitoolbox.utils.pro.LocalProState
import com.joasasso.minitoolbox.utils.pro.ProStateProvider

class MainActivity : AppCompatActivity() {

    private var startRouteState: String? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        startRouteState = intent.getStringExtra("startRoute")

        Log.d("Metrics", "endpoint=" + UploadConfig.getEndpoint(this))
        Log.d("Metrics", "apiKey=" + UploadConfig.getApiKey(this).take(6) + "…")

        setContent {
            ConsentGateProvider {
                LaunchedEffect(Unit) {
                    // Configurar una vez
                    UploadConfig.set(
                        applicationContext,
                        endpoint = "https://us-central1-minitoolbox-7ab7d.cloudfunctions.net/ingest",
                        apiKey = "cc8af2654262d35c72dde40bbb42480b8f60b3a89dfed56e09368f4633187767"
                    )

                    // Registrar apertura y "open once" del día
                    appOpen(applicationContext)
                    dailyOpenOnce(applicationContext)

                    // 🔸 Planificador con cotas:
                    // Al superar 3 aperturas (o si ya hay un payload pendiente), encola envío inmediato
                    UploadScheduler.maybeFlushOnThreshold(
                        applicationContext,
                        UploadScheduler.FlushThreshold(appOpens = 3, tools = Int.MAX_VALUE, ads = Int.MAX_VALUE)
                    )
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
