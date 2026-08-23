package com.joasasso.minitoolbox.dev

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.joasasso.minitoolbox.metrics.adImpression
import com.joasasso.minitoolbox.metrics.appOpen
import com.joasasso.minitoolbox.metrics.isMetricsEnabled
import com.joasasso.minitoolbox.metrics.storage.AggregatesRepository
import com.joasasso.minitoolbox.metrics.storage.JsonUtils
import com.joasasso.minitoolbox.metrics.storage.MetricsKeys
import com.joasasso.minitoolbox.metrics.storage.metricsDataStore
import com.joasasso.minitoolbox.metrics.toolUse
import com.joasasso.minitoolbox.metrics.MetricsConfig
import com.joasasso.minitoolbox.metrics.uploader.UploadMetricsWorker
import com.joasasso.minitoolbox.metrics.uploader.UploadScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Vista neutral para mostrar deltas sin depender de la data class interna del repositorio. */
data class DayDeltaView(
    val day: String,
    val appOpen: Int,
    val tools: Map<String, Int>,
    val ads: Map<String, Int>,
    val versions: Map<String, Int> = emptyMap(),
    val versionsFirstSeen: Map<String, Int> = emptyMap(),
    val langPrimary: Map<String, Int> = emptyMap(),
    val langSecondary: Map<String, Int> = emptyMap(),
    val widgets: Map<String, Int> = emptyMap()
)

data class DevSnapshot(
    val isEnabled: Boolean,
    val endpoint: String,
    val apiKeyPreview: String,
    val isDirty: Boolean,
    val lastEnqueuedMs: Long,
    val pendingBatchId: String,
    val pendingPayloadJson: String,
    val remainingDeltas: List<DayDeltaView>,
    val remainingTotals: Triple<Int, Int, Int> // (appOpens, tools, ads)
)

/** Carga un snapshot completo del estado actual de métricas y uploader. */
suspend fun loadDevSnapshot(context: Context): DevSnapshot {
    val appCtx = context.applicationContext
    val repo = AggregatesRepository(appCtx)
    val prefs = appCtx.metricsDataStore.data.first()

    val pendingBatchId = prefs[MetricsKeys.PENDING_BATCH_ID].orEmpty()
    val pendingPayloadJson = prefs[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON].orEmpty()

    val remaining = repo.buildDeltasSinceLastSent().map {
        DayDeltaView(
            day = it.day,
            appOpen = it.appOpen,
            tools = it.tools.toMap(),
            ads = it.ads.toMap(),
            versions = it.versions.toMap(),
            versionsFirstSeen = it.versionsFirstSeen.toMap(),
            langPrimary = it.langPrimary.toMap(),
            langSecondary = it.langSecondary.toMap(),
            widgets = it.widgets.toMap()
        )
    }

    val remainingTotals = Triple(
        remaining.sumOf { it.appOpen },
        remaining.sumOf { it.tools.values.sum() },
        remaining.sumOf { it.ads.values.sum() }
    )

    val endpoint = MetricsConfig.endpoint
    val rawKey = MetricsConfig.apiKey

    return DevSnapshot(
        isEnabled = isMetricsEnabled(appCtx),
        endpoint = endpoint,
        apiKeyPreview = previewKey(rawKey),
        isDirty = UploadScheduler.isDirty(appCtx),
        lastEnqueuedMs = UploadScheduler.lastEnqueuedMs(appCtx),
        pendingBatchId = pendingBatchId,
        pendingPayloadJson = pendingPayloadJson,
        remainingDeltas = remaining,
        remainingTotals = remainingTotals
    )
}

/** Encola un envío forzado inmediato a través de WorkManager. */
fun triggerFlushNow(context: Context) {
    val appCtx = context.applicationContext
    val endpoint = MetricsConfig.endpoint
    val apiKey = MetricsConfig.apiKey
    UploadMetricsWorker.testEnqueueNow(appCtx, endpoint, apiKey)
}

/** Envía un payload de prueba síncrono para verificar conexión y clave en el backend. */
suspend fun testDirectConnection(context: Context): Result<String> = withContext(Dispatchers.IO) {
    val endpoint = MetricsConfig.endpoint
    val apiKey = MetricsConfig.apiKey

    if (endpoint.isBlank()) {
        return@withContext Result.failure(Exception("Endpoint no configurado"))
    }

    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val testPayload = org.json.JSONObject().apply {
        put("batch_id", "test_ping_" + UUID.randomUUID().toString().take(8))
        put("platform", "android_dev_test")
        put("app_version", "dev_probe")
        put("items", org.json.JSONArray().apply {
            put(org.json.JSONObject().apply {
                put("day", today)
                put("app_open", 1)
                put("tools", org.json.JSONObject().apply {
                    put("metricTest", 1)
                })
            })
        })
    }

    try {
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Content-Type", "application/json")
            if (apiKey.isNotBlank()) setRequestProperty("X-API-Key", apiKey)
        }

        conn.outputStream.use { it.write(testPayload.toString().toByteArray()) }
        val code = conn.responseCode
        val responseBody = try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        }

        if (code in 200..299) {
            Result.success("HTTP $code OK - $responseBody")
        } else {
            Result.failure(Exception("HTTP $code Error: $responseBody"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/** Simula eventos locales y opcionalmente los envía. */
fun simulateMetricEvents(context: Context, type: String) {
    when (type) {
        "open" -> appOpen(context)
        "test_tool" -> toolUse(context, "metricTest")
        "metricsTest" -> toolUse(context, "metricsTest")
        "ad" -> adImpression(context, "banner")
    }
}

suspend fun clearPendingBatch(context: Context) {
    val appCtx = context.applicationContext
    appCtx.metricsDataStore.edit { e ->
        e[MetricsKeys.PENDING_BATCH_ID] = ""
        e[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON] = ""
    }
}

fun prettyJson(json: String): String = try {
    org.json.JSONObject(json).toString(2)
} catch (_: Throwable) {
    try { org.json.JSONArray(json).toString(2) } catch (_: Throwable) { json }
}

private fun previewKey(k: String): String =
    if (k.isBlank()) "NO CONFIGURADA" else k.take(6) + "…(${k.length} chars)"

/**
 * 1. Elimina cualquier clave de tool que contenga barras o UUIDs inválidos
 * de los contadores acumulados y descarta el lote congelado.
 */
suspend fun sanitizeAndClearInvalidToolKeys(context: Context) {
    val appCtx = context.applicationContext
    appCtx.metricsDataStore.edit { prefs ->
        // Limpiar lote congelado
        prefs[MetricsKeys.PENDING_BATCH_ID] = ""
        prefs[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON] = ""

        // Sanitizar TOOL_USE_BY_DAY_JSON
        val toolMap = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.TOOL_USE_BY_DAY_JSON])
        val keyRegex = Regex("^[a-zA-Z0-9._-]{1,64}$")

        for ((day, tools) in toolMap) {
            val validTools = tools.filterKeys { key -> keyRegex.matches(key) }.toMutableMap()
            toolMap[day] = validTools
        }
        prefs[MetricsKeys.TOOL_USE_BY_DAY_JSON] = JsonUtils.toDayNestedIntMap(toolMap)

        // Sanitizar SENT_TOOL_USE_BY_DAY_JSON por consistencia
        val sentToolMap = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_TOOL_USE_BY_DAY_JSON])
        for ((day, tools) in sentToolMap) {
            val validTools = tools.filterKeys { key -> keyRegex.matches(key) }.toMutableMap()
            sentToolMap[day] = validTools
        }
        prefs[MetricsKeys.SENT_TOOL_USE_BY_DAY_JSON] = JsonUtils.toDayNestedIntMap(sentToolMap)
    }
}

/**
 * 2. Reset total de métricas locales (deja el DataStore en cero para desarrollo).
 */
suspend fun resetAllMetricsDataStore(context: Context) {
    val appCtx = context.applicationContext
    appCtx.metricsDataStore.edit { it.clear() }
}