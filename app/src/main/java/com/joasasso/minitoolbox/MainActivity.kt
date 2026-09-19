package com.joasasso.minitoolbox

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.rememberNavController
import com.joasasso.minitoolbox.metrics.appOpen
import com.joasasso.minitoolbox.metrics.dailyOpenOnce
import com.joasasso.minitoolbox.metrics.MetricsConfig
import com.joasasso.minitoolbox.metrics.MetricsSource
import com.joasasso.minitoolbox.metrics.PendingEntrySource
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
        installSplashScreen()
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        startRouteState = intent?.getStringExtra(Screen.EXTRA_START_ROUTE_JSON)
            ?: intent?.getStringExtra("startRoute")

        val isMetricsConfigured = MetricsConfig.isConfigured
        Log.d("Metrics", "metrics configured: $isMetricsConfigured")

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
                    if (isMetricsConfigured) {
                        // Registrar apertura y "open once" del día
                        appOpen(applicationContext)
                        dailyOpenOnce(applicationContext)

                        // 🔸 Planificador con cotas
                        UploadScheduler.maybeFlushOnThreshold(
                            applicationContext,
                            UploadScheduler.FlushThreshold(appOpens = 1, tools = Int.MAX_VALUE, ads = Int.MAX_VALUE)
                        )
                    }
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_STOP) {
                            if (isMetricsConfigured) {
                                UploadScheduler.enqueueNowExpedited(
                                    applicationContext,
                                    MetricsConfig.endpoint
                                )
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
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
                            val raw = startRoute
                            if (!raw.isNullOrBlank()) {
                                val targetScreen = Screen.fromJson(raw) ?: Screen.fromRouteString(raw)
                                if (targetScreen != null && targetScreen != Screen.Categories) {
                                    try {
                                        PendingEntrySource.set(
                                            intent?.getStringExtra(MetricsSource.EXTRA_START_SOURCE)
                                        )
                                        widgetUse(applicationContext, "widget_shortcuts")

                                        navController.popBackStack<Screen.Categories>(inclusive = false)

                                        navController.navigate(targetScreen) {
                                            launchSingleTop = true
                                            restoreState = false
                                        }
                                    } catch (e: Exception) {
                                        Log.e("Navigation", "Error navegando a destino: $raw", e)
                                    } finally {
                                        startRoute = null
                                    }
                                } else {
                                    startRoute = null
                                }
                            }
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
        val newRoute = intent.getStringExtra(Screen.EXTRA_START_ROUTE_JSON)
            ?: intent.getStringExtra("startRoute")
        // Actualiza Compose state → dispara LaunchedEffect(startRoute)
        pushStartRoute?.invoke(newRoute)
    }
}