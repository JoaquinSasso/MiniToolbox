// app/src/main/java/com/example/minitoolbox/NavGraph.kt
package com.joasasso.minitoolbox

import ZodiacSignScreen
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.joasasso.minitoolbox.tools.info.BasicPhrasesScreen
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
            LightSensorScreen(onBack = {navController.popBackStack() })
        }
        composable(Screen.Linterna.route){
            FlashScreen(onBack = {navController.popBackStack() })
        }
        composable(Screen.Rachas.route){
            HabitTrackerScreen(onBack = {navController.popBackStack() })
        }
        composable(Screen.Agua.route){
            AguaReminderScreen(
                onBack = { navController.popBackStack() },
                onShowEstadisticas = {
                    navController.navigate(Screen.EstadisticasAgua.route)
                }
            )
        }
        composable(Screen.TiempoHasta.route){
            RemainingTimeScreen(onBack = { navController.popBackStack() }) }

        composable(Screen.EstadisticasAgua.route){
            AguaStatisticsScreen(onBack = {navController.popBackStack() })
        }

        composable(Screen.PaisesInfo.route){
            CountriesInfoScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.RuletaSelectora.route)
        {
            OptionSelectorScreen(onBack = {navController.popBackStack()})
        }
        composable(Screen.AdivinaBandera.route)
        {
            AdivinaBanderaScreen(onBack = {navController.popBackStack()})
        }
        composable(Screen.Reuniones.route) {
            ReunionesScreen(
                onBack = { navController.popBackStack() },
                onCrearReunion = { navController.navigate(Screen.CrearReunion.route) },
                onReunionClick = { reunion ->
                    navController.navigate(Screen.DetallesReunion.route + "/${reunion.id}")
                }
            )
        }
        composable(Screen.CrearReunion.route) {
            CrearReunionScreen(
                onBack = { navController.popBackStack() },
                onReunionCreada = { navController.popBackStack() }
            )
        }
        composable(Screen.DetallesReunion.route + "/{reunionId}") {
            val reunionId = it.arguments?.getString("reunionId") ?: ""
            DetallesReunionScreen(
                onBack = { navController.popBackStack() },
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
                onBack = { navController.popBackStack() })
        }
        composable(Screen.AgregarGasto.route + "/{reunionId}") {
            val reunionId = it.arguments?.getString("reunionId") ?: ""
            AgregarGastoScreen(
                reunionId = reunionId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Dados.route){
            LanzadorDadosScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.CalculosRapidos.route){
            CalculosRapidosScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Frases.route){
            BasicPhrasesScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.MiYoDelMultiverso.route){
            InOtherWoldScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.AdivinaCapital.route){
            AdivinaCapitalScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Brujula.route){
            BrujulaScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.ToDo.route){
            ToDoListScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Eventos.route){
            EventosImportantesScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.InteresCompuesto.route){
            InteresCompuestoScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Scoreboard.route){
            MarcadorEquiposScreen(onBack = { navController.popBackStack() })
        }
    }
}
