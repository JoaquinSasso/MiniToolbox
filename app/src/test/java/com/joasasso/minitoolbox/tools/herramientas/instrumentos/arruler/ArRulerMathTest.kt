package com.joasasso.minitoolbox.tools.herramientas.instrumentos.arruler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ArRulerMathTest {

    private val delta = 1e-4f

    @Test
    fun dist3_calculaDistanciaCorrecta() {
        val p1 = floatArrayOf(0f, 0f, 0f)
        val p2 = floatArrayOf(0f, 0f, 0f)
        assertEquals(0f, dist3(p1, p2), delta)

        val p3 = floatArrayOf(1f, 2f, 3f)
        val p4 = floatArrayOf(4f, 2f, 3f)
        assertEquals(3f, dist3(p3, p4), delta)

        // Diagonal 3D: dx=1, dy=2, dz=2 -> sqrt(1 + 4 + 4) = 3
        val p5 = floatArrayOf(1f, 2f, 3f)
        val p6 = floatArrayOf(2f, 4f, 5f)
        assertEquals(3f, dist3(p5, p6), delta)

        // Negativos
        val p7 = floatArrayOf(-1f, -1f, -1f)
        val p8 = floatArrayOf(1f, 1f, 1f)
        assertEquals(kotlin.math.sqrt(12f), dist3(p7, p8), delta)
    }

    @Test
    fun chooseTickStep_descartaCasosInvalidos() {
        // Longitud menor a 3 cm
        assertNull(chooseTickStep(meters = 0.02f, pxLen = 200f))

        // Longitud en pantalla menor a 90 px
        assertNull(chooseTickStep(meters = 1.0f, pxLen = 80f))
    }

    @Test
    fun chooseTickStep_seleccionaPasoApropiado() {
        // 1 metro en 500 px
        val step1m = chooseTickStep(meters = 1.0f, pxLen = 500f)
        assertNotNull(step1m)
        val count = 1.0f / step1m!!
        assertTrue("El número de ticks debe estar entre 1.5 y 20", count in 1.5f..20f)
        assertTrue("La distancia en px entre ticks debe ser >= 9px", (500f / count) >= 9f)

        // 5 metros en 1000 px
        val step5m = chooseTickStep(meters = 5.0f, pxLen = 1000f)
        assertNotNull(step5m)
        assertTrue((5.0f / step5m!!) in 1.5f..20f)
    }

    @Test
    fun robustCentroid_calculaCentroideYDescartaOutliers() {
        val empty = robustCentroid(emptyList())
        assertEquals(0f, empty[0], delta)

        val single = robustCentroid(listOf(floatArrayOf(1f, 2f, 3f)))
        assertEquals(1f, single[0], delta)
        assertEquals(2f, single[1], delta)
        assertEquals(3f, single[2], delta)

        // Cluster concentrado alrededor de (1.0, 1.0, 1.0) con un outlier lejano
        val samples = listOf(
            floatArrayOf(1.00f, 1.00f, 1.00f),
            floatArrayOf(1.01f, 1.00f, 1.00f),
            floatArrayOf(0.99f, 1.01f, 1.00f),
            floatArrayOf(1.00f, 0.99f, 1.01f),
            floatArrayOf(1.50f, 2.00f, 3.00f) // Outlier a > 3cm
        )
        val centroid = robustCentroid(samples, outlierThresholdM = 0.03f)
        // El outlier debe haber sido descartado, el centroide debe estar muy cerca de 1.0
        assertEquals(1.0f, centroid[0], 0.02f)
        assertEquals(1.0f, centroid[1], 0.02f)
        assertEquals(1.0f, centroid[2], 0.02f)
    }

    @Test
    fun snapToPlaneEquation_proyectaPuntoCorrectamente() {
        val normal = floatArrayOf(0f, 1f, 0f) // Plano horizontal en Y=0
        val center = floatArrayOf(0f, 0f, 0f)

        val pointNear = floatArrayOf(1f, 0.02f, 3f)
        val snapped = snapToPlaneEquation(pointNear, normal, center, maxDistanceM = 0.08f)
        assertNotNull(snapped)
        assertEquals(1f, snapped!![0], delta)
        assertEquals(0f, snapped[1], delta)
        assertEquals(3f, snapped[2], delta)

        // Punto demasiado lejos del plano
        val pointFar = floatArrayOf(1f, 0.15f, 3f)
        val rejected = snapToPlaneEquation(pointFar, normal, center, maxDistanceM = 0.08f)
        assertNull(rejected)
    }

    @Test
    fun liveHitFilter_estabilizaValoresYCalculaDispersion() {
        val filter = LiveHitFilter(bufferSize = 6, stableThresholdM = 0.04f, minSamplesForStable = 4)
        assertFalse(filter.stable)

        // Menos de 4 muestras no es estable
        filter.push(floatArrayOf(1f, 1f, 1f))
        filter.push(floatArrayOf(1f, 1f, 1f))
        filter.push(floatArrayOf(1f, 1f, 1f))
        assertFalse(filter.stable)

        // 4ta muestra coherente -> estable
        filter.push(floatArrayOf(1f, 1f, 1f))
        assertTrue(filter.stable)
        assertEquals(1f, filter.value[0], delta)
        assertEquals(0f, filter.spread, delta)

        // Muestra ruidosa que supera el umbral
        filter.push(floatArrayOf(1.2f, 1.2f, 1.2f))
        assertFalse(filter.stable)

        // Reset
        filter.reset()
        assertFalse(filter.stable)
        assertEquals(Float.MAX_VALUE, filter.spread, delta)
    }

    @Test
    fun hitSampler_controlaVentanaDeMuestras() {
        val sampler = HitSampler(targetSamples = 5, minSamples = 3, timeoutFrames = 10)
        assertFalse(sampler.active)

        sampler.start()
        assertTrue(sampler.active)
        assertFalse(sampler.isComplete)
        assertFalse(sampler.hasEnoughSamples)

        sampler.addSample(floatArrayOf(1f, 1f, 1f))
        sampler.addSample(floatArrayOf(1f, 1f, 1f))
        sampler.addSample(floatArrayOf(1f, 1f, 1f))
        assertTrue(sampler.hasEnoughSamples)
        assertFalse(sampler.isComplete)

        sampler.addSample(floatArrayOf(1f, 1f, 1f))
        sampler.addSample(floatArrayOf(1f, 1f, 1f))
        assertTrue(sampler.isComplete)

        sampler.stop()
        assertFalse(sampler.active)
        assertEquals(0, sampler.samples.size)
    }

    @Test
    fun unitFormat_formateaCorrectamenteEnMetrico() {
        val locale = Locale.US

        // < 30 cm -> resolución 0.5 cm
        assertEquals("12.5 cm", UnitFormat.format(0.124, Units.METRIC, locale))
        assertEquals("24.0 cm", UnitFormat.format(0.241, Units.METRIC, locale))

        // 30 cm a 1 m -> resolución 1 cm
        assertEquals("45 cm", UnitFormat.format(0.453, Units.METRIC, locale))
        assertEquals("89 cm", UnitFormat.format(0.887, Units.METRIC, locale))

        // 1 m a 2 m -> resolución 1 cm
        assertEquals("1.25 m", UnitFormat.format(1.254, Units.METRIC, locale))
        assertEquals("1.80 m", UnitFormat.format(1.798, Units.METRIC, locale))

        // > 2 m -> resolución 5 cm
        assertEquals("2.45 m", UnitFormat.format(2.46, Units.METRIC, locale))
        assertEquals("3.50 m", UnitFormat.format(3.52, Units.METRIC, locale))
    }

    @Test
    fun unitFormat_formateaCorrectamenteEnImperial() {
        val locale = Locale.US

        // Menor a 12 pulgadas (0.10 m = 3.937 in -> redondeo a 4.00")
        assertEquals("4.00\"", UnitFormat.format(0.10, Units.IMPERIAL, locale))

        // Mayor a 12 pulgadas (1.00 m = 39.37 in -> 3 pies, 3.37" -> snap 0.25 -> 3' 3.25")
        val formatted1m = UnitFormat.format(1.00, Units.IMPERIAL, locale)
        assertTrue(formatted1m.startsWith("3'"))
    }
}
