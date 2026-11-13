package com.joasasso.minitoolbox.tools.herramientas.instrumentos.compass

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlin.math.abs
import kotlin.math.roundToInt

data class CompassUiState(
    /** `false` si el dispositivo no tiene magnetómetro. */
    val isSupported: Boolean = true,
    /** `false` hasta recibir la primera lectura de orientación. */
    val hasReading: Boolean = false,
    /** Rumbo del borde superior de la pantalla, en [0, 360). */
    val heading: Float = 0f,
    /** Igual que [heading] pero continuo (sin saltos en 0/360), para animar el dial. */
    val continuousHeading: Float = 0f,
    val tiltDeg: Float = 0f,
    val bubble: BubbleOffset = BubbleOffset(0f, 0f),
    val accuracy: CompassAccuracy = CompassAccuracy.Unknown,
    /** Rumbo fijado por el usuario, o `null` si no hay ninguno. */
    val targetBearing: Float? = null
) {
    val directionIndex: Int get() = CompassMath.directionIndex(heading)
    val isTilted: Boolean get() = hasReading && tiltDeg > CompassMath.TILT_WARNING_DEG
    val needsCalibration: Boolean
        get() = accuracy == CompassAccuracy.Low || accuracy == CompassAccuracy.Unreliable

    /** Cuánto hay que girar para volver al rumbo fijado. Positivo = a la derecha. */
    val deviation: Float?
        get() = if (hasReading && targetBearing != null) CompassMath.signedDelta(heading, targetBearing) else null

    val isOnCourse: Boolean
        get() = deviation?.let { abs(it) <= CompassMath.ON_COURSE_TOLERANCE_DEG } ?: false
}

sealed interface CompassEvent {
    /** El usuario se había desviado del rumbo fijado y volvió a él. */
    data object BackOnCourse : CompassEvent
}

class CompassViewModel @JvmOverloads constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val sensor: CompassSensor = AndroidCompassSensor(application)
) : AndroidViewModel(application) {

    private val initialState = CompassUiState(
        isSupported = sensor.isAvailable,
        targetBearing = savedStateHandle[KEY_TARGET_BEARING]
    )

    private val _events = Channel<CompassEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /**
     * Se arma al alejarse más de [OFF_COURSE_DEG] del rumbo y avisa al volver a menos de
     * [CompassMath.ON_COURSE_TOLERANCE_DEG]. La histéresis evita vibrar una y otra vez en el borde.
     */
    private var backOnCourseArmed = false

    private val sensorState = sensor.readings().scan(initialState, ::reduce)

    /**
     * Los sensores quedan registrados mientras la UI esté suscripta. Con
     * `collectAsStateWithLifecycle` eso es mientras la pantalla esté en primer plano, más los
     * 5 s de gracia de `WhileSubscribed`, que evitan re-registrarlos al rotar.
     */
    val uiState: StateFlow<CompassUiState> = combine(
        sensorState,
        savedStateHandle.getStateFlow<Float?>(KEY_TARGET_BEARING, null)
    ) { state, target -> state.copy(targetBearing = target) }
        .onEach(::trackCourse)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState)

    /** Fija como objetivo el rumbo actual, redondeado al grado. Sin lectura no hace nada. */
    fun lockBearing() {
        val state = uiState.value
        if (!state.hasReading) return
        backOnCourseArmed = false
        savedStateHandle[KEY_TARGET_BEARING] = (state.heading.roundToInt() % 360).toFloat()
    }

    fun clearBearing() {
        backOnCourseArmed = false
        savedStateHandle[KEY_TARGET_BEARING] = null
    }

    private fun trackCourse(state: CompassUiState) {
        val deviation = state.deviation ?: return
        if (abs(deviation) > OFF_COURSE_DEG) {
            backOnCourseArmed = true
        } else if (backOnCourseArmed && state.isOnCourse) {
            backOnCourseArmed = false
            _events.trySend(CompassEvent.BackOnCourse)
        }
    }

    internal companion object {
        const val KEY_TARGET_BEARING = "compass_target_bearing"

        /** Desvío a partir del cual se considera que el usuario dejó el rumbo. */
        const val OFF_COURSE_DEG = 10f

        fun reduce(state: CompassUiState, reading: CompassReading): CompassUiState = when (reading) {
            is CompassReading.Accuracy ->
                state.copy(accuracy = CompassMath.accuracyFromStatus(reading.status))

            is CompassReading.Orientation -> {
                val heading = CompassMath.screenHeading(reading.azimuthDeg, reading.rotationQuarters)
                state.copy(
                    hasReading = true,
                    heading = heading,
                    continuousHeading = if (state.hasReading) {
                        CompassMath.unwrap(state.continuousHeading, heading)
                    } else {
                        heading
                    },
                    tiltDeg = CompassMath.tiltDeg(reading.upZ),
                    bubble = CompassMath.bubbleOffset(reading.upX, reading.upY, reading.rotationQuarters)
                )
            }
        }
    }
}
