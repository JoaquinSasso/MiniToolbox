package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.TestApplication
import com.joasasso.minitoolbox.data.DefaultAguaRepository
import com.joasasso.minitoolbox.data.aguaDataStore
import com.joasasso.minitoolbox.data.guardarAguaFecha
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class AguaViewModelTest {

    private lateinit var app: Application
    private val testDispatcher = UnconfinedTestDispatcher()
    private var vm: AguaViewModel? = null

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        app.aguaDataStore.edit { it.clear() }
        val notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
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
    fun agregarAgua_incrementaConsumoYEmiteEvento() = runTest {
        val dateFlow = MutableStateFlow(LocalDate.of(2026, 10, 2))
        val model = AguaViewModel(app, DefaultAguaRepository, dateFlowOverride = dateFlow)
        vm = model
        mantenerSuscripto(model.uiState)

        model.uiState.first { !it.isLoading }

        val eventDeferred = async(UnconfinedTestDispatcher(testScheduler)) { model.events.first() }

        model.agregarAgua(250)
        advanceUntilIdle()

        assertEquals(AguaEvent.WaterAdded(250), eventDeferred.await())
        val state = model.uiState.first { it.totalAgua == 250 }
        assertEquals(250, state.totalAgua)

        model.viewModelScope.cancel()
    }

    @Test
    fun agregarAgua_toquesRapidosActualizanNotificacionConTotalReal() = runTest {
        val fecha = LocalDate.of(2026, 10, 2)
        val dateFlow = MutableStateFlow(fecha)
        val model = AguaViewModel(app, DefaultAguaRepository, dateFlowOverride = dateFlow)
        vm = model
        mantenerSuscripto(model.uiState)

        try {
            model.uiState.first { !it.isLoading }

            val notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val shadowNotificationManager = shadowOf(notificationManager)
            val receiver = WaterReminderReceiver()

            // Activar notificaciones (objetivo por defecto 2000 ml)
            val notifEnabledDeferred = async(UnconfinedTestDispatcher(testScheduler)) { model.events.first() }
            model.toggleNotificaciones(true)
            assertEquals(AguaEvent.NotifEnabled, notifEnabledDeferred.await())

            // 8:00 AM: El usuario toma 0.25L (250 ml)
            val addOchoAmDeferred = async(UnconfinedTestDispatcher(testScheduler)) { model.events.first() }
            model.agregarAgua(250)
            assertEquals(AguaEvent.WaterAdded(250), addOchoAmDeferred.await())

            // Supongamos que había una notificación previa mostrando 0.25L
            receiver.handleReminder(app, today = fecha)
            val notifOchoAm = shadowNotificationManager.getNotification(WATER_REMINDER_NOTIFICATION_ID)
            assertNotNull(notifOchoAm)
            assertEquals(
                app.getString(R.string.water_reminder_content, 0.25f, 2.0f),
                notifOchoAm.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            )

            // 8:25 AM: El usuario suma 0.50L mediante dos toques rápidos de +250 ml sin esperar recomposición de UI
            model.agregarAgua(250)
            model.agregarAgua(250)

            // Esperar los dos eventos de agregado
            assertEquals(AguaEvent.WaterAdded(250), model.events.first())
            assertEquals(AguaEvent.WaterAdded(250), model.events.first())

            // La notificación obsoleta de 0.25L debe haberse cancelado al registrar nuevo consumo
            assertNull(shadowNotificationManager.getNotification(WATER_REMINDER_NOTIFICATION_ID))

            // El estado y DataStore deben reflejar 750 ml (0.75L), no 500 ml ni 250 ml
            val state = model.uiState.first { it.totalAgua == 750 }
            assertEquals(750, state.totalAgua)

            // 10:00 AM: Se dispara el recordatorio reprogramado y debe mostrar 0.75L de 2.00L
            receiver.handleReminder(app, today = fecha)
            val notifDiezAm = shadowNotificationManager.getNotification(WATER_REMINDER_NOTIFICATION_ID)
            assertNotNull(notifDiezAm)
            assertEquals(
                app.getString(R.string.water_reminder_content, 0.75f, 2.0f),
                notifDiezAm.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            )
        } finally {
            model.viewModelScope.cancel()
        }
    }

    @Test
    fun agregarAguaNegativa_cuandoEsCero_emiteAvisoCeroYSigueEnCero() = runTest {
        val dateFlow = MutableStateFlow(LocalDate.of(2026, 10, 2))
        val model = AguaViewModel(app, DefaultAguaRepository, dateFlowOverride = dateFlow)
        vm = model
        mantenerSuscripto(model.uiState)

        model.uiState.first { !it.isLoading }

        val eventDeferred = async(UnconfinedTestDispatcher(testScheduler)) { model.events.first() }

        model.agregarAgua(-250)
        advanceUntilIdle()

        assertEquals(AguaEvent.ShowZeroWarning, eventDeferred.await())
        assertEquals(0, model.uiState.value.totalAgua)

        model.viewModelScope.cancel()
    }

    @Test
    fun resetear_poneContadorEnCeroYEmiteEvento() = runTest {
        val dateFlow = MutableStateFlow(LocalDate.of(2026, 10, 2))
        val model = AguaViewModel(app, DefaultAguaRepository, dateFlowOverride = dateFlow)
        vm = model
        mantenerSuscripto(model.uiState)

        model.uiState.first { !it.isLoading }

        app.guardarAguaFecha(dateFlow.value, 500)
        advanceUntilIdle()
        assertEquals(500, model.uiState.first { it.totalAgua == 500 }.totalAgua)

        val eventDeferred = async(UnconfinedTestDispatcher(testScheduler)) { model.events.first() }

        model.resetear()
        advanceUntilIdle()

        val state = model.uiState.first { it.totalAgua == 0 }
        assertEquals(0, state.totalAgua)
        assertEquals(AguaEvent.WaterReset, eventDeferred.await())

        model.viewModelScope.cancel()
    }

    @Test
    fun rolloverMedianoche_noArrastraConsumoDelDiaAnterior() = runTest {
        val dia1 = LocalDate.of(2026, 10, 1)
        val dia2 = LocalDate.of(2026, 10, 2)
        val dateFlow = MutableStateFlow(dia1)

        // Día 1: El usuario acumuló 1750 ml
        app.guardarAguaFecha(dia1, 1750)

        val model = AguaViewModel(app, DefaultAguaRepository, dateFlowOverride = dateFlow)
        vm = model
        mantenerSuscripto(model.uiState)

        // Verificar que en día 1 se leen los 1750 ml
        val stateDia1 = model.uiState.first { !it.isLoading && it.totalAgua == 1750 }
        assertEquals(1750, stateDia1.totalAgua)

        // Medianoche: dateFlow conmuta a día 2
        dateFlow.value = dia2
        advanceUntilIdle()

        // Verificar que pasa reactivamente a 0 ml para el día 2
        val stateDia2Inicial = model.uiState.first { it.fecha == dia2 && it.totalAgua == 0 }
        assertEquals(0, stateDia2Inicial.totalAgua)
        assertEquals(dia2, stateDia2Inicial.fecha)

        // El usuario bebe un vaso (250 ml) en día 2
        val eventDeferred = async(UnconfinedTestDispatcher(testScheduler)) { model.events.first() }
        model.agregarAgua(250)
        advanceUntilIdle()

        assertEquals(AguaEvent.WaterAdded(250), eventDeferred.await())
        val stateDia2Final = model.uiState.first { it.fecha == dia2 && it.totalAgua == 250 }
        assertEquals(250, stateDia2Final.totalAgua)

        // Comprobar que en DataStore el día 1 sigue teniendo 1750 y día 2 tiene 250
        assertEquals(1750, DefaultAguaRepository.flujoAguaFecha(app, dia1).first())
        assertEquals(250, DefaultAguaRepository.flujoAguaFecha(app, dia2).first())

        model.viewModelScope.cancel()
    }

    @Test
    fun guardarObjetivoYPorVaso_actualizaValoresEnUiState() = runTest {
        val dateFlow = MutableStateFlow(LocalDate.of(2026, 10, 2))
        val model = AguaViewModel(app, DefaultAguaRepository, dateFlowOverride = dateFlow)
        vm = model
        mantenerSuscripto(model.uiState)

        model.uiState.first { !it.isLoading }

        model.guardarObjetivo(3000)
        model.guardarPorVaso(350)
        advanceUntilIdle()

        val state = model.uiState.first { it.objetivoML == 3000 && it.mlPorVaso == 350 }
        assertEquals(3000, state.objetivoML)
        assertEquals(350, state.mlPorVaso)

        model.viewModelScope.cancel()
    }

    @Test
    fun toggleNotificaciones_actualizaEstadoYEmiteEvento() = runTest {
        val dateFlow = MutableStateFlow(LocalDate.of(2026, 10, 2))
        val model = AguaViewModel(app, DefaultAguaRepository, dateFlowOverride = dateFlow)
        vm = model
        mantenerSuscripto(model.uiState)

        model.uiState.first { !it.isLoading }

        val event1Deferred = async(UnconfinedTestDispatcher(testScheduler)) { model.events.first() }
        model.toggleNotificaciones(true)
        advanceUntilIdle()
        val event1 = event1Deferred.await()
        assertEquals(AguaEvent.NotifEnabled, event1)
        val stateActivo = model.uiState.first { it.notificacionesActivas }
        assertTrue(stateActivo.notificacionesActivas)

        val event2Deferred = async(UnconfinedTestDispatcher(testScheduler)) { model.events.first() }
        model.toggleNotificaciones(false)
        advanceUntilIdle()
        val event2 = event2Deferred.await()
        assertEquals(AguaEvent.NotifDisabled, event2)
        val stateInactivo = model.uiState.first { !it.notificacionesActivas }
        assertTrue(!stateInactivo.notificacionesActivas)

        model.viewModelScope.cancel()
    }
}
