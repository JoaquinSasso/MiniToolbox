package com.joasasso.minitoolbox.tools.herramientas.instrumentos.compass

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

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
    val accuracy: CompassAccuracy = CompassAccuracy.Unknown
) {
    val directionIndex: Int get() = CompassMath.directionIndex(heading)
    val isTilted: Boolean get() = hasReading && tiltDeg > CompassMath.TILT_WARNING_DEG
    val needsCalibration: Boolean
        get() = accuracy == CompassAccuracy.Low || accuracy == CompassAccuracy.Unreliable
}

class CompassViewModel @JvmOverloads constructor(
    application: Application,
    private val sensor: CompassSensor = AndroidCompassSensor(application)
) : AndroidViewModel(application) {

    private val initialState = CompassUiState(isSupported = sensor.isAvailable)

    /**
     * Los sensores quedan registrados mientras la UI esté suscripta. Con
     * `collectAsStateWithLifecycle` eso es mientras la pantalla esté en primer plano, más los
     * 5 s de gracia de `WhileSubscribed`, que evitan re-registrarlos al rotar.
     */
    val uiState: StateFlow<CompassUiState> = sensor.readings()
        .scan(initialState, ::reduce)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState)

    internal companion object {
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
