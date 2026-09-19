package com.joasasso.minitoolbox.tools.herramientas.instrumentos.arruler

import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Utilidad de formateo y redondeo adaptativo para distancias.
 */
object UnitFormat {

    private fun snap(value: Double, step: Double): Double = (value / step).roundToInt() * step

    fun format(meters: Double, units: Units, locale: Locale = Locale.getDefault()): String = when (units) {
        Units.METRIC -> when {
            // < 30 cm -> resolución 0,5 cm
            meters < 0.30 -> String.format(locale, "%.1f cm", snap(meters * 100.0, 0.5))
            // 30 cm a 1 m -> resolución 1 cm
            meters < 1.00 -> String.format(locale, "%.0f cm", snap(meters * 100.0, 1.0))
            // 1 m a 2 m -> resolución 1 cm
            meters < 2.00 -> String.format(locale, "%.2f m", snap(meters, 0.01))
            // > 2 m -> resolución 5 cm
            else -> String.format(locale, "%.2f m", snap(meters, 0.05))
        }

        Units.IMPERIAL -> {
            val totalIn = snap(meters / 0.0254, 0.25) // resolución 1/4"
            if (totalIn >= 12.0) {
                val ft = floor(totalIn / 12.0).toInt()
                val inch = totalIn - ft * 12.0
                String.format(locale, "%d' %.2f\"", ft, inch)
            } else {
                String.format(locale, "%.2f\"", totalIn)
            }
        }
    }
}

/**
 * Calcula la distancia euclidiana tridimensional entre dos puntos.
 */
fun dist3(a: FloatArray, b: FloatArray): Float {
    val dx = b[0] - a[0]
    val dy = b[1] - a[1]
    val dz = b[2] - a[2]
    return sqrt(dx * dx + dy * dy + dz * dz)
}

/**
 * Elige un paso de graduación legible: no más de ~20 marcas ni menos de 9 px entre ellas.
 * Devuelve null si el segmento es demasiado corto o no hay resolución suficiente en pantalla.
 */
fun chooseTickStep(meters: Float, pxLen: Float): Float? {
    if (pxLen < 90f || meters < 0.03f) return null
    val candidates = floatArrayOf(0.01f, 0.05f, 0.10f, 0.25f, 0.50f, 1.00f)
    return candidates.firstOrNull { s ->
        val count = meters / s
        count in 1.5f..20f && (pxLen / count) >= 9f
    }
}

/**
 * Calcula el centroide robusto de una lista de puntos muestreados:
 * 1. Calcula la mediana independiente por eje (X, Y, Z).
 * 2. Filtra los puntos descartando aquellos cuya distancia a la mediana supere [outlierThresholdM].
 * 3. Devuelve la media aritmética de las muestras conservadas (o de todas si quedaron menos de 2).
 */
fun robustCentroid(samples: List<FloatArray>, outlierThresholdM: Float = 0.03f): FloatArray {
    if (samples.isEmpty()) return FloatArray(3)
    val median = FloatArray(3)
    for (axis in 0..2) {
        val sorted = samples.map { it[axis] }.sorted()
        median[axis] = sorted[sorted.size / 2]
    }
    val kept = samples.filter { dist3(it, median) <= outlierThresholdM }
    val base = if (kept.size >= 2) kept else samples
    val out = FloatArray(3)
    base.forEach {
        out[0] += it[0]
        out[1] += it[1]
        out[2] += it[2]
    }
    out[0] /= base.size
    out[1] /= base.size
    out[2] /= base.size
    return out
}

/**
 * Aplana un punto contra la ecuación de un plano tridimensional definido por su vector normal
 * y un punto de traslación perteneciente al plano.
 * Retorna null si la distancia ortogonal al plano supera [maxDistanceM].
 */
fun snapToPlaneEquation(
    world: FloatArray,
    planeNormal: FloatArray,
    planeTranslation: FloatArray,
    maxDistanceM: Float = 0.08f
): FloatArray? {
    val d = (world[0] - planeTranslation[0]) * planeNormal[0] +
            (world[1] - planeTranslation[1]) * planeNormal[1] +
            (world[2] - planeTranslation[2]) * planeNormal[2]
    if (abs(d) > maxDistanceM) return null
    return floatArrayOf(
        world[0] - planeNormal[0] * d,
        world[1] - planeNormal[1] * d,
        world[2] - planeNormal[2] * d
    )
}

/**
 * Filtro de mediana móvil y dispersión espacial para la retícula AR.
 */
class LiveHitFilter(
    private val bufferSize: Int = 6,
    private val stableThresholdM: Float = 0.04f,
    private val minSamplesForStable: Int = 4
) {
    private val buf = ArrayDeque<FloatArray>()
    val value = FloatArray(3)
    var spread = Float.MAX_VALUE
        private set
    var stable = false
        private set

    fun push(p: FloatArray) {
        buf.addLast(p.clone())
        while (buf.size > bufferSize) buf.removeFirst()
        for (axis in 0..2) {
            val sorted = buf.map { it[axis] }.sorted()
            value[axis] = sorted[sorted.size / 2]
        }
        spread = buf.maxOf { dist3(it, value) }
        stable = buf.size >= minSamplesForStable && spread <= stableThresholdM
    }

    fun reset() {
        buf.clear()
        spread = Float.MAX_VALUE
        stable = false
        value[0] = 0f
        value[1] = 0f
        value[2] = 0f
    }
}

/**
 * Acumula muestras de un punto en múltiples frames para reducir el ruido del sensor.
 */
class HitSampler(
    val targetSamples: Int = 8,
    val minSamples: Int = 4,
    val timeoutFrames: Int = 25
) {
    var active = false
        private set
    var frames = 0
        private set
    val samples = mutableListOf<FloatArray>()

    fun start() {
        active = true
        frames = 0
        samples.clear()
    }

    fun addSample(sample: FloatArray) {
        if (active) {
            samples.add(sample.clone())
        }
    }

    fun advanceFrame() {
        if (active) {
            frames++
        }
    }

    val isComplete: Boolean
        get() = active && (samples.size >= targetSamples || frames >= timeoutFrames)

    val hasEnoughSamples: Boolean
        get() = samples.size >= minSamples

    fun stop() {
        active = false
        frames = 0
        samples.clear()
    }
}
