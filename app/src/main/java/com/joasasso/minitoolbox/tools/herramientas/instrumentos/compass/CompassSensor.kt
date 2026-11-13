package com.joasasso.minitoolbox.tools.herramientas.instrumentos.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.view.Display
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Lo que emite el sensor de la brújula. */
sealed interface CompassReading {
    /**
     * Orientación del teléfono. [azimuthDeg] es el azimut del borde superior en la orientación
     * natural; [upX], [upY] y [upZ] son el eje vertical del mundo en coordenadas del teléfono
     * (fila inferior de la matriz de rotación). [rotationQuarters] es la rotación de la pantalla.
     */
    data class Orientation(
        val azimuthDeg: Float,
        val upX: Float,
        val upY: Float,
        val upZ: Float,
        val rotationQuarters: Int
    ) : CompassReading

    /** Estado de precisión del magnetómetro (`SensorManager.SENSOR_STATUS_*`). */
    data class Accuracy(val status: Int) : CompassReading
}

/** Fuente de lecturas de la brújula. La interfaz es la costura que usan los tests. */
interface CompassSensor {
    /** `false` si el dispositivo no tiene magnetómetro. */
    val isAvailable: Boolean

    /** Registra los sensores mientras haya un colector y los libera al cancelarse. */
    fun readings(): Flow<CompassReading>
}

/**
 * Implementación con `TYPE_ROTATION_VECTOR` (fusión de giroscopio, acelerómetro y magnetómetro)
 * y, si el dispositivo no lo tiene, acelerómetro + magnetómetro con un filtro paso bajo.
 * El magnetómetro se registra en los dos casos para conocer su precisión.
 */
class AndroidCompassSensor(context: Context) : CompassSensor {

    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val displayManager = appContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val rotationVector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    override val isAvailable: Boolean =
        magnetometer != null && (rotationVector != null || accelerometer != null)

    override fun readings(): Flow<CompassReading> = callbackFlow {
        val mag = magnetometer
        if (!isAvailable || mag == null) {
            awaitClose { }
            return@callbackFlow
        }
        val useRotationVector = rotationVector != null

        val r = FloatArray(9)
        val orient = FloatArray(3)
        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasGeomagnetic = false
        var lastStatus: Int? = null

        fun emitOrientation() {
            SensorManager.getOrientation(r, orient)
            val rotation = displayManager.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: 0
            trySend(
                CompassReading.Orientation(
                    azimuthDeg = Math.toDegrees(orient[0].toDouble()).toFloat(),
                    upX = r[6],
                    upY = r[7],
                    upZ = r[8],
                    rotationQuarters = rotation
                )
            )
        }

        fun emitAccuracy(status: Int) {
            if (status != lastStatus) {
                lastStatus = status
                trySend(CompassReading.Accuracy(status))
            }
        }

        fun lowPass(input: FloatArray, output: FloatArray, initialized: Boolean) {
            if (!initialized) {
                input.copyInto(output)
                return
            }
            for (i in 0..2) output[i] += LOW_PASS_ALPHA * (input[i] - output[i])
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(r, event.values)
                        emitOrientation()
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        lowPass(event.values, gravity, hasGravity)
                        hasGravity = true
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        emitAccuracy(event.accuracy)
                        if (!useRotationVector) {
                            lowPass(event.values, geomagnetic, hasGeomagnetic)
                            hasGeomagnetic = true
                        }
                    }
                }
                if (!useRotationVector && hasGravity && hasGeomagnetic &&
                    event.sensor.type != Sensor.TYPE_ROTATION_VECTOR &&
                    SensorManager.getRotationMatrix(r, null, gravity, geomagnetic)
                ) {
                    emitOrientation()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) emitAccuracy(accuracy)
            }
        }

        if (useRotationVector) {
            sensorManager.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_UI)
            // Solo para conocer la precisión: el rotation vector ya usa el magnetómetro por dentro.
            sensorManager.registerListener(listener, mag, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(listener, mag, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose { sensorManager.unregisterListener(listener) }
    }

    private companion object {
        const val LOW_PASS_ALPHA = 0.15f
    }
}
