package com.joasasso.minitoolbox.tools.herramientas.instrumentos.compass

import android.hardware.SensorManager
import org.junit.Assert.assertEquals
import org.junit.Test

class CompassMathTest {

    private val eps = 0.001f

    @Test
    fun normalize_llevaAlRango0a360() {
        assertEquals(0f, CompassMath.normalize(0f), eps)
        assertEquals(0f, CompassMath.normalize(360f), eps)
        assertEquals(350f, CompassMath.normalize(-10f), eps)
        assertEquals(10f, CompassMath.normalize(730f), eps)
        // Un negativo diminuto no debe redondear a 360 (fuera de rango).
        assertEquals(true, CompassMath.normalize(-1e-6f) < 360f)
    }

    @Test
    fun signedDelta_tomaElGiroMasCorto() {
        assertEquals(2f, CompassMath.signedDelta(359f, 1f), eps)
        assertEquals(-2f, CompassMath.signedDelta(1f, 359f), eps)
        assertEquals(90f, CompassMath.signedDelta(0f, 90f), eps)
        assertEquals(-90f, CompassMath.signedDelta(0f, 270f), eps)
        assertEquals(180f, CompassMath.signedDelta(0f, 180f), eps)
        assertEquals(0f, CompassMath.signedDelta(42f, 42f), eps)
    }

    @Test
    fun unwrap_noDaLaVueltaAlCruzarElNorte() {
        assertEquals(361f, CompassMath.unwrap(359f, 1f), eps)
        assertEquals(-1f, CompassMath.unwrap(1f, 359f), eps)
        // Acumula vueltas: el valor continuo puede salir de [0, 360).
        assertEquals(725f, CompassMath.unwrap(719f, 5f), eps)
    }

    @Test
    fun screenHeading_sumaUnCuartoDeVueltaPorRotacion() {
        assertEquals(10f, CompassMath.screenHeading(10f, 0), eps)
        assertEquals(100f, CompassMath.screenHeading(10f, 1), eps)
        assertEquals(190f, CompassMath.screenHeading(10f, 2), eps)
        assertEquals(280f, CompassMath.screenHeading(10f, 3), eps)
        // getOrientation devuelve azimuts negativos al oeste.
        assertEquals(270f, CompassMath.screenHeading(-90f, 0), eps)
    }

    @Test
    fun directionIndex_respetaLosLimitesDeSector() {
        assertEquals(0, CompassMath.directionIndex(0f))
        assertEquals(0, CompassMath.directionIndex(22.4f))
        assertEquals(1, CompassMath.directionIndex(22.5f))
        assertEquals(2, CompassMath.directionIndex(90f))
        assertEquals(4, CompassMath.directionIndex(180f))
        assertEquals(7, CompassMath.directionIndex(337.4f))
        assertEquals(0, CompassMath.directionIndex(337.5f))
        assertEquals(0, CompassMath.directionIndex(359.9f))
    }

    @Test
    fun tiltDeg_planoEs0YParadoEs90() {
        assertEquals(0f, CompassMath.tiltDeg(1f), eps)
        assertEquals(90f, CompassMath.tiltDeg(0f), eps)
        assertEquals(60f, CompassMath.tiltDeg(0.5f), 0.01f)
        // Valores fuera de [-1, 1] por ruido numérico no deben dar NaN.
        assertEquals(0f, CompassMath.tiltDeg(1.0001f), eps)
    }

    @Test
    fun bubbleOffset_subeHaciaElBordeLevantado() {
        // Borde superior levantado: el eje vertical tiene componente +Y en el teléfono.
        val portrait = CompassMath.bubbleOffset(upX = 0f, upY = 0.5f, rotationQuarters = 0)
        assertEquals(0f, portrait.x, eps)
        assertEquals(-0.5f, portrait.y, eps) // y de pantalla crece hacia abajo

        // Pantalla rotada 90°: el borde derecho natural (+X) queda arriba.
        val landscape = CompassMath.bubbleOffset(upX = 0.5f, upY = 0f, rotationQuarters = 1)
        assertEquals(0f, landscape.x, eps)
        assertEquals(-0.5f, landscape.y, eps)

        val upsideDown = CompassMath.bubbleOffset(upX = 0.3f, upY = 0f, rotationQuarters = 2)
        assertEquals(-0.3f, upsideDown.x, eps)

        val reverseLandscape = CompassMath.bubbleOffset(upX = 0f, upY = 0.4f, rotationQuarters = 3)
        assertEquals(0.4f, reverseLandscape.x, eps)
    }

    @Test
    fun accuracyFromStatus_mapeaLosEstadosDelSensor() {
        assertEquals(CompassAccuracy.High, CompassMath.accuracyFromStatus(SensorManager.SENSOR_STATUS_ACCURACY_HIGH))
        assertEquals(CompassAccuracy.Medium, CompassMath.accuracyFromStatus(SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM))
        assertEquals(CompassAccuracy.Low, CompassMath.accuracyFromStatus(SensorManager.SENSOR_STATUS_ACCURACY_LOW))
        assertEquals(CompassAccuracy.Unreliable, CompassMath.accuracyFromStatus(SensorManager.SENSOR_STATUS_UNRELIABLE))
        assertEquals(CompassAccuracy.Unknown, CompassMath.accuracyFromStatus(SensorManager.SENSOR_STATUS_NO_CONTACT))
    }
}
