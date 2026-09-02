package com.joasasso.minitoolbox.metrics.uploader

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import android.util.Log
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.joasasso.minitoolbox.metrics.MetricsContract
import com.joasasso.minitoolbox.metrics.storage.JsonUtils
import com.joasasso.minitoolbox.metrics.storage.MetricsKeys
import com.joasasso.minitoolbox.metrics.storage.MetricsSanitizer
import com.joasasso.minitoolbox.metrics.storage.metricsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.joasasso.minitoolbox.TestApplication
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Verifica que ningún camino del worker pueda dejar el pipeline detenido para siempre.
 *
 * El bug original tenía dos formas: cualquier no-2xx devolvía retry, así que un rechazo
 * determinista reintentaba el mismo payload congelado indefinidamente; y las validaciones
 * previas devolvían success sin enviar nada, apagando el sistema en silencio.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class UploadMetricsWorkerTest {

    private lateinit var context: Context

    private val endpoint = "https://example.invalid/ingest"

    /** Uploader falso: devuelve siempre el mismo resultado y cuenta las llamadas. */
    private class FakeUploader(private val outcome: UploadOutcome) : MetricsUploader {
        var calls = 0
            private set
        var lastPayload: String? = null
            private set

        override suspend fun post(endpoint: String, json: String): UploadOutcome {
            calls++
            lastPayload = json
            return outcome
        }
    }

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()

        // El worker puede reagendar tras un envío exitoso. Sin esto,
        // WorkManager.getInstance falla porque su inicialización automática
        // no corre en tests unitarios.
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .setExecutor(SynchronousExecutor())
                .build()
        )

        context.metricsDataStore.edit { it.clear() }
    }

    @After
    fun tearDown() {
        UploadMetricsWorker.uploaderFactory = null
    }

    private suspend fun runWorker(
        outcome: UploadOutcome,
        runAttemptCount: Int = 0
    ): Pair<ListenableWorker.Result, FakeUploader> {
        val fake = FakeUploader(outcome)
        UploadMetricsWorker.uploaderFactory = { fake }

        val worker = TestListenableWorkerBuilder<UploadMetricsWorker>(context)
            .setInputData(workDataOf("endpoint" to endpoint))
            .setRunAttemptCount(runAttemptCount)
            .build()

        return worker.doWork() to fake
    }

    /** Deja el esquema al día para que runIfNeeded no interfiera con lo que se prueba. */
    private suspend fun markSchemaCurrent() {
        context.metricsDataStore.edit {
            it[MetricsKeys.SCHEMA_VERSION] = MetricsSanitizer.SCHEMA_VERSION
        }
    }

    private suspend fun seedPendingUsage(toolKey: String, count: Int = 3) {
        context.metricsDataStore.edit {
            it[MetricsKeys.TOOL_USE_BY_DAY_JSON] =
                JsonUtils.toDayNestedIntMap(mapOf(today() to mapOf(toolKey to count)))
        }
    }

    private suspend fun prefs() = context.metricsDataStore.data.first()

    // -----------------------------------------------------------------

    @Test
    fun `sin datos pendientes no se llama al backend`() = runTest {
        markSchemaCurrent()

        val (result, fake) = runWorker(UploadOutcome.Success)

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(0, fake.calls)
    }

    @Test
    fun `un envio exitoso limpia el pendiente y registra el estado`() = runTest {
        markSchemaCurrent()
        seedPendingUsage("water")

        val (result, fake) = runWorker(UploadOutcome.Success)

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(1, fake.calls)

        val p = prefs()
        assertEquals("", p[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON])
        assertEquals(200, p[MetricsKeys.LAST_UPLOAD_CODE])
        assertEquals(0, p[MetricsKeys.CONSECUTIVE_FAILURES])
        assertTrue((p[MetricsKeys.LAST_SUCCESS_AT] ?: 0L) > 0L)
    }

    @Test
    fun `un error transitorio reintenta y conserva el lote`() = runTest {
        markSchemaCurrent()
        seedPendingUsage("water")

        val (result, _) = runWorker(UploadOutcome.Transient(500, "boom"))

        assertEquals(ListenableWorker.Result.retry(), result)

        val p = prefs()
        assertTrue(
            "el lote debe conservarse para el próximo intento",
            p[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON].orEmpty().isNotBlank()
        )
        assertEquals(1, p[MetricsKeys.CONSECUTIVE_FAILURES])
    }

    @Test
    fun `tras agotar los intentos se descarta el lote y se desbloquea la cola`() = runTest {
        markSchemaCurrent()
        seedPendingUsage("water")

        val (result, _) = runWorker(UploadOutcome.Transient(500, "boom"), runAttemptCount = 5)

        assertEquals(ListenableWorker.Result.success(), result)

        val p = prefs()
        assertEquals("", p[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON])
        assertEquals(1, p[MetricsKeys.DROPPED_BATCHES])
    }

    @Test
    fun `un error de autenticacion no reintenta ni descarta datos`() = runTest {
        markSchemaCurrent()
        seedPendingUsage("water")

        val (result, _) = runWorker(UploadOutcome.AuthError(401))

        assertEquals(
            "reintentar con la misma credencial sólo gasta batería",
            ListenableWorker.Result.success(),
            result
        )

        val p = prefs()
        assertTrue(
            "los datos siguen siendo válidos para cuando App Check vuelva a funcionar",
            p[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON].orEmpty().isNotBlank()
        )
        assertEquals(401, p[MetricsKeys.LAST_UPLOAD_CODE])
        assertEquals(false, p[MetricsKeys.LAST_UPLOAD_USED_APPCHECK])
    }

    @Test
    fun `un rechazo permanente nunca devuelve retry`() = runTest {
        markSchemaCurrent()
        seedPendingUsage("water")

        val (result, _) = runWorker(UploadOutcome.PermanentReject(400, "invalid_tools"))

        assertEquals(
            "retry reenviaría exactamente el mismo payload rechazado, para siempre",
            ListenableWorker.Result.success(),
            result
        )
    }

    /**
     * Regresión del incidente completo: un lote congelado con una clave de ruta que el
     * backend rechaza. Antes, el dispositivo quedaba mudo de forma permanente.
     */
    @Test
    fun `un lote envenenado se sanea y el siguiente ciclo envia datos validos`() = runTest {
        markSchemaCurrent()
        val poisoned = "pomodoro/detail/2f7a1b3c-4d5e-6f70-8a9b-0c1d2e3f4a5b"
        seedPendingUsage(poisoned, count = 3)

        // Primer ciclo: el backend rechaza el lote.
        val (firstResult, _) = runWorker(UploadOutcome.PermanentReject(400, "invalid_tools"))
        assertEquals(ListenableWorker.Result.success(), firstResult)

        val afterReject = prefs()
        assertEquals(
            "el lote rechazado no debe quedar congelado",
            "",
            afterReject[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON]
        )

        val tools = JsonUtils.fromDayNestedIntMap(afterReject[MetricsKeys.TOOL_USE_BY_DAY_JSON])
        val keys = tools.values.flatMap { it.keys }
        assertTrue(
            "el origen quedó sin sanear: el próximo lote regeneraría el mismo veneno",
            keys.isNotEmpty() && keys.all { MetricsContract.isValidKey(it) }
        )

        // Segundo ciclo: ahora el backend acepta.
        val (secondResult, fake) = runWorker(UploadOutcome.Success)
        assertEquals(ListenableWorker.Result.success(), secondResult)
        assertEquals(1, fake.calls)

        val payload = fake.lastPayload.orEmpty()
        assertTrue("el payload debe llevar la clave normalizada", payload.contains("pomodoro_detail"))
        assertTrue("no debe quedar rastro de la clave con barras", !payload.contains(poisoned))
    }

    @Test
    fun `el uso de una herramienta genera tambien su marca de dispositivo-dia`() = runTest {
        markSchemaCurrent()
        // Tres usos de la misma herramienta en el mismo día: 3 usos, 1 dispositivo-día.
        val repo = com.joasasso.minitoolbox.metrics.storage.AggregatesRepository(context)
        repeat(3) { repo.incrementToolUse("water") }

        val (_, fake) = runWorker(UploadOutcome.Success)

        val payload = org.json.JSONObject(fake.lastPayload.orEmpty())
        val item = payload.getJSONArray("items").getJSONObject(0)

        assertEquals(3, item.getJSONObject("tools").getInt("water"))
        assertEquals(
            "el DAU se marca, no se acumula: es el denominador de tools",
            1,
            item.getJSONObject("tools_dau").getInt("water")
        )
    }

    @Test
    fun `el origen de la apertura viaja en la clave compuesta`() = runTest {
        markSchemaCurrent()
        val repo = com.joasasso.minitoolbox.metrics.storage.AggregatesRepository(context)
        repo.incrementToolUse("water", com.joasasso.minitoolbox.metrics.MetricsSource.WIDGET)
        repo.incrementToolUse("water", com.joasasso.minitoolbox.metrics.MetricsSource.NAV)

        val (_, fake) = runWorker(UploadOutcome.Success)

        val item = org.json.JSONObject(fake.lastPayload.orEmpty())
            .getJSONArray("items").getJSONObject(0)
        val entry = item.getJSONObject("tool_entry")

        assertEquals(1, entry.getInt("water.widget"))
        assertEquals(1, entry.getInt("water.nav"))
        assertEquals(
            "el total de aperturas por origen debe cuadrar con el contador de usos",
            2,
            item.getJSONObject("tools").getInt("water")
        )
    }

    @Test
    fun `un origen desconocido no crea claves nuevas`() = runTest {
        markSchemaCurrent()
        val repo = com.joasasso.minitoolbox.metrics.storage.AggregatesRepository(context)
        repo.incrementToolUse("water", "origen_inventado")

        val (_, fake) = runWorker(UploadOutcome.Success)

        val entry = org.json.JSONObject(fake.lastPayload.orEmpty())
            .getJSONArray("items").getJSONObject(0).getJSONObject("tool_entry")

        assertEquals(
            "los orígenes libres explotarían la cardinalidad del documento diario",
            1,
            entry.getInt("water.unknown")
        )
    }

    @Test
    fun `el payload incluye la version de esquema y la salud del cliente`() = runTest {
        markSchemaCurrent()
        seedPendingUsage("water")

        val (_, fake) = runWorker(UploadOutcome.Success)

        val payload = fake.lastPayload.orEmpty()
        assertTrue(payload.contains("schema_version"))
        assertTrue(payload.contains("client_health"))
    }
}