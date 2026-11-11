// app/src/main/java/com/example/minitoolbox/NavGraph.kt
package com.joasasso.minitoolbox

import android.app.Activity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.joasasso.minitoolbox.dev.MetricsDiagnosticsScreen
import com.joasasso.minitoolbox.metrics.PendingEntrySource
import com.joasasso.minitoolbox.metrics.TOOL_USAGE_METRIC_COOLDOWN_MS
import com.joasasso.minitoolbox.metrics.ToolRoutes
import com.joasasso.minitoolbox.metrics.toolUse
import com.joasasso.minitoolbox.nav.Screen
import com.joasasso.minitoolbox.tools.ToolRegistry
import com.joasasso.minitoolbox.tools.entretenimiento.MarcadorEquiposScreen
import com.joasasso.minitoolbox.tools.entretenimiento.TrucoScoreBoardScreen
import com.joasasso.minitoolbox.tools.entretenimiento.aleatorio.OptionSelectorScreen
import com.joasasso.minitoolbox.tools.entretenimiento.minijuegos.AdivinaBanderaScreen
import com.joasasso.minitoolbox.tools.entretenimiento.minijuegos.minesweeper.MinesweeperScreen
import com.joasasso.minitoolbox.tools.herramientas.calculadoras.ConversorUnidadesScreen
import com.joasasso.minitoolbox.tools.herramientas.generadores.GeneradorQrScreen
import com.joasasso.minitoolbox.tools.herramientas.generadores.GroupSelectorScreen
import com.joasasso.minitoolbox.tools.herramientas.instrumentos.BrujulaScreen
import com.joasasso.minitoolbox.tools.herramientas.instrumentos.BubbleLevelScreen
import com.joasasso.minitoolbox.tools.herramientas.instrumentos.FlashScreen
import com.joasasso.minitoolbox.tools.herramientas.instrumentos.LightSensorScreen
import com.joasasso.minitoolbox.tools.herramientas.instrumentos.MagnifierScreen
import com.joasasso.minitoolbox.tools.herramientas.instrumentos.ReglaScreen
import com.joasasso.minitoolbox.tools.herramientas.instrumentos.arruler.ArRulerSceneViewScreen
import com.joasasso.minitoolbox.tools.organizacion.divisorGastos.AgregarGastoScreen
import com.joasasso.minitoolbox.tools.organizacion.divisorGastos.CrearReunionScreen
import com.joasasso.minitoolbox.tools.organizacion.divisorGastos.DetallesReunionScreen
import com.joasasso.minitoolbox.tools.organizacion.divisorGastos.EditarGastoScreen
import com.joasasso.minitoolbox.tools.organizacion.divisorGastos.ReunionesScreen
import com.joasasso.minitoolbox.tools.organizacion.pomodoro.PomodoroScreen
import com.joasasso.minitoolbox.tools.organizacion.pomodoro.PomodoroTimersListScreen
import com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua.AguaReminderScreen
import com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua.AguaStatisticsScreen
import com.joasasso.minitoolbox.utils.ToolDebouncer

@Composable
fun MiniToolboxNavGraph(navController: NavHostController) {
    val animationDuration = 280
    val context = LocalContext.current

    val metricsDebouncer = remember { ToolDebouncer(cooldownMs = TOOL_USAGE_METRIC_COOLDOWN_MS) }

    // Herramienta en la que está el usuario (null fuera de una). Es saveable para que
    // recrear la Activity no cuente otra visita a la misma herramienta.
    var currentToolKey by rememberSaveable { mutableStateOf<String?>(null) }
    val backStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(backStackEntry?.destination?.route) {
        val route = backStackEntry?.destination?.route ?: return@LaunchedEffect
        val key = ToolRoutes.usageKey(route)
        val toCount = ToolRoutes.visitToCount(currentToolKey, key)
        currentToolKey = key
        if (toCount != null) {
            // El origen se consume en toda visita nueva, aunque el debouncer la descarte,
            // para que no se atribuya a la herramienta siguiente.
            val source = PendingEntrySource.consume()
            if (metricsDebouncer.canExecute(toCount)) {
                toolUse(context, toCount, source)
            }
        }
    }

    val onBackSmart: () -> Unit = {
        val popped = navController.popBackStack()
        if (!popped) (context as? Activity)?.finish()
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Categories,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(animationDuration, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                targetOffset = { (it * 0.35f).toInt() },
                animationSpec = tween(animationDuration, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(animationDuration))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                initialOffset = { -(it * 0.35f).toInt() },
                animationSpec = tween(animationDuration, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(animationDuration))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                targetOffset = { it },
                animationSpec = tween(animationDuration, easing = FastOutSlowInEasing)
            )
        },
        predictivePopEnterTransition = { _ ->
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                initialOffset = { -(it * 0.35f).toInt() },
                animationSpec = tween(animationDuration, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(animationDuration))
        },
        predictivePopExitTransition = { _ ->
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                targetOffset = { it },
                animationSpec = tween(animationDuration, easing = FastOutSlowInEasing)
            )
        }
    ) {
        composable<Screen.Categories> {
            CategoriesScreen(
                tools = ToolRegistry.tools,
                onToolClick = { tool -> navController.navigate(tool.screen) },
                onNavigateToAbout = { navController.navigate(Screen.About) }
            )
        }

        composable<Screen.GroupSelector> {
            GroupSelectorScreen(onBack = onBackSmart)
        }
        composable<Screen.TrucoScoreboard> {
            TrucoScoreBoardScreen(onBack = onBackSmart)
        }
        composable<Screen.PomodoroList> {
            PomodoroTimersListScreen(
                onBack = { navController.popBackStack() },
                onOpenTimer = { timer ->
                    navController.navigate(Screen.PomodoroDetail(timerId = timer.id))
                }
            )
        }
        composable<Screen.PomodoroDetail> { backStackEntry ->
            val detail = backStackEntry.toRoute<Screen.PomodoroDetail>()
            PomodoroScreen(timerId = detail.timerId, onBack = { navController.popBackStack() })
        }
        composable<Screen.BubbleLevel> {
            BubbleLevelScreen(onBack = onBackSmart)
        }
        composable<Screen.UnitConverter> {
            ConversorUnidadesScreen(onBack = onBackSmart)
        }
        composable<Screen.QrGenerator> {
            GeneradorQrScreen(onBack = onBackSmart)
        }
        composable<Screen.Ruler> {
            ReglaScreen(onBack = onBackSmart)
        }
        composable<Screen.LightMeter> {
            LightSensorScreen(onBack = onBackSmart)
        }
        composable<Screen.Flashlight> {
            FlashScreen(onBack = onBackSmart)
        }
        composable<Screen.Water> {
            AguaReminderScreen(
                onBack = onBackSmart,
                onShowEstadisticas = {
                    navController.navigate(Screen.WaterStats)
                }
            )
        }
        composable<Screen.WaterStats> {
            AguaStatisticsScreen(onBack = onBackSmart)
        }
        composable<Screen.SelectorWheel> {
            OptionSelectorScreen(onBack = onBackSmart)
        }
        composable<Screen.GuessFlag> {
            AdivinaBanderaScreen(onBack = onBackSmart)
        }
        composable<Screen.Meetings> {
            ReunionesScreen(
                onBack = onBackSmart,
                onCrearReunion = { navController.navigate(Screen.MeetingCreate) },
                onReunionClick = { reunion ->
                    navController.navigate(Screen.MeetingDetail(reunionId = reunion.id))
                }
            )
        }
        composable<Screen.MeetingCreate> {
            CrearReunionScreen(
                onBack = onBackSmart,
                onReunionCreada = { reunionId ->
                    navController.navigate(Screen.MeetingDetail(reunionId = reunionId)) {
                        popUpTo<Screen.Meetings> {
                            inclusive = false
                        }
                    }
                }
            )
        }
        composable<Screen.MeetingDetail> { backStackEntry ->
            val detail = backStackEntry.toRoute<Screen.MeetingDetail>()
            DetallesReunionScreen(
                onBack = onBackSmart,
                reunionId = detail.reunionId,
                onEditarGasto = { idReunion, idGasto ->
                    navController.navigate(Screen.ExpenseEdit(reunionId = idReunion, gastoId = idGasto))
                },
                onAgregarGasto = { idReunion ->
                    navController.navigate(Screen.ExpenseAdd(reunionId = idReunion))
                }
            )
        }
        composable<Screen.ExpenseEdit> { _ ->
            EditarGastoScreen(
                onBack = onBackSmart
            )
        }
        composable<Screen.ExpenseAdd> { _ ->
            AgregarGastoScreen(
                onBack = onBackSmart
            )
        }
        composable<Screen.Compass> {
            BrujulaScreen(onBack = onBackSmart)
        }
        composable<Screen.Scoreboard> {
            MarcadorEquiposScreen(onBack = onBackSmart)
        }
        composable<Screen.Magnifier> {
            MagnifierScreen(onBack = onBackSmart)
        }
        composable<Screen.ArRuler> {
            ArRulerSceneViewScreen(onBack = onBackSmart)
        }
        composable<Screen.About> {
            val licensesTitle = stringResource(R.string.about_licenses_button)
            AboutScreen(
                onBack = onBackSmart,
                onOpenLicenses = {
                    com.google.android.gms.oss.licenses.OssLicensesMenuActivity.setActivityTitle(
                        licensesTitle
                    )
                    context.startActivity(
                        android.content.Intent(context, com.google.android.gms.oss.licenses.OssLicensesMenuActivity::class.java)
                    )
                },
                onOpenDevTools = { navController.navigate(Screen.DevMetrics) }
            )
        }
        composable<Screen.DevMetrics> {
            MetricsDiagnosticsScreen(onBack = onBackSmart)
        }
        composable<Screen.Minesweeper> {
            MinesweeperScreen(onBack = onBackSmart)
        }
    }
}
