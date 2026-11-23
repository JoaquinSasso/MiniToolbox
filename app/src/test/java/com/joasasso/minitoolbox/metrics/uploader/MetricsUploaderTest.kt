package com.joasasso.minitoolbox.metrics.uploader

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetricsUploaderTest {

    @Test
    fun `classify clasifica respuestas exitosas 2xx`() {
        assertEquals(UploadOutcome.Success, HttpMetricsUploader.classify(200, "OK"))
        assertEquals(UploadOutcome.Success, HttpMetricsUploader.classify(204, null))
    }

    @Test
    fun `classify clasifica errores de auth 401 y 403`() {
        val auth401 = HttpMetricsUploader.classify(401, "Unauthorized")
        assertTrue(auth401 is UploadOutcome.AuthError && auth401.code == 401)

        val auth403 = HttpMetricsUploader.classify(403, "Forbidden")
        assertTrue(auth403 is UploadOutcome.AuthError && auth403.code == 403)
    }

    @Test
    fun `classify clasifica errores transitorios 408 y 429`() {
        val t408 = HttpMetricsUploader.classify(408, "Request Timeout")
        assertTrue(t408 is UploadOutcome.Transient && t408.code == 408)

        val t429 = HttpMetricsUploader.classify(429, "Too Many Requests")
        assertTrue(t429 is UploadOutcome.Transient && t429.code == 429)
    }

    @Test
    fun `classify clasifica rechazo permanente 400 por payload invalido`() {
        val outcome = HttpMetricsUploader.classify(400, "Bad Request: invalid key")
        assertTrue(outcome is UploadOutcome.PermanentReject && outcome.code == 400)
    }

    @Test
    fun `post sin token de App Check es transitorio y no sale a la red`() = runTest {
        var tokenRequests = 0
        val uploader = HttpMetricsUploader(tokenProvider = { tokenRequests++; null })

        // Un endpoint inválido: si post intentara conectar, el resultado traería la
        // excepción de red como causa en lugar del motivo del token.
        val outcome = uploader.post("http://invalid.invalid/ingest", "{}")

        assertEquals(UploadOutcome.Transient(null, "appcheck_token_unavailable"), outcome)
        assertEquals(1, tokenRequests)
    }

    @Test
    fun `cada post pide un token nuevo`() = runTest {
        // El backend consume cada token: reusar uno haría que el segundo envío
        // figure como replay. El proveedor tiene que consultarse en cada post.
        var tokenRequests = 0
        val uploader = HttpMetricsUploader(tokenProvider = { tokenRequests++; null })

        uploader.post("http://invalid.invalid/ingest", "{}")
        uploader.post("http://invalid.invalid/ingest", "{}")

        assertEquals(2, tokenRequests)
    }

    @Test
    fun `classify clasifica errores 5xx del servidor como transitorios`() {
        val outcome = HttpMetricsUploader.classify(500, "Internal Server Error")
        assertTrue(outcome is UploadOutcome.Transient && outcome.code == 500)
    }
}
