package com.joasasso.minitoolbox.tools.herramientas.instrumentos.compass

import android.hardware.SensorManager
import kotlin.math.acos

/** Precisión del magnetómetro, según lo que informa `onAccuracyChanged`. */
enum class CompassAccuracy { Unknown, Unreliable, Low, Medium, High }

/** Desplazamiento de la burbuja de nivel en coordenadas de pantalla (x a la derecha, y hacia abajo), en [-1, 1]. */
data class BubbleOffset(val x: Float, val y: Float)

/**
 * Cálculos de la brújula sin dependencias de UI ni de sensores, para testearlos en la JVM.
 * Todos los ángulos están en grados; los rumbos van de 0 (norte) a 360 en sentido horario.
 */
object CompassMath {

    /** Por encima de esta inclinación el azimut deja de ser confiable y se avisa al usuario. */
    const val TILT_WARNING_DEG = 30f

    /** Margen a cada lado del rumbo fijado dentro del cual se considera que el usuario va "en rumbo". */
    const val ON_COURSE_TOLERANCE_DEG = 5f

    /** Lleva cualquier ángulo al rango [0, 360). */
    fun normalize(deg: Float): Float {
        val r = ((deg % 360f) + 360f) % 360f
        return if (r >= 360f) 0f else r
    }

    /** Giro más corto para ir de [from] a [to], en (-180, 180]. Positivo = horario (a la derecha). */
    fun signedDelta(from: Float, to: Float): Float {
        val d = normalize(to - from)
        return if (d > 180f) d - 360f else d
    }

    /**
     * Ángulo continuo para animar el dial sin que dé una vuelta entera al cruzar el norte:
     * de 359° a 1° avanza 2°, no retrocede 358°.
     */
    fun unwrap(previousContinuous: Float, newHeading: Float): Float =
        previousContinuous + signedDelta(previousContinuous, newHeading)

    /**
     * Rumbo del borde superior de la pantalla. `getOrientation` da el azimut del borde superior
     * del teléfono en su orientación natural; con la pantalla rotada [rotationQuarters] cuartos
     * de vuelta (`Surface.ROTATION_*`), el frente que ve el usuario es otro borde.
     */
    fun screenHeading(azimuthDeg: Float, rotationQuarters: Int): Float =
        normalize(azimuthDeg + 90f * rotationQuarters)

    /** Índice del rumbo entre los 8 puntos (0 = N, 1 = NE, ... 7 = NO), con sectores de 45°. */
    fun directionIndex(heading: Float): Int = ((normalize(heading) + 22.5f) / 45f).toInt() % 8

    /**
     * Inclinación respecto de la horizontal. [upZ] es la componente Z del eje vertical del mundo
     * en coordenadas del teléfono (`R[8]` de la matriz de rotación): 1 con el teléfono plano
     * boca arriba, 0 parado.
     */
    fun tiltDeg(upZ: Float): Float =
        Math.toDegrees(acos(upZ.coerceIn(-1f, 1f).toDouble())).toFloat()

    /**
     * Posición de la burbuja de nivel. La burbuja sube hacia el borde levantado, así que sigue
     * al eje vertical del mundo proyectado sobre la pantalla (`R[6]`, `R[7]`), rotado según la
     * orientación de la pantalla. La magnitud es el seno de la inclinación.
     */
    fun bubbleOffset(upX: Float, upY: Float, rotationQuarters: Int): BubbleOffset {
        val (x, yUp) = when (((rotationQuarters % 4) + 4) % 4) {
            1 -> -upY to upX
            2 -> -upX to -upY
            3 -> upY to -upX
            else -> upX to upY
        }
        return BubbleOffset(x.coerceIn(-1f, 1f), (-yUp).coerceIn(-1f, 1f))
    }

    /** Traduce `SensorManager.SENSOR_STATUS_*` a [CompassAccuracy]. */
    fun accuracyFromStatus(status: Int): CompassAccuracy = when (status) {
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> CompassAccuracy.High
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> CompassAccuracy.Medium
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> CompassAccuracy.Low
        SensorManager.SENSOR_STATUS_UNRELIABLE -> CompassAccuracy.Unreliable
        else -> CompassAccuracy.Unknown
    }
}
