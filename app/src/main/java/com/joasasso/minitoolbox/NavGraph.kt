// app/src/main/java/com/example/minitoolbox/NavGraph.kt
package com.joasasso.minitoolbox

import ZodiacSignScreen
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.joasasso.minitoolbox.nav.Screen
import com.joasasso.minitoolbox.tools.ToolRegistry
import com.joasasso.minitoolbox.tools.calculadoras.ConversorHorasScreen
import com.joasasso.minitoolbox.tools.calculadoras.ConversorRomanosScreen
import com.joasasso.minitoolbox.tools.calculadoras.ConversorUnidadesScreen
import com.joasasso.minitoolbox.tools.calculadoras.DecimalBinaryConverterScreen
import com.joasasso.minitoolbox.tools.calculadoras.IMCScreen
import com.joasasso.minitoolbox.tools.calculadoras.PorcentajeScreen
import com.joasasso.minitoolbox.tools.generadores.GeneradorContrasenaScreen
import com.joasasso.minitoolbox.tools.generadores.GeneradorLoremIpsumScreen
import com.joasasso.minitoolbox.tools.generadores.GeneradorNombresScreen
import com.joasasso.minitoolbox.tools.generadores.GeneradorQrContactoScreen
import com.joasasso.minitoolbox.tools.generadores.GeneradorQrScreen
import com.joasasso.minitoolbox.tools.generadores.GroupSelectorScreen
import com.joasasso.minitoolbox.tools.generadores.RandomColorGeneratorScreen
import com.joasasso.minitoolbox.tools.generadores.SugeridorActividadScreen
import com.joasasso.minitoolbox.tools.info.AgeCalculatorScreen
import com.joasasso.minitoolbox.tools.info.PaisesInfoScreen
import com.joasasso.minitoolbox.tools.info.TextBinaryConverterScreen
import com.joasasso.minitoolbox.tools.info.TiempoHasta
import com.joasasso.minitoolbox.tools.juegos.CoinFlipScreen
import com.joasasso.minitoolbox.tools.juegos.SelectorOpcionesScreen
import com.joasasso.minitoolbox.tools.juegos.TrucoScoreBoardScreen
import com.joasasso.minitoolbox.tools.medicion.BubbleLevelScreen
import com.joasasso.minitoolbox.tools.medicion.LinternaScreen
import com.joasasso.minitoolbox.tools.medicion.MedidorLuzScreen
import com.joasasso.minitoolbox.tools.medicion.ReglaScreen
import com.joasasso.minitoolbox.tools.recordatorios.ContadorRachaScreen
import com.joasasso.minitoolbox.tools.recordatorios.agua.AguaEstadisticasScreen
import com.joasasso.minitoolbox.tools.recordatorios.agua.RecordatorioAguaScreen
import com.joasasso.minitoolbox.tools.recordatorios.pomodoro.PomodoroScreen

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
        composable(Screen.TiempoHasta.route){
            TiempoHasta(onBack = { navController.popBackStack() }) }

        composable(Screen.EstadisticasAgua.route){
            AguaEstadisticasScreen(onBack = {navController.popBackStack() })
        }

        composable(Screen.PaisesInfo.route){
            PaisesInfoScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.RuletaSelectora.route)
        {
            SelectorOpcionesScreen(onBack = {navController.popBackStack()})
        }

    }
}
