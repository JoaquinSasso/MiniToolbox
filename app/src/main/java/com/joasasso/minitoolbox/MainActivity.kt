package com.joasasso.minitoolbox

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.ReportDrawnWhen
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavDestination.Companion.hasRoute
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

class MainActivity : AppCompatActivity() {

    // bridge para comunicar onNewIntent -> Compose
    private var pushStartRoute: ((String?) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        val startRouteState = intent?.getStringExtra(Screen.EXTRA_START_ROUTE_JSON)
            ?: intent?.getStringExtra("startRoute")

        // Destino pedido por el intent de arranque (widget, notificación, atajo). Se
        // resuelve antes de setContent para que el NavHost arranque directamente en él.
        // Si la Activity se recrea, NavController restaura su propia pila.
        val initialTarget = if (savedInstanceState == null) parseStartRoute(startRouteState) else null
        if (initialTarget != null) recordEntrySource(intent)

        val isMetricsConfigured = MetricsConfig.isConfigured
        Log.d("Metrics", "metrics configured: $isMetricsConfigured")

        setContent {
            LockScreenOrientationIfAllowed(
                orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                enabled = true
            )
            LaunchedEffect(Unit) {
                if (isMetricsConfigured) {
                    // Registrar apertura y "open once" del día
                    appOpen(applicationContext)
                    dailyOpenOnce(applicationContext)

                    // 🔸 Planificador con cotas
                    UploadScheduler.maybeFlushOnThreshold(
                        applicationContext,
                        UploadScheduler.FlushThreshold(appOpens = 1, tools = Int.MAX_VALUE)
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

            MiniToolboxTheme {
                val navController = rememberNavController()
                // Rutas que llegan por onNewIntent con la Activity ya creada. La del
                // arranque ya se aplicó como pila inicial del NavHost.
                var startRoute by rememberSaveable { mutableStateOf<String?>(null) }

                // Exponer el setter al Activity (onNewIntent lo usa)
                DisposableEffect(Unit) {
                    pushStartRoute = { newRoute -> startRoute = newRoute }
                    onDispose { pushStartRoute = null }
                }
                MiniToolboxNavGraph(
                    navController = navController,
                    initialDestination = initialTarget
                )

                // Time to full display: la pantalla que el usuario pidió está sola en
                // pantalla, sin transición en curso.
                val visibleEntries by navController.visibleEntries.collectAsState()
                ReportDrawnWhen {
                    val destination = visibleEntries.singleOrNull()?.destination
                        ?: return@ReportDrawnWhen false
                    initialTarget == null || destination.hasRoute(initialTarget::class)
                }

                LaunchedEffect(startRoute) {
                    val raw = startRoute
                    if (!raw.isNullOrBlank()) {
                        val targetScreen = parseStartRoute(raw)
                        if (targetScreen != null) {
                            try {
                                recordEntrySource(intent)

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

    /** Destino de un extra de arranque; `null` si falta, es inválido o es el catálogo. */
    private fun parseStartRoute(raw: String?): Screen? {
        if (raw.isNullOrBlank()) return null
        return (Screen.fromJson(raw) ?: Screen.fromRouteString(raw))
            ?.takeIf { it != Screen.Categories }
    }

    /** Atribuye la próxima visita al origen del intent y cuenta el uso del widget. */
    private fun recordEntrySource(intent: Intent?) {
        val source = intent?.getStringExtra(MetricsSource.EXTRA_START_SOURCE)
        PendingEntrySource.set(source)
        MetricsSource.deepLinkWidgetKey(
            source,
            intent?.getStringExtra(MetricsSource.EXTRA_START_WIDGET)
        )?.let { widgetUse(applicationContext, it) }
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