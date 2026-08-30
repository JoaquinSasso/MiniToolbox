package com.joasasso.minitoolbox.metrics.uploader

import com.google.android.gms.tasks.Tasks
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Resultado de un intento de envío, clasificado por si tiene sentido reintentarlo.
 *
 * La distinción es el corazón del arreglo: antes cualquier respuesta que no fuera 2xx
 * se trataba igual, así que un rechazo determinista (400 por una clave inválida) se
 * reintentaba con el mismo payload congelado, indefinidamente y sin dejar rastro.
 */
sealed interface UploadOutcome {

    /** 2xx. El lote fue aceptado y se puede hacer commit del delta. */
    data object Success : UploadOutcome

    /**
     * 4xx determinista. Reintentar el mismo payload no puede funcionar: hay que
     * corregir los datos de origen o descartar el lote.
     */
    data class PermanentReject(val code: Int, val error: String?) : UploadOutcome

    /**
     * 401 / 403. Problema de credenciales o de atestación, no de los datos.
     * No conviene reintentar de inmediato ni descartar el lote: cuando la app se
     * actualice o App Check vuelva a funcionar, el lote sigue siendo válido.
     */
    data class AuthError(val code: Int) : UploadOutcome

    /**
     * Red caída, 5xx, 408, 429, o falla al obtener el token de App Check.
     * El mismo payload puede funcionar más tarde.
     */
    data class Transient(val code: Int?, val cause: String?) : UploadOutcome
}

/**
 * Abstracción del envío. Existe para poder inyectar una implementación falsa en los
 * tests: con el POST embebido en el worker no había forma de verificar el manejo de
 * cada código de respuesta sin salir a la red.
 */
interface MetricsUploader {
    suspend fun post(endpoint: String, json: String): UploadOutcome
}

/**
 * Implementación real sobre HttpURLConnection.
 *
 * Autenticación exclusivamente por Firebase App Check desde la 1.3.2.
 */
class HttpMetricsUploader : MetricsUploader {

    override suspend fun post(endpoint: String, json: String): UploadOutcome {
        val token = fetchAppCheckToken()
            ?: return UploadOutcome.Transient(null, "appcheck_token_unavailable")

        val conn = try {
            (withContext(Dispatchers.IO) {
                URL(endpoint).openConnection()
            } as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("X-Firebase-AppCheck", token)
            }
        } catch (t: Throwable) {
            return UploadOutcome.Transient(null, t.javaClass.simpleName)
        }

        return try {
            conn.outputStream.use { it.write(json.toByteArray()) }
            val code = conn.responseCode
            classify(code, readBody(conn, code))
        } catch (t: Throwable) {
            UploadOutcome.Transient(null, t.javaClass.simpleName)
        } finally {
            try {
                conn.disconnect()
            } catch (_: Throwable) {
                // no-op
            }
        }
    }

    /**
     * El backend devuelve el motivo del rechazo en el cuerpo. Leerlo es lo que
     * permite diagnosticar sin depender de los logs del servidor.
     */
    private fun readBody(conn: HttpURLConnection, code: Int): String? = try {
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        stream?.bufferedReader()?.use { it.readText() }?.take(MAX_BODY_CHARS)
    } catch (_: Throwable) {
        null
    }

    private fun fetchAppCheckToken(): String? = try {
        Tasks.await(
            FirebaseAppCheck.getInstance().getAppCheckToken(false),
            APPCHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS
        ).token
    } catch (_: Throwable) {
        null
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 15_000
        private const val APPCHECK_TIMEOUT_SECONDS = 15L
        private const val MAX_BODY_CHARS = 300

        /**
         * Clasificación de códigos HTTP. Extraída del cliente para poder testearla
         * sin red.
         */
        fun classify(code: Int, body: String?): UploadOutcome = when {
            code in 200..299 -> UploadOutcome.Success
            code == 401 || code == 403 -> UploadOutcome.AuthError(code)
            code == 408 || code == 429 -> UploadOutcome.Transient(code, body)
            code in 400..499 -> UploadOutcome.PermanentReject(code, body)
            else -> UploadOutcome.Transient(code, body)
        }
    }
}