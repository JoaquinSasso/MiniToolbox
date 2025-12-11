package com.joasasso.minitoolbox

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.joasasso.minitoolbox.metrics.appOpen
import com.joasasso.minitoolbox.metrics.dailyOpenOnce
import com.joasasso.minitoolbox.metrics.toolUse
import com.joasasso.minitoolbox.metrics.uploader.UploadConfig
import com.joasasso.minitoolbox.metrics.uploader.UploadScheduler
import com.joasasso.minitoolbox.metrics.widgetUse
import com.joasasso.minitoolbox.nav.Screen
import com.joasasso.minitoolbox.ui.theme.MiniToolboxTheme
import com.joasasso.minitoolbox.ui.utils.LockScreenOrientationIfAllowed
import com.joasasso.minitoolbox.utils.ads.ConsentGateProvider
import com.joasasso.minitoolbox.utils.ads.LocalConsentState
import com.joasasso.minitoolbox.utils.pro.LocalProState
import com.joasasso.minitoolbox.utils.pro.ProSilentInitializer
import com.joasasso.minitoolbox.utils.pro.ProStateProvider

class MainActivity : AppCompatActivity() {

    private var startRouteState: String? = null
    // bridge para comunicar onNewIntent -> Compose
    private var pushStartRoute: ((String?) -> Unit)? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        startRouteState = intent?.getStringExtra("startRoute")

        Log.d("Metrics", "endpoint=" + UploadConfig.getEndpoint(this))
        Log.d("Metrics", "apiKey=" + UploadConfig.getApiKey(this).take(6) + "…")

        // 🔹 Inicialización / restauración silenciosa del estado PRO
        ProSilentInitializer.init(
            context = applicationContext,
            inappProductId = getString(R.string.billing_pro_id)
        )

        setContent {
            LockScreenOrientationIfAllowed(
                orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                enabled = true
            )
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

                    // 🔸 Planificador con cotas
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

                        // Estado Compose de la ruta inicial
                        var startRoute by rememberSaveable { mutableStateOf(startRouteState) }

                        // Exponer el setter al Activity (onNewIntent lo usa)
                        DisposableEffect(Unit) {
                            pushStartRoute = { newRoute -> startRoute = newRoute }
                            onDispose { pushStartRoute = null }
                        }
                        MiniToolboxNavGraph(
                            navController = navController,
                            shouldShowAds = if (BuildConfig.DEBUG) true else shouldShowAds,
                            interstitialAdUnitId = if (BuildConfig.DEBUG)
                                getString(R.string.admob_interstitial_test)
                            else
                                getString(R.string.admob_interstitial_prod),
                            rewardedAdUnitId = if (BuildConfig.DEBUG)
                                getString(R.string.admob_rewarded_test)
                            else
                                getString(R.string.admob_rewarded_prod)
                        )

                        LaunchedEffect(startRoute) {
                            val route = startRoute
                            if (Screen.isValidRoute(route) && route != Screen.Categories.route) {
                                toolUse(applicationContext, route!!)
                                widgetUse(applicationContext, "widget_shortcuts")

                                // opcional: solo si esa entry existe
                                navController.popBackStack(Screen.Categories.route, inclusive = false)

                                navController.navigate(route) {
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }
                            // siempre limpiar al final (evita re-navegaciones)
                            startRoute = null
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ProSilentInitializer.recheckOnForeground()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val newRoute = intent.getStringExtra("startRoute")
        // Actualiza Compose state → dispara LaunchedEffect(startRoute)
        pushStartRoute?.invoke(newRoute)
    }
}
