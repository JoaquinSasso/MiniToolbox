package com.joasasso.minitoolbox.dev

import android.content.Context
import android.content.pm.PackageManager
import com.joasasso.minitoolbox.metrics.MetricsContract
import com.joasasso.minitoolbox.metrics.isMetricsEnabled
import com.joasasso.minitoolbox.metrics.storage.MetricsKeys
import com.joasasso.minitoolbox.metrics.storage.MetricsSanitizer
import com.joasasso.minitoolbox.metrics.storage.metricsDataStore
import com.joasasso.minitoolbox.metrics.uploader.UploadScheduler
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Resumen estructural del lote congelado.
 *
 * Responde "¿por qué el backend rechazaría este lote?" sin volcar el payload entero: si hay
 * claves o días que no cumplen [MetricsContract], aparecen nombrados y eso suele ser toda
 * la explicación.
 */
data class PayloadAnalysis(
    val present: Boolean,
    val sizeChars: Int,
    val itemCount: Int,
    val distinctKeys: Int,
    val invalidKeys: List<String>,
    val invalidDays: List<String>,
    val parseError: String?
) {
    val looksValid: Boolean
        get() = !present || (parseError == null && invalidKeys.isEmpty() && invalidDays.isEmpty())
}

/** Campos del item que contienen mapas clave -> contador. */
private val COUNTER_FIELDS = listOf(
    "tools", "ads", "versions", "versions_first_seen",
    "lang_primary", "lang_secondary", "widgets"
)

/**
 * Analiza el payload congelado buscando lo que el backend rechazaría.
 * Función pura: se puede testear sin Android.
 */
fun analyzePendingPayload(json: String): PayloadAnalysis {
    if (json.isBlank()) {
        return PayloadAnalysis(
            present = false,
            sizeChars = 0,
            itemCount = 0,
            distinctKeys = 0,
            invalidKeys = emptyList(),
            invalidDays = emptyList(),
            parseError = null
        )
    }

    return try {
        val root = JSONObject(json)
        val items: JSONArray = root.optJSONArray("items") ?: JSONArray()

        val allKeys = mutableSetOf<String>()
        val invalidKeys = mutableSetOf<String>()
        val invalidDays = mutableSetOf<String>()

        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue

            val day = item.optString("day", "")
            if (!MetricsContract.isValidDay(day)) invalidDays.add(day.ifBlank { "(vacío)" })

            for (field in COUNTER_FIELDS) {
                val map = item.optJSONObject(field) ?: continue
                val keys = map.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    allKeys.add(key)
                    if (!MetricsContract.isValidKey(key)) invalidKeys.add(key)
                }
            }
        }

        PayloadAnalysis(
            present = true,
            sizeChars = json.length,
            itemCount = items.length(),
            distinctKeys = allKeys.size,
            invalidKeys = invalidKeys.sorted(),
            invalidDays = invalidDays.sorted(),
            parseError = null
        )
    } catch (e: JSONException) {
        PayloadAnalysis(
            present = true,
            sizeChars = json.length,
            itemCount = 0,
            distinctKeys = 0,
            invalidKeys = emptyList(),
            invalidDays = emptyList(),
            parseError = "${e.javaClass.simpleName}: ${e.message}"
        )
    } catch (e: Exception) {
        PayloadAnalysis(
            present = true,
            sizeChars = json.length,
            itemCount = 0,
            distinctKeys = 0,
            invalidKeys = emptyList(),
            invalidDays = emptyList(),
            parseError = "${e.javaClass.simpleName}: ${e.message}"
        )
    }
}

/**
 * Estado de salud del pipeline de métricas, apto para mostrarse en release.
 *
 * El payload crudo queda en [pendingPayloadRaw] pero fuera del informe por defecto: son
 * contadores de uso de la persona y conviene que decida explícitamente compartirlos.
 */
data class MetricsHealth(
    val metricsEnabled: Boolean,
    val schemaVersion: Int,
    val lastUploadCode: Int,
    val lastUploadError: String,
    val lastSuccessAtMs: Long,
    val consecutiveFailures: Int,
    val droppedBatches: Int,
    val sanitizedKeysTotal: Int,
    val lastUploadUsedAppCheck: Boolean,
    val pendingBatchId: String,
    val pendingCreatedAtMs: Long,
    val pendingPayloadRaw: String,
    val payload: PayloadAnalysis,
    val isDirty: Boolean,
    val appVersion: String
) {
    val looksHealthy: Boolean
        get() = lastSuccessAtMs > 0L && consecutiveFailures == 0

    val daysSinceLastSuccess: Long?
        get() = if (lastSuccessAtMs <= 0L) null
        else TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastSuccessAtMs)
}

private val TIMESTAMP_FMT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

private fun formatTimestamp(ms: Long): String =
    if (ms <= 0L) "nunca" else TIMESTAMP_FMT.format(Date(ms))

/** Lee el estado de salud del DataStore de métricas. */
suspend fun loadMetricsHealth(context: Context): MetricsHealth {
    val appCtx = context.applicationContext
    val prefs = appCtx.metricsDataStore.data.first()

    val appVersion = try {
        appCtx.packageManager.getPackageInfo(appCtx.packageName, 0).versionName ?: "unknown"
    } catch (_: PackageManager.NameNotFoundException) {
        "unknown"
    } catch (_: Exception) {
        "unknown"
    }

    val rawPayload = prefs[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON].orEmpty()

    return MetricsHealth(
        metricsEnabled = isMetricsEnabled(appCtx),
        schemaVersion = prefs[MetricsKeys.SCHEMA_VERSION] ?: 0,
        lastUploadCode = prefs[MetricsKeys.LAST_UPLOAD_CODE] ?: 0,
        lastUploadError = prefs[MetricsKeys.LAST_UPLOAD_ERROR].orEmpty(),
        lastSuccessAtMs = prefs[MetricsKeys.LAST_SUCCESS_AT] ?: 0L,
        consecutiveFailures = prefs[MetricsKeys.CONSECUTIVE_FAILURES] ?: 0,
        droppedBatches = prefs[MetricsKeys.DROPPED_BATCHES] ?: 0,
        sanitizedKeysTotal = prefs[MetricsKeys.SANITIZED_KEYS_TOTAL] ?: 0,
        lastUploadUsedAppCheck = prefs[MetricsKeys.LAST_UPLOAD_USED_APPCHECK] ?: false,
        pendingBatchId = prefs[MetricsKeys.PENDING_BATCH_ID].orEmpty(),
        pendingCreatedAtMs = prefs[MetricsKeys.PENDING_BATCH_CREATED_AT] ?: 0L,
        pendingPayloadRaw = rawPayload,
        payload = analyzePendingPayload(rawPayload),
        isDirty = UploadScheduler.isDirty(appCtx),
        appVersion = appVersion
    )
}

/**
 * Informe de texto plano para que el usuario lo copie y lo mande.
 *
 * Incluye el análisis del lote pendiente con las claves problemáticas nombradas, pero no el
 * payload completo: con esto suele alcanzar para identificar por qué el backend rechaza.
 */
fun buildDiagnosticsText(health: MetricsHealth): String = buildString {
    appendLine("--- MiniToolbox: diagnóstico de métricas ---")
    appendLine("app_version: ${health.appVersion}")
    appendLine("metrics_enabled: ${health.metricsEnabled}")
    appendLine("schema_version: ${health.schemaVersion}")
    appendLine("sanitizer_version: ${MetricsSanitizer.SCHEMA_VERSION}")
    appendLine()
    appendLine("last_success: ${formatTimestamp(health.lastSuccessAtMs)}")
    health.daysSinceLastSuccess?.let { appendLine("days_since_success: $it") }
    appendLine("last_code: ${health.lastUploadCode}")
    appendLine("last_error: ${health.lastUploadError.ifBlank { "-" }}")
    appendLine("used_appcheck: ${health.lastUploadUsedAppCheck}")
    appendLine("consecutive_failures: ${health.consecutiveFailures}")
    appendLine()
    appendLine("dropped_batches: ${health.droppedBatches}")
    appendLine("sanitized_keys: ${health.sanitizedKeysTotal}")
    appendLine("dirty: ${health.isDirty}")
    appendLine()

    val p = health.payload
    if (!p.present) {
        appendLine("pending_batch: ninguno")
        return@buildString
    }

    appendLine("pending_batch: ${health.pendingBatchId.ifBlank { "(sin id)" }}")
    appendLine("pending_since: ${formatTimestamp(health.pendingCreatedAtMs)}")
    appendLine("pending_size: ${p.sizeChars} chars")
    appendLine("pending_items: ${p.itemCount}")
    appendLine("pending_distinct_keys: ${p.distinctKeys}")

    p.parseError?.let { appendLine("pending_parse_error: $it") }

    if (p.looksValid) {
        appendLine("pending_contract: OK")
        return@buildString
    }

    if (p.invalidKeys.isNotEmpty()) {
        appendLine("pending_invalid_keys (${p.invalidKeys.size}):")
        p.invalidKeys.take(MAX_LISTED_KEYS).forEach { appendLine("  - $it") }
        if (p.invalidKeys.size > MAX_LISTED_KEYS) {
            appendLine("  … y ${p.invalidKeys.size - MAX_LISTED_KEYS} más")
        }
    }
    if (p.invalidDays.isNotEmpty()) {
        appendLine("pending_invalid_days: ${p.invalidDays.joinToString(", ")}")
    }
}

private const val MAX_LISTED_KEYS = 20

/** Resumen de una línea para encabezar la pantalla. */
fun summarizeHealth(health: MetricsHealth): String = when {
    !health.metricsEnabled -> "Métricas desactivadas en Configuración"
    !health.payload.looksValid -> "Lote pendiente con datos que el servidor rechazaría"
    health.lastSuccessAtMs <= 0L && health.consecutiveFailures == 0 ->
        "Todavía no se envió ningún lote"
    health.looksHealthy -> {
        val days = health.daysSinceLastSuccess ?: 0L
        if (days == 0L) "Funcionando: último envío hoy"
        else "Funcionando: último envío hace $days día(s)"
    }
    health.consecutiveFailures > 0 ->
        "Fallando: ${health.consecutiveFailures} intento(s) seguidos, último código ${health.lastUploadCode}"
    else -> "Estado indeterminado"
}

/** Formatea el payload crudo con indentación, para inspección manual. */
fun prettyPayload(json: String): String = try {
    JSONObject(json).toString(2)
} catch (_: JSONException) {
    json
}