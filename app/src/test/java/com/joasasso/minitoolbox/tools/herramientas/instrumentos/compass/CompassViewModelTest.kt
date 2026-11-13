package com.joasasso.minitoolbox.tools.herramientas.instrumentos.compass

import android.app.Application
import android.hardware.SensorManager
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
        val model = CompassViewModel(app, FakeCompassSensor(isAvailable = false))
        vm = model
        assertFalse(model.uiState.value.isSupported)
        assertFalse(model.uiState.value.hasReading)
    }

    @Test
    fun lectura_actualizaRumboYDireccion() = runTest {
        val sensor = FakeCompassSensor()
        val model = CompassViewModel(app, sensor)
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
        val model = CompassViewModel(app, sensor)
        vm = model
        mantenerSuscripto(model.uiState)

        sensor.readings.emit(flat(0f, rotation = 1))

        assertEquals(90f, model.uiState.value.heading, 0.001f)
    }

    @Test
    fun cruzarElNorte_rumboContinuoNoSalta() = runTest {
        val sensor = FakeCompassSensor()
        val model = CompassViewModel(app, sensor)
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
        val model = CompassViewModel(app, sensor)
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
        val model = CompassViewModel(app, sensor)
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
        val model = CompassViewModel(app, sensor)
        vm = model

        assertEquals(0, sensor.subscribers)
        mantenerSuscripto(model.uiState)
        assertEquals(1, sensor.subscribers)
    }
}
