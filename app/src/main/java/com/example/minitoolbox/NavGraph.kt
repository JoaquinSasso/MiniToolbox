// app/src/main/java/com/example/minitoolbox/NavGraph.kt
package com.example.minitoolbox

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.minitoolbox.nav.Screen
import com.example.minitoolbox.tools.ToolRegistry
import com.example.minitoolbox.tools.calculadoras.AgeCalculatorScreen
import com.example.minitoolbox.tools.calculadoras.ConversorHorasScreen
import com.example.minitoolbox.tools.calculadoras.ConversorRomanosScreen
import com.example.minitoolbox.tools.calculadoras.ConversorUnidadesScreen
import com.example.minitoolbox.tools.calculadoras.DecimalBinaryConverterScreen
import com.example.minitoolbox.tools.calculadoras.IMCScreen
import com.example.minitoolbox.tools.calculadoras.PorcentajeScreen
import com.example.minitoolbox.tools.calculadoras.TextBinaryConverterScreen
import com.example.minitoolbox.tools.calculadoras.ZodiacSignScreen
import com.example.minitoolbox.tools.generadores.GeneradorContrasenaScreen
import com.example.minitoolbox.tools.generadores.GeneradorLoremIpsumScreen
import com.example.minitoolbox.tools.generadores.GeneradorNombresScreen
import com.example.minitoolbox.tools.generadores.GeneradorQrContactoScreen
import com.example.minitoolbox.tools.generadores.GeneradorQrScreen
import com.example.minitoolbox.tools.generadores.GroupSelectorScreen
import com.example.minitoolbox.tools.generadores.RandomColorGeneratorScreen
import com.example.minitoolbox.tools.generadores.SugeridorActividadScreen
import com.example.minitoolbox.tools.juegos.CoinFlipScreen
import com.example.minitoolbox.tools.juegos.TrucoScoreBoardScreen
import com.example.minitoolbox.tools.medicion.BubbleLevelScreen
import com.example.minitoolbox.tools.medicion.LinternaScreen
import com.example.minitoolbox.tools.medicion.MedidorLuzScreen
import com.example.minitoolbox.tools.medicion.ReglaScreen
import com.example.minitoolbox.tools.recordatorios.ContadorRachaScreen
import com.example.minitoolbox.tools.recordatorios.agua.AguaEstadisticasScreen
import com.example.minitoolbox.tools.recordatorios.agua.RecordatorioAguaScreen
import com.example.minitoolbox.tools.recordatorios.pomodoro.PomodoroScreen

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
        composable(Screen.Regla.route){
            ReglaScreen(onBack = {navController.popBackStack() })
        }
        composable(Screen.MedidorLuz.route){
            MedidorLuzScreen(onBack = {navController.popBackStack() })
        }
        composable(Screen.Linterna.route){
            LinternaScreen(onBack = {navController.popBackStack() })
        }
        composable(Screen.Rachas.route){
            ContadorRachaScreen(onBack = {navController.popBackStack() })
        }
        composable(Screen.Agua.route){
            RecordatorioAguaScreen(
                onBack = { navController.popBackStack() },
                onShowEstadisticas = {
                    navController.navigate(Screen.EstadisticasAgua.route)
                }
            )
        }

        composable(Screen.EstadisticasAgua.route){
            AguaEstadisticasScreen(onBack = {navController.popBackStack() })
        }

    }
}
