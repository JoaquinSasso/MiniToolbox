// app/src/main/java/com/example/minitoolbox/NavGraph.kt
package com.joasasso.minitoolbox

import ZodiacSignScreen
import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.joasasso.minitoolbox.dev.MetricsDevScreen
import com.joasasso.minitoolbox.nav.Screen
import com.joasasso.minitoolbox.tools.ToolRegistry
import com.joasasso.minitoolbox.tools.entretenimiento.MarcadorEquiposScreen
import com.joasasso.minitoolbox.tools.entretenimiento.TrucoScoreBoardScreen
import com.joasasso.minitoolbox.tools.entretenimiento.aleatorio.CoinFlipScreen
import com.joasasso.minitoolbox.tools.entretenimiento.aleatorio.LanzadorDadosScreen
import com.joasasso.minitoolbox.tools.entretenimiento.aleatorio.OptionSelectorScreen
import com.joasasso.minitoolbox.tools.entretenimiento.minijuegos.AdivinaBanderaScreen
import com.joasasso.minitoolbox.tools.entretenimiento.minijuegos.AdivinaCapitalScreen
import com.joasasso.minitoolbox.tools.entretenimiento.minijuegos.CalculosRapidosScreen
import com.joasasso.minitoolbox.tools.entretenimiento.minijuegos.InOtherWoldScreen
import com.joasasso.minitoolbox.tools.herramientas.calculadoras.ConversorHorasScreen
import com.joasasso.minitoolbox.tools.herramientas.calculadoras.ConversorRomanosScreen
import com.joasasso.minitoolbox.tools.herramientas.calculadoras.ConversorUnidadesScreen
import com.joasasso.minitoolbox.tools.herramientas.calculadoras.DecimalBinaryConverterScreen
import com.joasasso.minitoolbox.tools.herramientas.calculadoras.IMCScreen
import com.joasasso.minitoolbox.tools.herramientas.calculadoras.InteresCompuestoScreen
import com.joasasso.minitoolbox.tools.herramientas.calculadoras.PorcentajeScreen
import com.joasasso.minitoolbox.tools.herramientas.generadores.GeneradorContrasenaScreen
import com.joasasso.minitoolbox.tools.herramientas.generadores.GeneradorLoremIpsumScreen
import com.joasasso.minitoolbox.tools.herramientas.generadores.GeneradorNombresScreen
import com.joasasso.minitoolbox.tools.herramientas.generadores.GeneradorQrContactoScreen
import com.joasasso.minitoolbox.tools.herramientas.generadores.GeneradorQrScreen
import com.joasasso.minitoolbox.tools.herramientas.generadores.GroupSelectorScreen
import com.joasasso.minitoolbox.tools.herramientas.generadores.RandomColorGeneratorScreen
import com.joasasso.minitoolbox.tools.herramientas.generadores.noiseGenerator.WhiteNoiseScreen
import com.joasasso.minitoolbox.tools.herramientas.instrumentos.ArRulerSceneViewScreen
import com.joasasso.minitoolbox.tools.herramientas.instrumentos.BrujulaScreen
import com.joasasso.minitoolbox.tools.herramientas.instrumentos.BubbleLevelScreen
import com.joasasso.minitoolbox.tools.herramientas.instrumentos.FlashScreen
import com.joasasso.minitoolbox.tools.herramientas.instrumentos.LightSensorScreen
import com.joasasso.minitoolbox.tools.herramientas.instrumentos.MagnifierScreen
import com.joasasso.minitoolbox.tools.herramientas.instrumentos.ReglaScreen
import com.joasasso.minitoolbox.tools.info.AgeCalculatorScreen
import com.joasasso.minitoolbox.tools.info.CountriesInfoScreen
import com.joasasso.minitoolbox.tools.info.RemainingTimeScreen
import com.joasasso.minitoolbox.tools.info.TextBinaryConverterScreen
import com.joasasso.minitoolbox.tools.organizacion.SugeridorActividadScreen
import com.joasasso.minitoolbox.tools.organizacion.divisorGastos.AgregarGastoScreen
import com.joasasso.minitoolbox.tools.organizacion.divisorGastos.CrearReunionScreen
import com.joasasso.minitoolbox.tools.organizacion.divisorGastos.DetallesReunionScreen
import com.joasasso.minitoolbox.tools.organizacion.divisorGastos.EditarGastoScreen
import com.joasasso.minitoolbox.tools.organizacion.divisorGastos.ReunionesScreen
import com.joasasso.minitoolbox.tools.organizacion.pomodoro.PomodoroScreen
import com.joasasso.minitoolbox.tools.organizacion.recordatorios.EventosImportantesScreen
import com.joasasso.minitoolbox.tools.organizacion.recordatorios.HabitTrackerScreen
import com.joasasso.minitoolbox.tools.organizacion.recordatorios.ToDoListScreen
import com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua.AguaReminderScreen
import com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua.AguaStatisticsScreen
import com.joasasso.minitoolbox.ui.screens.BasicPhrasesScreen
import com.joasasso.minitoolbox.ui.screens.ProScreen
import com.joasasso.minitoolbox.utils.ads.InterstitialManager
import com.joasasso.minitoolbox.utils.ads.RewardedManager
import com.joasasso.minitoolbox.utils.ads.ToolUsageTracker

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun MiniToolboxNavGraph(
    navController: NavHostController,
    shouldShowAds: Boolean,
    interstitialAdUnitId: String,
    rewardedAdUnitId: String
) {
    val animationDuration = 150
    val context = LocalContext.current
    val activity = (context as? Activity)

    LaunchedEffect(Unit) {
        // Inicializar managers una sola vez
        InterstitialManager.init(context.applicationContext, interstitialAdUnitId)
        RewardedManager.init(context.applicationContext, rewardedAdUnitId, false)
    }

    // Mapeo route -> toolId (ajustalo si tenés otro id)
    val routeToToolId: Map<String, String> = remember {
        ToolRegistry.tools.associate { it.screen.route to (it.screen.route) }
    }
    val toolRoutes: Set<String> = remember { routeToToolId.keys }

    var lastRoute by remember { mutableStateOf<String?>(null) }
    val backStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(backStackEntry?.destination?.route) {
        val route = backStackEntry?.destination?.route ?: return@LaunchedEffect
        if (route != lastRoute && toolRoutes.contains(route)) {
            val toolId = routeToToolId[route] ?: route

            // contar solo si pasó el cooldown por herramienta
            val isNewAccess = ToolUsageTracker.onToolOpened(context, toolId)

            if (isNewAccess && activity != null) {
                // Realiza conteo global y lleva cooldown de tiempo
                InterstitialManager.onToolOpened(
                    activity = activity,
                    shouldShowAds = shouldShowAds
                )
            }

            lastRoute = route
        }
    }

    val onBackSmart: () -> Unit = {
        val popped = navController.popBackStack()
        if (!popped) (context as? Activity)?.finish()
    }

    NavHost(
        navController    = navController,
        startDestination = Screen.Categories.route,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(animationDuration)
            )
        },
        exitTransition = { fadeOut(animationSpec = tween(animationDuration)) },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(animationDuration)
            ) + fadeIn(animationSpec = tween(animationDuration))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(animationDuration)
            )
        }
    ) {
        composable(Screen.Categories.route) {
            CategoriesScreen(
                tools = ToolRegistry.tools,
                onToolClick = { tool -> navController.navigate(tool.screen.route) },
                onNavigateToPro = { navController.navigate(Screen.Pro.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) }
            )
        }

        composable(Screen.RandomColor.route) {
            RandomColorGeneratorScreen(onBack = onBackSmart)
        }
        composable(Screen.GroupSelector.route) {
            GroupSelectorScreen(onBack = onBackSmart)
        }
        composable(Screen.CoinFlip.route) {
            CoinFlipScreen(onBack = onBackSmart)
        }
        composable(Screen.DecimalBinary.route) {
            DecimalBinaryConverterScreen(onBack = onBackSmart)
        }
        composable(Screen.TextBinary.route) {
            TextBinaryConverterScreen(onBack = onBackSmart)
        }
        composable(Screen.TrucoScoreboard.route) {
            TrucoScoreBoardScreen(onBack = onBackSmart)
        }
        composable(Screen.AgeCalculator.route) {
            AgeCalculatorScreen(onBack = onBackSmart)
        }
        composable(Screen.ZodiacSign.route) {
            ZodiacSignScreen(onBack = onBackSmart)
        }
        composable(Screen.Pomodoro.route) {
            PomodoroScreen(onBack = onBackSmart)
        }
        composable(Screen.BubbleLevel.route) {
            BubbleLevelScreen(onBack = onBackSmart)
        }
        composable(Screen.Percentage.route) {
            PorcentajeScreen(onBack = onBackSmart)
        }
        composable(Screen.TimeConverter.route) {
            ConversorHorasScreen(onBack = onBackSmart)
        }
        composable(Screen.BmiCalculator.route) {
            IMCScreen(onBack = onBackSmart)
        }
        composable(Screen.RomanNumerals.route) {
            ConversorRomanosScreen(onBack = onBackSmart)
        }
        composable(Screen.UnitConverter.route) {
            ConversorUnidadesScreen(onBack = onBackSmart)
        }
        composable(Screen.PasswordGenerator.route) {
            GeneradorContrasenaScreen(onBack = onBackSmart)
        }
        composable(Screen.ActivitySuggester.route) {
            SugeridorActividadScreen(onBack = onBackSmart)
        }
        composable(Screen.NameGenerator.route) {
            GeneradorNombresScreen(onBack = onBackSmart)
        }
        composable(Screen.QrGenerator.route) {
            GeneradorQrScreen(onBack = onBackSmart)
        }
        composable(Screen.VcardGenerator.route) {
            GeneradorQrContactoScreen(onBack = onBackSmart)
        }
        composable(Screen.LoremIpsum.route) {
            GeneradorLoremIpsumScreen(onBack = onBackSmart)
        }
        composable(Screen.Ruler.route) {
            ReglaScreen(onBack = onBackSmart)
        }
        composable(Screen.LightMeter.route) {
            LightSensorScreen(onBack = onBackSmart)
        }
        composable(Screen.Flashlight.route) {
            FlashScreen(onBack = onBackSmart)
        }
        composable(Screen.Streaks.route) {
            HabitTrackerScreen(onBack = onBackSmart)
        }
        composable(Screen.Water.route) {
            AguaReminderScreen(
                onBack = onBackSmart,
                onShowEstadisticas = {
                    navController.navigate(Screen.WaterStats.route)
                }
            )
        }
        composable(Screen.Countdown.route) {
            RemainingTimeScreen(onBack = onBackSmart)
        }
        composable(Screen.WaterStats.route) {
            AguaStatisticsScreen(onBack = onBackSmart)
        }
        composable(Screen.CountriesInfo.route) {
            CountriesInfoScreen(onBack = onBackSmart)
        }
        composable(Screen.SelectorWheel.route) {
            OptionSelectorScreen(onBack = onBackSmart)
        }
        composable(Screen.GuessFlag.route) {
            AdivinaBanderaScreen(onBack = onBackSmart)
        }
        composable(Screen.Meetings.route) {
            ReunionesScreen(
                onBack = onBackSmart,
                onCrearReunion = { navController.navigate(Screen.MeetingCreate.route) },
                onReunionClick = { reunion ->
                    navController.navigate(Screen.MeetingDetail.route + "/${reunion.id}")
                }
            )
        }
        composable(Screen.MeetingCreate.route) {
            CrearReunionScreen(
                onBack = onBackSmart,
                onReunionCreada = { navController.popBackStack() }
            )
        }
        composable(Screen.MeetingDetail.route + "/{reunionId}") {
            val reunionId = it.arguments?.getString("reunionId") ?: ""
            DetallesReunionScreen(
                onBack = onBackSmart,
                reunionId = reunionId,
                onEditarGasto = { idReunion, idGasto ->
                    navController.navigate(Screen.ExpenseEdit.route + "/$idReunion/$idGasto")
                },
                onAgregarGasto = { idReunion ->
                    navController.navigate(Screen.ExpenseAdd.route + "/$idReunion")
                }
            )
        }
        composable(Screen.ExpenseEdit.route + "/{reunionId}/{gastoId}") {
            EditarGastoScreen(
                reunionId = remember {
                    it.arguments?.getString("reunionId") ?: ""
                },
                gastoId = remember {
                    it.arguments?.getString("gastoId") ?: ""
                },
                onBack = onBackSmart
            )
        }
        composable(Screen.ExpenseAdd.route + "/{reunionId}") {
            val reunionId = it.arguments?.getString("reunionId") ?: ""
            AgregarGastoScreen(
                reunionId = reunionId,
                onBack = onBackSmart
            )
        }
        composable(Screen.Dice.route) {
            LanzadorDadosScreen(onBack = onBackSmart)
        }
        composable(Screen.QuickCalcs.route) {
            CalculosRapidosScreen(onBack = onBackSmart)
        }
        composable(Screen.Quotes.route) {
            BasicPhrasesScreen(onBack = onBackSmart)
        }
        composable(Screen.MultiverseMe.route) {
            InOtherWoldScreen(onBack = onBackSmart)
        }
        composable(Screen.GuessCapital.route) {
            AdivinaCapitalScreen(onBack = onBackSmart)
        }
        composable(Screen.Compass.route) {
            BrujulaScreen(onBack = onBackSmart)
        }
        composable(Screen.Todo.route) {
            ToDoListScreen(onBack = onBackSmart)
        }
        composable(Screen.Events.route) {
            EventosImportantesScreen(onBack = onBackSmart)
        }
        composable(Screen.CompoundInterest.route) {
            InteresCompuestoScreen(onBack = onBackSmart)
        }
        composable(Screen.Scoreboard.route) {
            MarcadorEquiposScreen(onBack = onBackSmart)
        }
        composable(Screen.Magnifier.route) {
            MagnifierScreen(onBack = onBackSmart)
        }
        composable(Screen.ArRuler.route) {
            ArRulerSceneViewScreen(onBack = onBackSmart)
        }
        composable(Screen.Noise.route) {
            WhiteNoiseScreen(onBack = onBackSmart)
        }
        composable(Screen.About.route) {
            AboutScreen(
                onBack = onBackSmart,
                onOpenLicenses = {
                    com.google.android.gms.oss.licenses.OssLicensesMenuActivity.setActivityTitle(
                        context.getString(R.string.about_licenses_button)
                    )
                    context.startActivity(
                        android.content.Intent(context, com.google.android.gms.oss.licenses.OssLicensesMenuActivity::class.java)
                    )
                },
                onOpenDevTools = { navController.navigate("dev_metrics") },
                onNavigateToPro = { navController.navigate(Screen.Pro.route) }
            )
        }
        composable(Screen.DevMetrics.route) {
            MetricsDevScreen()
        }
        composable(Screen.Pro.route) {
            ProScreen(onBack = onBackSmart)
        }
    }
}

