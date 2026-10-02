package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.joasasso.minitoolbox.TestApplication
import com.joasasso.minitoolbox.data.DefaultAguaRepository
import com.joasasso.minitoolbox.data.aguaDataStore
import com.joasasso.minitoolbox.data.guardarAguaFecha
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class AguaViewModelTest {

    private lateinit var application: Application

    @Before
    fun setUp() = runTest {
        application = ApplicationProvider.getApplicationContext()
        application.aguaDataStore.edit { it.clear() }
    }

    @Test
    fun agregarAgua_incrementaConsumoYEmiteEvento() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val viewModel = AguaViewModel(application, DefaultAguaRepository)

        val collectJob = launch(testDispatcher) { viewModel.uiState.collect {} }

        // Agregar 250 ml
        viewModel.agregarAgua(250)

        val state = viewModel.uiState.first { !it.isLoading && it.totalAgua == 250 }
        assertEquals(250, state.totalAgua)

        collectJob.cancel()
    }

    @Test
    fun agregarAguaNegativa_cuandoEsCero_emiteAvisoCeroYSigueEnCero() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val viewModel = AguaViewModel(application, DefaultAguaRepository)

        var eventReceived: AguaEvent? = null
        val eventJob = launch(testDispatcher) {
            viewModel.events.collect { eventReceived = it }
        }
        val collectJob = launch(testDispatcher) { viewModel.uiState.collect {} }

        // Intentar restar cuando el total es 0
        viewModel.agregarAgua(-250)

        assertEquals(AguaEvent.ShowZeroWarning, eventReceived)
        assertEquals(0, viewModel.uiState.value.totalAgua)

        eventJob.cancel()
        collectJob.cancel()
    }

    @Test
    fun resetear_poneContadorEnCeroYEmiteEvento() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val viewModel = AguaViewModel(application, DefaultAguaRepository)

        var eventReceived: AguaEvent? = null
        val eventJob = launch(testDispatcher) {
            viewModel.events.collect { eventReceived = it }
        }
        val collectJob = launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.agregarAgua(500)
        viewModel.resetear()

        val state = viewModel.uiState.first { !it.isLoading && it.totalAgua == 0 }
        assertEquals(0, state.totalAgua)
        assertEquals(AguaEvent.WaterReset, eventReceived)

        eventJob.cancel()
        collectJob.cancel()
    }

    @Test
    fun rolloverMedianoche_noArrastraConsumoDelDiaAnterior() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val dia1 = LocalDate.of(2026, 10, 1)
        val dia2 = LocalDate.of(2026, 10, 2)
        val dateFlow = MutableStateFlow(dia1)

        // Día 1: El usuario acumuló 1750 ml
        application.guardarAguaFecha(dia1, 1750)

        val viewModel = AguaViewModel(application, DefaultAguaRepository, dateFlowOverride = dateFlow)
        val collectJob = launch(testDispatcher) { viewModel.uiState.collect {} }

        // Verificar que en día 1 se leen los 1750 ml
        val stateDia1 = viewModel.uiState.first { !it.isLoading && it.totalAgua == 1750 }
        assertEquals(1750, stateDia1.totalAgua)

        // Medianoche: dateFlow conmuta a día 2
        dateFlow.value = dia2

        // Verificar que pasa reactivamente a 0 ml para el día 2
        val stateDia2Inicial = viewModel.uiState.first { !it.isLoading && it.totalAgua == 0 }
        assertEquals(0, stateDia2Inicial.totalAgua)
        assertEquals(dia2, stateDia2Inicial.fecha)

        // El usuario bebe un vaso (250 ml) en día 2
        viewModel.agregarAgua(250)

        val stateDia2Final = viewModel.uiState.first { !it.isLoading && it.totalAgua == 250 }
        // Se valida que tiene 250 ml (NO 1750 + 250 = 2000)
        assertEquals(250, stateDia2Final.totalAgua)

        collectJob.cancel()
    }

    @Test
    fun guardarObjetivoYPorVaso_actualizaValoresEnUiState() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val viewModel = AguaViewModel(application, DefaultAguaRepository)

        val collectJob = launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.guardarObjetivo(3000)
        viewModel.guardarPorVaso(350)

        val state = viewModel.uiState.first { !it.isLoading && it.objetivoML == 3000 && it.mlPorVaso == 350 }
        assertEquals(3000, state.objetivoML)
        assertEquals(350, state.mlPorVaso)

        collectJob.cancel()
    }

    @Test
    fun toggleNotificaciones_actualizaEstadoYEmiteEvento() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val viewModel = AguaViewModel(application, DefaultAguaRepository)

        var lastEvent: AguaEvent? = null
        val eventJob = launch(testDispatcher) {
            viewModel.events.collect { lastEvent = it }
        }
        val collectJob = launch(testDispatcher) { viewModel.uiState.collect {} }

        viewModel.toggleNotificaciones(true)
        val stateActivo = viewModel.uiState.first { !it.isLoading && it.notificacionesActivas }
        assertTrue(stateActivo.notificacionesActivas)
        assertEquals(AguaEvent.NotifEnabled, lastEvent)

        viewModel.toggleNotificaciones(false)
        val stateInactivo = viewModel.uiState.first { !it.isLoading && !it.notificacionesActivas }
        assertTrue(!stateInactivo.notificacionesActivas)
        assertEquals(AguaEvent.NotifDisabled, lastEvent)

        eventJob.cancel()
        collectJob.cancel()
    }
}
