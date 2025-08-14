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
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
import com.joasasso.minitoolbox.tools.herramientas.instrumentos.BrujulaScreen
import com.joasasso.minitoolbox.tools.herramientas.instrumentos.BubbleLevelScreen
import com.joasasso.minitoolbox.tools.herramientas.instrumentos.FlashScreen
import com.joasasso.minitoolbox.tools.herramientas.instrumentos.LightSensorScreen
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

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun MiniToolboxNavGraph(navController: NavHostController) {
    val animationDuration = 150
    val context = LocalContext.current

    val onBackSmart: () -> Unit = {
        val popped = navController.popBackStack()
        if (!popped) {
            (context as? Activity)?.finish()
        }
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
        exitTransition = {
            fadeOut(animationSpec = tween(animationDuration))
        },
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
                tools       = ToolRegistry.tools,
                onToolClick = { tool -> navController.navigate(tool.screen.route) }
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
        composable(Screen.DecimalBinaryConverter.route) {
            DecimalBinaryConverterScreen(onBack = onBackSmart)
        }
        composable(Screen.TextBinaryConverter.route) {
            TextBinaryConverterScreen(onBack = onBackSmart)
        }
        composable(Screen.TrucoScoreBoard.route) {
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
        composable(Screen.Porcentaje.route) {
            PorcentajeScreen(onBack = onBackSmart)
        }
        composable(Screen.ConversorHoras.route) {
            ConversorHorasScreen(onBack = onBackSmart)
        }
        composable(Screen.CalculadoraDeIMC.route) {
            IMCScreen(onBack = onBackSmart)
        }
        composable(Screen.ConversorRomanos.route) {
            ConversorRomanosScreen(onBack = onBackSmart)
        }
        composable(Screen.ConversorUnidades.route) {
            ConversorUnidadesScreen(onBack = onBackSmart)
        }
        composable(Screen.GeneradorContrasena.route) {
            GeneradorContrasenaScreen(onBack = onBackSmart)
        }
        composable(Screen.SugeridorActividades.route) {
            SugeridorActividadScreen(onBack = onBackSmart)
        }
        composable(Screen.GeneradorNombres.route) {
            GeneradorNombresScreen(onBack = onBackSmart)
        }
        composable(Screen.GeneradorQR.route) {
            GeneradorQrScreen(onBack = onBackSmart)
        }
        composable(Screen.GeneradorVCard.route) {
            GeneradorQrContactoScreen(onBack = onBackSmart)
        }
        composable(Screen.LoremIpsum.route) {
            GeneradorLoremIpsumScreen(onBack = onBackSmart)
        }
        composable(Screen.Regla.route) {
            ReglaScreen(onBack = onBackSmart)
        }
        composable(Screen.MedidorLuz.route) {
            LightSensorScreen(onBack = onBackSmart)
        }
        composable(Screen.Linterna.route) {
            FlashScreen(onBack = onBackSmart)
        }
        composable(Screen.Rachas.route) {
            HabitTrackerScreen(onBack = onBackSmart)
        }
        composable(Screen.Agua.route) {
            AguaReminderScreen(
                onBack = onBackSmart,
                onShowEstadisticas = {
                    navController.navigate(Screen.EstadisticasAgua.route)
                }
            )
        }
        composable(Screen.TiempoHasta.route) {
            RemainingTimeScreen(onBack = onBackSmart)
        }
        composable(Screen.EstadisticasAgua.route) {
            AguaStatisticsScreen(onBack = onBackSmart)
        }
        composable(Screen.PaisesInfo.route) {
            CountriesInfoScreen(onBack = onBackSmart)
        }
        composable(Screen.RuletaSelectora.route) {
            OptionSelectorScreen(onBack = onBackSmart)
        }
        composable(Screen.AdivinaBandera.route) {
            AdivinaBanderaScreen(onBack = onBackSmart)
        }
        composable(Screen.Reuniones.route) {
            ReunionesScreen(
                onBack = onBackSmart,
                onCrearReunion = { navController.navigate(Screen.CrearReunion.route) },
                onReunionClick = { reunion ->
                    navController.navigate(Screen.DetallesReunion.route + "/${reunion.id}")
                }
            )
        }
        composable(Screen.CrearReunion.route) {
            CrearReunionScreen(
                onBack = onBackSmart,
                onReunionCreada = { navController.popBackStack() }
            )
        }
        composable(Screen.DetallesReunion.route + "/{reunionId}") {
            val reunionId = it.arguments?.getString("reunionId") ?: ""
            DetallesReunionScreen(
                onBack = onBackSmart,
                reunionId = reunionId,
                onEditarGasto = { idReunion, idGasto ->
                    navController.navigate(Screen.EditarGasto.route + "/$idReunion/$idGasto")
                },
                onAgregarGasto = { idReunion ->
                    navController.navigate(Screen.AgregarGasto.route + "/$idReunion")
                }
            )
        }
        composable(Screen.EditarGasto.route + "/{reunionId}/{gastoId}") {
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
        composable(Screen.AgregarGasto.route + "/{reunionId}") {
            val reunionId = it.arguments?.getString("reunionId") ?: ""
            AgregarGastoScreen(
                reunionId = reunionId,
                onBack = onBackSmart
            )
        }
        composable(Screen.Dados.route) {
            LanzadorDadosScreen(onBack = onBackSmart)
        }
        composable(Screen.CalculosRapidos.route) {
            CalculosRapidosScreen(onBack = onBackSmart)
        }
        composable(Screen.Frases.route) {
            BasicPhrasesScreen(onBack = onBackSmart)
        }
        composable(Screen.MiYoDelMultiverso.route) {
            InOtherWoldScreen(onBack = onBackSmart)
        }
        composable(Screen.AdivinaCapital.route) {
            AdivinaCapitalScreen(onBack = onBackSmart)
        }
        composable(Screen.Brujula.route) {
            BrujulaScreen(onBack = onBackSmart)
        }
        composable(Screen.ToDo.route) {
            ToDoListScreen(onBack = onBackSmart)
        }
        composable(Screen.Eventos.route) {
            EventosImportantesScreen(onBack = onBackSmart)
        }
        composable(Screen.InteresCompuesto.route) {
            InteresCompuestoScreen(onBack = onBackSmart)
        }
        composable(Screen.Scoreboard.route) {
            MarcadorEquiposScreen(onBack = onBackSmart)
        }
    }
}

