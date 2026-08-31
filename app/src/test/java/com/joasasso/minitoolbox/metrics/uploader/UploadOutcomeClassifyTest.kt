package com.joasasso.minitoolbox.metrics.uploader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica cómo se clasifica cada código HTTP.
 *
 * Es el test que cubre el bug original: antes, cualquier respuesta que no fuera 2xx
 * devolvía [androidx.work.ListenableWorker.Result.retry], así que un 400 determinista
 * reintentaba el mismo payload congelado para siempre.
 *
 * La distinción que importa es entre lo que tiene sentido reintentar y lo que no.
 */
class UploadOutcomeClassifyTest {

    private fun classify(code: Int) = HttpMetricsUploader.classify(code, null)

    @Test
    fun `2xx es exito`() {
        for (code in listOf(200, 201, 202, 204, 299)) {
            assertEquals("HTTP $code", UploadOutcome.Success, classify(code))
        }
    }

    @Test
    fun `4xx deterministas son rechazo permanente`() {
        for (code in listOf(400, 404, 409, 413, 422)) {
            val outcome = classify(code)
            assertTrue(
                "HTTP $code debería ser PermanentReject, fue $outcome",
                outcome is UploadOutcome.PermanentReject
            )
            assertEquals(code, (outcome as UploadOutcome.PermanentReject).code)
        }
    }

    @Test
    fun `401 y 403 son error de autenticacion`() {
        for (code in listOf(401, 403)) {
            val outcome = classify(code)
            assertTrue(
                "HTTP $code debería ser AuthError, fue $outcome",
                outcome is UploadOutcome.AuthError
            )
        }
    }

    @Test
    fun `408 y 429 son transitorios aunque sean 4xx`() {
        for (code in listOf(408, 429)) {
            val outcome = classify(code)
            assertTrue(
                "HTTP $code debería ser Transient, fue $outcome",
                outcome is UploadOutcome.Transient
            )
        }
    }

    @Test
    fun `5xx es transitorio`() {
        for (code in listOf(500, 502, 503, 504)) {
            val outcome = classify(code)
            assertTrue(
                "HTTP $code debería ser Transient, fue $outcome",
                outcome is UploadOutcome.Transient
            )
        }
    }

    @Test
    fun `los codigos inesperados caen en transitorio`() {
        // Un 3xx no debería ocurrir con un endpoint HTTPS fijo, pero si ocurre es
        // preferible reintentar que descartar datos del usuario.
        for (code in listOf(100, 301, 302)) {
            assertTrue(classify(code) is UploadOutcome.Transient)
        }
    }

    @Test
    fun `el cuerpo del error se conserva para diagnostico`() {
        val body = """{"ok":false,"error":"invalid_tools"}"""
        val outcome = HttpMetricsUploader.classify(400, body)

        assertEquals(body, (outcome as UploadOutcome.PermanentReject).error)
    }
}