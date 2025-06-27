// app/src/main/java/com/example/minitoolbox/NavGraph.kt
package com.example.minitoolbox

import GeneradorQrContactoScreen
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.minitoolbox.tools.ToolRegistry
import com.example.minitoolbox.tools.calculadoras.*
import com.example.minitoolbox.tools.generadores.*
import com.example.minitoolbox.tools.juegos.*
import com.example.minitoolbox.nav.Screen
import com.example.minitoolbox.tools.medicion.BubbleLevelScreen
import com.example.minitoolbox.tools.pomodoro.PomodoroScreen
import com.example.minitoolbox.tools.truco.TrucoScoreBoardScreen

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun MiniToolboxNavGraph(navController: NavHostController) {
    NavHost(
        navController    = navController,
        startDestination = Screen.Categories.route
    ) {
        composable(Screen.Categories.route) {
            CategoriesScreen(
                tools       = ToolRegistry.tools,
                onToolClick = { tool ->
                    // Navegamos usando la ruta definida en Screen, no el objeto Tool
                    navController.navigate(tool.screen.route)
                }
            )
        }
        composable(Screen.RandomColor.route) {
            RandomColorGeneratorScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.GroupSelector.route) {
            GroupSelectorScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.CoinFlip.route) {
            CoinFlipScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.DecimalBinaryConverter.route) {
            DecimalBinaryConverterScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.TextBinaryConverter.route) {
            TextBinaryConverterScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.TrucoScoreBoard.route) {
            TrucoScoreBoardScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.AgeCalculator.route) {
            AgeCalculatorScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.ZodiacSign.route) {
            ZodiacSignScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Pomodoro.route) {
            PomodoroScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.BubbleLevel.route) {
            BubbleLevelScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Porcentaje.route) {
            PorcentajeScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.ConversorHoras.route) {
            ConversorHorasScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.CalculadoraDeIMC.route) {
            IMCScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.ConversorRomanos.route) {
            ConversorRomanosScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.ConversorUnidades.route) {
            ConversorUnidadesScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.GeneradorContrasena.route) {
            GeneradorContrasenaScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.SugeridorActividades.route) {
            SugeridorActividadScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.GeneradorNombres.route) {
            GeneradorNombresScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.GeneradorQR.route){
            GeneradorQrScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.GeneradorVCard.route){
            GeneradorQrContactoScreen (onBack = {navController.popBackStack() })
        }
        composable(Screen.LoremIpsum.route){
            GeneradorLoremIpsumScreen(onBack = {navController.popBackStack() })
        }
    }
}
