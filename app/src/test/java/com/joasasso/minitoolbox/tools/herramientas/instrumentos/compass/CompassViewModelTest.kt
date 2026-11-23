package com.joasasso.minitoolbox.tools.herramientas.instrumentos.compass

import android.app.Application
import android.hardware.SensorManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.joasasso.minitoolbox.TestApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class CompassViewModelTest {

    private class FakeCompassSensor(override val isAvailable: Boolean = true) : CompassSensor {
        val readings = MutableSharedFlow<CompassReading>(extraBufferCapacity = 16)
        var subscribers = 0
            private set

        override fun readings(): Flow<CompassReading> = kotlinx.coroutines.flow.flow {
            subscribers++
            readings.collect { emit(it) }
        }
    }

    private lateinit var app: Application
    private val testDispatcher = UnconfinedTestDispatcher()
    private var vm: CompassViewModel? = null

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        vm?.viewModelScope?.cancel()
        Dispatchers.resetMain()
    }

    private fun TestScope.mantenerSuscripto(state: StateFlow<*>) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
    }

    private fun flat(azimuth: Float, rotation: Int = 0) =
        CompassReading.Orientation(azimuth, upX = 0f, upY = 0f, upZ = 1f, rotationQuarters = rotation)

    @Test
    fun sinMagnetometro_estadoNoSoportado() = runTest {
        val model = CompassViewModel(app, SavedStateHandle(), FakeCompassSensor(isAvailable = false))
        vm = model
        assertFalse(model.uiState.value.isSupported)
        assertFalse(model.uiState.value.hasReading)
    }

    @Test
    fun lectura_actualizaRumboYDireccion() = runTest {
        val sensor = FakeCompassSensor()
        val model = CompassViewModel(app, SavedStateHandle(), sensor)
        vm = model
        mantenerSuscripto(model.uiState)

        sensor.readings.emit(flat(-45f))

        val state = model.uiState.value
        assertTrue(state.hasReading)
        assertEquals(315f, state.heading, 0.001f)
        assertEquals(7, state.directionIndex) // NO
        assertFalse(state.isTilted)
    }

    @Test
    fun pantallaRotada_corrigeElRumbo() = runTest {
        val sensor = FakeCompassSensor()
        val model = CompassViewModel(app, SavedStateHandle(), sensor)
        vm = model
        mantenerSuscripto(model.uiState)

        sensor.readings.emit(flat(0f, rotation = 1))

        assertEquals(90f, model.uiState.value.heading, 0.001f)
    }

    @Test
    fun cruzarElNorte_rumboContinuoNoSalta() = runTest {
        val sensor = FakeCompassSensor()
        val model = CompassViewModel(app, SavedStateHandle(), sensor)
        vm = model
        mantenerSuscripto(model.uiState)

        sensor.readings.emit(flat(358f))
        sensor.readings.emit(flat(2f))

        assertEquals(2f, model.uiState.value.heading, 0.001f)
        assertEquals(362f, model.uiState.value.continuousHeading, 0.001f)
    }

    @Test
    fun telefonoParado_marcaInclinacion() = runTest {
        val sensor = FakeCompassSensor()
        val model = CompassViewModel(app, SavedStateHandle(), sensor)
        vm = model
        mantenerSuscripto(model.uiState)

        sensor.readings.emit(CompassReading.Orientation(0f, upX = 0f, upY = 0.9f, upZ = 0.1f, rotationQuarters = 0))

        val state = model.uiState.value
        assertTrue(state.isTilted)
        assertTrue(state.bubble.y < 0f)
    }

    @Test
    fun precisionBaja_pideCalibrarYAltaLoQuita() = runTest {
        val sensor = FakeCompassSensor()
        val model = CompassViewModel(app, SavedStateHandle(), sensor)
        vm = model
        mantenerSuscripto(model.uiState)

        sensor.readings.emit(CompassReading.Accuracy(SensorManager.SENSOR_STATUS_ACCURACY_LOW))
        assertEquals(CompassAccuracy.Low, model.uiState.value.accuracy)
        assertTrue(model.uiState.value.needsCalibration)

        sensor.readings.emit(CompassReading.Accuracy(SensorManager.SENSOR_STATUS_ACCURACY_HIGH))
        assertFalse(model.uiState.value.needsCalibration)
    }

    @Test
    fun sinSuscriptores_noRegistraSensores() = runTest {
        val sensor = FakeCompassSensor()
        val model = CompassViewModel(app, SavedStateHandle(), sensor)
        vm = model

        assertEquals(0, sensor.subscribers)
        mantenerSuscripto(model.uiState)
        assertEquals(1, sensor.subscribers)
    }

    @Test
    fun fijarRumbo_sinLectura_noHaceNada() = runTest {
        val model = CompassViewModel(app, SavedStateHandle(), FakeCompassSensor())
        vm = model
        mantenerSuscripto(model.uiState)

        model.lockBearing()

        assertNull(model.uiState.value.targetBearing)
    }

    @Test
    fun fijarRumbo_guardaElRumboRedondeadoYCalculaElDesvio() = runTest {
        val sensor = FakeCompassSensor()
        val handle = SavedStateHandle()
        val model = CompassViewModel(app, handle, sensor)
        vm = model
        mantenerSuscripto(model.uiState)

        sensor.readings.emit(flat(239.6f))
        model.lockBearing()
        assertEquals(240f, model.uiState.value.targetBearing!!, 0.001f)
        assertTrue(model.uiState.value.isOnCourse)

        // Mirando al norte, hay que girar 120° a la izquierda para volver a 240°.
        sensor.readings.emit(flat(0f))
        assertEquals(-120f, model.uiState.value.deviation!!, 0.001f)
        assertFalse(model.uiState.value.isOnCourse)

        // El rumbo fijado sobrevive a la muerte del proceso vía SavedStateHandle.
        assertEquals(240f, handle.get<Float>(CompassViewModel.KEY_TARGET_BEARING)!!, 0.001f)
    }

    @Test
    fun volverAlRumbo_emiteUnSoloEventoYNoAlFijar() = runTest {
        val sensor = FakeCompassSensor()
        val model = CompassViewModel(app, SavedStateHandle(), sensor)
        vm = model
        mantenerSuscripto(model.uiState)
        val events = mutableListOf<CompassEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.events.collect { events += it } }

        sensor.readings.emit(flat(90f))
        model.lockBearing()
        sensor.readings.emit(flat(91f))
        assertTrue("fijar el rumbo no debe vibrar", events.isEmpty())

        // Desvío leve (dentro de la histéresis): no arma el aviso.
        sensor.readings.emit(flat(97f))
        sensor.readings.emit(flat(90f))
        assertTrue(events.isEmpty())

        // Se aleja y vuelve: un solo aviso, aunque siga en rumbo.
        sensor.readings.emit(flat(130f))
        sensor.readings.emit(flat(93f))
        sensor.readings.emit(flat(90f))
        assertEquals(listOf<CompassEvent>(CompassEvent.BackOnCourse), events)
    }

    @Test
    fun quitarRumbo_limpiaElObjetivo() = runTest {
        val sensor = FakeCompassSensor()
        val model = CompassViewModel(app, SavedStateHandle(), sensor)
        vm = model
        mantenerSuscripto(model.uiState)

        sensor.readings.emit(flat(10f))
        model.lockBearing()
        model.clearBearing()

        assertNull(model.uiState.value.targetBearing)
        assertNull(model.uiState.value.deviation)
    }

    @Test
    fun displayedAt_elArcoTerminaEnLaMarcaAunqueLaAnimacionVayaAtrasada() {
        // Caso reportado en el teléfono: el sensor ya marca 129° pero el anillo animado sigue
        // en 180°, con el rumbo fijado en 180°. El arco tiene que medir 0°, no 51°.
        val sensorState = CompassUiState(hasReading = true, heading = 129f, targetBearing = 180f)
        assertEquals(0f, sensorState.displayedAt(180f).deviation!!, 0.001f)

        // Invariante: el extremo del arco (desde la línea de fe, a 0°) cae en el mismo ángulo
        // de pantalla que la marca del objetivo, que gira con el anillo (target - rumbo animado).
        for (target in listOf(0f, 45f, 180f, 359f)) {
            for (animated in listOf(-725f, -90f, 0f, 129.5f, 359f, 361f, 1080f)) {
                val shown = sensorState.copy(targetBearing = target).displayedAt(animated)
                val arcEnd = CompassMath.normalize(shown.deviation!!)
                val marker = CompassMath.normalize(target - animated)
                assertEquals("target=$target animated=$animated", marker, arcEnd, 0.01f)
            }
        }
    }

    @Test
    fun rumboGuardado_seRestauraAlRecrearElViewModel() = runTest {
        val handle = SavedStateHandle(mapOf(CompassViewModel.KEY_TARGET_BEARING to 45f))
        val model = CompassViewModel(app, handle, FakeCompassSensor())
        vm = model

        assertEquals(45f, model.uiState.value.targetBearing!!, 0.001f)
    }
}
