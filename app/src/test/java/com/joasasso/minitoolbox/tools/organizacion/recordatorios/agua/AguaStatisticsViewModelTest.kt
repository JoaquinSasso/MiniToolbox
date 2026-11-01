package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.joasasso.minitoolbox.TestApplication
import com.joasasso.minitoolbox.data.DefaultAguaRepository
import com.joasasso.minitoolbox.data.aguaDataStore
import com.joasasso.minitoolbox.data.guardarAguaFecha
import com.joasasso.minitoolbox.data.guardarObjetivo
import com.joasasso.minitoolbox.data.guardarPorVaso
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class AguaStatisticsViewModelTest {

    private lateinit var app: Application
    private val testDispatcher = UnconfinedTestDispatcher()
    private var vm: AguaStatisticsViewModel? = null

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        app.aguaDataStore.edit { it.clear() }
    }

    @After
    fun tearDown() {
        vm?.viewModelScope?.cancel()
        Dispatchers.resetMain()
    }

    /**
     * Mantiene un colector de `uiState` durante todo el test. Sin él, `WhileSubscribed(5000)`
     * deja de escuchar DataStore cada vez que el reloj virtual avanza 5 s entre dos `first {}`,
     * y el estado puede quedar congelado en un valor viejo: el test espera hasta el timeout
     * de `runTest` (falla intermitente en CI con `UncompletedCoroutinesError`).
     */
    private fun TestScope.mantenerSuscripto(state: StateFlow<*>) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
    }

    @Test
    fun uiState_inicialmenteVacio_exponeValoresPorDefecto() = runTest {
        val model = AguaStatisticsViewModel(app, DefaultAguaRepository)
        vm = model
        mantenerSuscripto(model.uiState)

        val state = model.uiState.first { !it.isLoading }
        assertTrue(state.historico.isEmpty())
        assertEquals(0, state.promedioSemana)
        assertNull(state.mejorDia)
        assertEquals(2000, state.objetivoML)
        assertEquals(250, state.mlPorVaso)

        model.viewModelScope.cancel()
    }

    @Test
    fun uiState_cargaHistoricoPromedioMejorDiaYConfiguracion() = runTest {
        val dia1 = LocalDate.of(2026, 10, 1)
        val dia2 = LocalDate.of(2026, 10, 2)
        app.guardarAguaFecha(dia1, 1500)
        app.guardarAguaFecha(dia2, 2500)
        app.guardarObjetivo(2250)
        app.guardarPorVaso(285)

        val model = AguaStatisticsViewModel(app, DefaultAguaRepository)
        vm = model
        mantenerSuscripto(model.uiState)

        val state = model.uiState.first { !it.isLoading && it.historico.size == 2 }
        assertEquals(listOf(dia1 to 1500, dia2 to 2500), state.historico)
        assertEquals(2000, state.promedioSemana)
        assertEquals(dia2 to 2500, state.mejorDia)
        assertEquals(2250, state.objetivoML)
        assertEquals(285, state.mlPorVaso)

        model.viewModelScope.cancel()
    }

    @Test
    fun editarConsumoFecha_persisteEnRepositorioYActualizaUiState() = runTest {
        val fecha = LocalDate.of(2026, 10, 3)
        app.guardarAguaFecha(fecha, 1000)

        val model = AguaStatisticsViewModel(
            application = app,
            repository = DefaultAguaRepository,
            todayProvider = { LocalDate.of(2026, 10, 10) }
        )
        vm = model
        mantenerSuscripto(model.uiState)

        model.uiState.first { !it.isLoading && it.historico.size == 1 }

        model.editarConsumoFecha(fecha, 1995)
        advanceUntilIdle()

        val updated = model.uiState.first { it.historico.firstOrNull()?.second == 1995 }
        assertEquals(1995, updated.promedioSemana)
        assertEquals(fecha to 1995, updated.mejorDia)
        assertEquals(1995, DefaultAguaRepository.flujoAguaFecha(app, fecha).first())

        model.viewModelScope.cancel()
    }
}
