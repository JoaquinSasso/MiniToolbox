package com.joasasso.minitoolbox.metrics.uploader

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.datastore.preferences.core.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.joasasso.minitoolbox.metrics.MetricsContract
import com.joasasso.minitoolbox.metrics.storage.AggregatesRepository
import com.joasasso.minitoolbox.metrics.storage.MetricsKeys
import com.joasasso.minitoolbox.metrics.storage.MetricsSanitizer
import com.joasasso.minitoolbox.metrics.storage.metricsDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Envía los agregados locales pendientes al backend de métricas.
 *
 * Invariante central: **ningún camino puede dejar el pipeline detenido para siempre**.
 * Todo error o bien corrige los datos y sigue, o bien descarta el lote y sigue. Antes
 * de este rediseño había dos formas de bloqueo permanente:
 *
 *  - Cualquier respuesta no-2xx devolvía [Result.retry], así que un 400 determinista
 *    reintentaba el mismo payload congelado indefinidamente.
 *  - Las validaciones previas devolvían [Result.success] sin enviar nada, apagando el
 *    sistema en silencio y reportando éxito.
 */
class UploadMetricsWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    private val uploader: MetricsUploader = uploaderFactory?.invoke() ?: HttpMetricsUploader()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val ctx = applicationContext

        // Barato tras la primera pasada. Garantiza que el worker nunca opere sobre
        // datos sin sanear, incluso si arrancó por un camino inesperado.
        MetricsSanitizer.runIfNeeded(ctx)

        val endpoint = inputData.getString("endpoint") ?: return@withContext Result.failure()
        if (!endpoint.startsWith("https://")) return@withContext Result.failure()

        val repo = AggregatesRepository(ctx)
        val ds = ctx.metricsDataStore
        val prefs = ds.data.first()

        var batchId = prefs[MetricsKeys.PENDING_BATCH_ID].orEmpty()
        var payloadJson = prefs[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON].orEmpty()

        // Caducidad del lote congelado: si lleva demasiado tiempo sin poder enviarse,
        // se reconstruye en lugar de arrastrarlo eternamente.
        val createdAt = prefs[MetricsKeys.PENDING_BATCH_CREATED_AT] ?: 0L
        if (payloadJson.isNotBlank() &&
            createdAt > 0L &&
            System.currentTimeMillis() - createdAt > MAX_PENDING_AGE_MS
        ) {
            payloadJson = ""
        }

        val deltas: List<AggregatesRepository.DayDelta>

        if (payloadJson.isBlank()) {
            // Se corrigen los deltas en lugar de abortar el envío ante cualquier anomalía.
            val built = repo.buildDeltasSinceLastSent().mapNotNull { sanitizeDelta(it) }
            if (built.isEmpty()) {
                clearPending(ds)
                UploadScheduler.clearDirty(ctx)
                return@withContext Result.success()
            }

            // Se reusa el batch_id previo si existía: la deduplicación del backend evita
            // el doble conteo si el lote anterior sí había sido persistido.
            if (batchId.isBlank()) batchId = UUID.randomUUID().toString()

            payloadJson = buildPayload(batchId, built, prefs)
            deltas = built

            ds.edit { e ->
                e[MetricsKeys.PENDING_BATCH_ID] = batchId
                e[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON] = payloadJson
                e[MetricsKeys.PENDING_BATCH_CREATED_AT] = System.currentTimeMillis()
            }
        } else {
            deltas = parseDeltasFromPendingPayload(payloadJson)
        }

        when (val outcome = uploader.post(endpoint, payloadJson)) {

            is UploadOutcome.Success -> {
                repo.commitSent(deltas)
                clearPending(ds)
                ds.edit { e ->
                    e[MetricsKeys.LAST_UPLOAD_CODE] = 200
                    e[MetricsKeys.LAST_UPLOAD_ERROR] = ""
                    e[MetricsKeys.LAST_SUCCESS_AT] = System.currentTimeMillis()
                    e[MetricsKeys.CONSECUTIVE_FAILURES] = 0
                    e[MetricsKeys.LAST_UPLOAD_USED_APPCHECK] = true
                }

                if (repo.buildDeltasSinceLastSent().isNotEmpty()) {
                    UploadScheduler.markDirty(ctx)
                    UploadScheduler.maybeSchedule(ctx, endpoint)
                } else {
                    UploadScheduler.clearDirty(ctx)
                }
                Result.success()
            }

            is UploadOutcome.PermanentReject -> {
                recordFailure(ds, outcome.code, outcome.error)

                // ORDEN CRÍTICO: primero se sanea el origen, después se descarta el
                // pendiente. Al revés, buildDeltasSinceLastSent regeneraría exactamente
                // el mismo payload rechazado y el ciclo no terminaría nunca.
                val report = MetricsSanitizer.sanitizeNow(ctx, discardPendingPayload = true)

                if (!report.changed || runAttemptCount >= MAX_ATTEMPTS) {
                    // El saneo no encontró nada que corregir y el servidor igual rechaza:
                    // se corta el bucle aceptando la pérdida de este delta.
                    repo.commitSent(deltas)
                    clearPending(ds)
                    ds.edit { e ->
                        e[MetricsKeys.DROPPED_BATCHES] = (e[MetricsKeys.DROPPED_BATCHES] ?: 0) + 1
                    }
                }

                // Nunca retry: el próximo disparo por cota lo reintenta ya corregido.
                Result.success()
            }

            is UploadOutcome.AuthError -> {
                recordFailure(ds, outcome.code, "auth")
                ds.edit { e -> e[MetricsKeys.LAST_UPLOAD_USED_APPCHECK] = false }
                // El lote sigue siendo válido: se conserva para cuando la atestación
                // vuelva a funcionar. Reintentar ahora sólo gastaría batería.
                Result.success()
            }

            is UploadOutcome.Transient -> {
                recordFailure(ds, outcome.code ?: -1, outcome.cause)
                if (runAttemptCount >= MAX_ATTEMPTS) {
                    clearPending(ds)
                    ds.edit { e ->
                        e[MetricsKeys.DROPPED_BATCHES] = (e[MetricsKeys.DROPPED_BATCHES] ?: 0) + 1
                    }
                    Result.success()
                } else {
                    Result.retry()
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Construcción del payload
    // ---------------------------------------------------------------------

    private fun buildPayload(
        batchId: String,
        deltas: List<AggregatesRepository.DayDelta>,
        prefs: androidx.datastore.preferences.core.Preferences
    ): String {
        val itemsArr = org.json.JSONArray()
        for (d in deltas) {
            val obj = org.json.JSONObject()
                .put("day", d.day)
                .put("app_open", d.appOpen)
                .put("daily_active", d.dailyActive)
                .put("tools", org.json.JSONObject(d.tools as Map<*, *>))
                .put("ads", org.json.JSONObject(d.ads as Map<*, *>))
                .put("versions", org.json.JSONObject(d.versions as Map<*, *>))
                .put("versions_first_seen", org.json.JSONObject(d.versionsFirstSeen as Map<*, *>))
                .put("lang_primary", org.json.JSONObject(d.langPrimary as Map<*, *>))
                .put("lang_secondary", org.json.JSONObject(d.langSecondary as Map<*, *>))
                .put("widgets", org.json.JSONObject(d.widgets as Map<*, *>))
            itemsArr.put(obj)
        }

        // Salud del cliente: sin esto no hay forma de detectar dispositivos que
        // dejaron de reportar. El backend ignora los campos que no conoce.
        val health = org.json.JSONObject()
            .put("dropped_batches", prefs[MetricsKeys.DROPPED_BATCHES] ?: 0)
            .put("sanitized_keys", prefs[MetricsKeys.SANITIZED_KEYS_TOTAL] ?: 0)
            .put("consecutive_failures", prefs[MetricsKeys.CONSECUTIVE_FAILURES] ?: 0)

        return org.json.JSONObject()
            .put("batch_id", batchId)
            .put("platform", "android")
            .put("app_version", safeVersionName())
            .put("client_health", health)
            .put("items", itemsArr)
            .toString()
    }

    /**
     * Corrige un delta en lugar de descartar el envío completo.
     * Devuelve null sólo si el día es irrecuperable.
     */
    private fun sanitizeDelta(d: AggregatesRepository.DayDelta): AggregatesRepository.DayDelta? {
        if (!MetricsContract.isValidDay(d.day)) return null

        fun clean(m: MutableMap<String, Int>): MutableMap<String, Int> =
            MetricsContract.sanitizeCounters(m).clean

        return AggregatesRepository.DayDelta(
            day = d.day,
            appOpen = d.appOpen.coerceAtLeast(0),
            dailyActive = d.dailyActive.coerceAtLeast(0),
            tools = clean(d.tools),
            ads = clean(d.ads),
            versions = clean(d.versions),
            versionsFirstSeen = clean(d.versionsFirstSeen),
            langPrimary = clean(d.langPrimary),
            langSecondary = clean(d.langSecondary),
            widgets = clean(d.widgets)
        )
    }

    // ---------------------------------------------------------------------
    // Estado y diagnóstico
    // ---------------------------------------------------------------------

    private suspend fun recordFailure(
        ds: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
        code: Int,
        error: String?
    ) {
        ds.edit { e ->
            e[MetricsKeys.LAST_UPLOAD_CODE] = code
            e[MetricsKeys.LAST_UPLOAD_ERROR] = error.orEmpty().take(MAX_ERROR_CHARS)
            e[MetricsKeys.CONSECUTIVE_FAILURES] = (e[MetricsKeys.CONSECUTIVE_FAILURES] ?: 0) + 1
        }
    }

    private suspend fun clearPending(
        ds: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
    ) {
        ds.edit { e ->
            e[MetricsKeys.PENDING_BATCH_ID] = ""
            e[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON] = ""
            e[MetricsKeys.PENDING_BATCH_CREATED_AT] = 0L
        }
    }

    private fun safeVersionName(): String = try {
        val pm = applicationContext.packageManager
        val p = pm.getPackageInfo(applicationContext.packageName, 0)
        p.versionName ?: "unknown"
    } catch (_: Throwable) {
        "unknown"
    }

    /** Reconstruye la lista de deltas (por día) a partir del JSON del lote congelado. */
    private fun parseDeltasFromPendingPayload(json: String): List<AggregatesRepository.DayDelta> {
        return try {
            val root = org.json.JSONObject(json)
            val items = root.optJSONArray("items") ?: return emptyList()
            val out = mutableListOf<AggregatesRepository.DayDelta>()

            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val day = item.optString("day", "")
                if (!MetricsContract.isValidDay(day)) continue

                fun objToMap(obj: org.json.JSONObject?): MutableMap<String, Int> {
                    val o = obj ?: org.json.JSONObject()
                    val m = mutableMapOf<String, Int>()
                    val keys = o.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        m[k] = o.optInt(k, 0)
                    }
                    return m
                }

                out += AggregatesRepository.DayDelta(
                    day = day,
                    appOpen = item.optInt("app_open", 0),
                    dailyActive = item.optInt("daily_active", 0),
                    tools = objToMap(item.optJSONObject("tools")),
                    ads = objToMap(item.optJSONObject("ads")),
                    versions = objToMap(item.optJSONObject("versions")),
                    versionsFirstSeen = objToMap(item.optJSONObject("versions_first_seen")),
                    langPrimary = objToMap(item.optJSONObject("lang_primary")),
                    langSecondary = objToMap(item.optJSONObject("lang_secondary")),
                    widgets = objToMap(item.optJSONObject("widgets"))
                )
            }
            out
        } catch (_: Throwable) {
            emptyList()
        }
    }

    companion object {
        /** Intentos antes de descartar un lote y desbloquear la cola. */
        private const val MAX_ATTEMPTS = 5

        private const val MAX_ERROR_CHARS = 200

        /** Antigüedad máxima de un lote congelado antes de reconstruirlo. */
        private val MAX_PENDING_AGE_MS = TimeUnit.DAYS.toMillis(14)

        /**
         * Hook para inyectar un uploader falso en tests.
         * Existe porque el proyecto todavía no tiene inyección de dependencias,
         * y debe eliminarse cuando se introduzca Hilt.
         */
        @VisibleForTesting
        internal var uploaderFactory: (() -> MetricsUploader)? = null

        fun testEnqueueNow(ctx: Context, endpoint: String) {
            val data = workDataOf("endpoint" to endpoint)
            androidx.work.WorkManager.getInstance(ctx).enqueue(
                androidx.work.OneTimeWorkRequestBuilder<UploadMetricsWorker>()
                    .setInputData(data)
                    .build()
            )
        }
    }
}