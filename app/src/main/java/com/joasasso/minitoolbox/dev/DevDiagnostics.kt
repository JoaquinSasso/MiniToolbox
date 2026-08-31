package com.joasasso.minitoolbox.dev

import android.content.Context
import com.joasasso.minitoolbox.metrics.isMetricsEnabled
import com.joasasso.minitoolbox.metrics.storage.MetricsKeys
import com.joasasso.minitoolbox.metrics.storage.MetricsSanitizer
import com.joasasso.minitoolbox.metrics.storage.metricsDataStore
import com.joasasso.minitoolbox.metrics.uploader.UploadScheduler
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Estado de salud del pipeline de métricas, apto para mostrarse en release.
 *
 * Contiene sólo contadores y códigos: ningún dato personal, ninguna credencial. El
 * endpoint se muestra recortado y la clave de API ya no existe en el cliente.
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
    val pendingPayloadLength: Int,
    val pendingCreatedAtMs: Long,
    val isDirty: Boolean,
    val appVersion: String
) {
    /** True si el pipeline parece sano: envió alguna vez y no viene fallando. */
    val looksHealthy: Boolean
        get() = lastSuccessAtMs > 0L && consecutiveFailures == 0

    /** Días desde el último envío exitoso, o null si nunca hubo uno. */
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
    } catch (_: Throwable) {
        "unknown"
    }

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
        pendingPayloadLength = prefs[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON].orEmpty().length,
        pendingCreatedAtMs = prefs[MetricsKeys.PENDING_BATCH_CREATED_AT] ?: 0L,
        isDirty = UploadScheduler.isDirty(appCtx),
        appVersion = appVersion
    )
}

/**
 * Texto plano del diagnóstico, pensado para que el usuario lo copie y lo mande.
 *
 * Es la razón principal para exponer esta pantalla en release: convierte un
 * "no me andan las métricas" en un reporte accionable sin pedirle a nadie que instale
 * una build especial.
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
    appendLine("pending_batch: ${health.pendingBatchId.ifBlank { "-" }}")
    appendLine("pending_size: ${health.pendingPayloadLength}")
    appendLine("pending_since: ${formatTimestamp(health.pendingCreatedAtMs)}")
    appendLine("dirty: ${health.isDirty}")
}

/** Resumen de una línea para encabezar la pantalla. */
fun summarizeHealth(health: MetricsHealth): String = when {
    !health.metricsEnabled -> "Métricas desactivadas en Configuración"
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