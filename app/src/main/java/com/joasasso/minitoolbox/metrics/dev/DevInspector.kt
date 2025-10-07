package com.joasasso.minitoolbox.metrics.dev

import android.content.Context
import com.joasasso.minitoolbox.metrics.storage.AggregatesRepository
import com.joasasso.minitoolbox.metrics.storage.JsonUtils
import com.joasasso.minitoolbox.metrics.storage.MetricsKeys
import com.joasasso.minitoolbox.metrics.storage.metricsDataStore
import com.joasasso.minitoolbox.metrics.uploader.UploadConfig
import com.joasasso.minitoolbox.metrics.uploader.UploadScheduler
import kotlinx.coroutines.flow.first

/** Vista neutral para mostrar deltas sin depender de la data class interna del repositorio. */
data class DayDeltaView(
    val day: String,
    val appOpen: Int,
    val tools: Map<String, Int>,
    val ads: Map<String, Int>
)

data class DevSnapshot(
    // Agregados actuales (lo que venís contando localmente)
    val appOpenByDay: Map<String, Int>,
    val toolUseByDay: Map<String, Map<String, Int>>,
    val adImprByDay: Map<String, Map<String, Int>>,

    // Snapshots de “enviado hasta” (SENT_*)
    val sentAppOpenByDay: Map<String, Int>,
    val sentToolUseByDay: Map<String, Map<String, Int>>,
    val sentAdImprByDay: Map<String, Map<String, Int>>,

    // Payload pendiente (idempotencia local)
    val pendingBatchId: String,
    val pendingPayloadJson: String,

    // Deltas
    /** Deltas que quedarían por enviar HOY (construidos desde current - sent). */
    val remainingDeltas: List<DayDeltaView>,
    /** Suma de deltas pendientes: (appOpens, tools, ads). */
    val remainingTotals: Triple<Int, Int, Int>,

    /** Deltas reconstruidos desde el payload pendiente (si existe), para validar consistencia visual. */
    val pendingDeltasFromPayload: List<DayDeltaView>,

    // Estado del planificador
    val isDirty: Boolean,
    val lastEnqueuedMs: Long,

    // Config de envío
    val endpoint: String,
    val apiKeyPreview: String // primeras 6 letras + longitud, para no exponerla entera
)

/** Carga un snapshot completo del estado de métricas y del uploader. */
suspend fun loadDevSnapshot(context: Context): DevSnapshot {
    val appCtx = context.applicationContext
    val repo = AggregatesRepository(appCtx)
    val prefs = appCtx.metricsDataStore.data.first()

    // Agregados actuales
    val appOpenByDay = repo.getAppOpenCounts()
    val toolUseByDay = repo.getToolUseCounts()
    val adImprByDay  = repo.getAdImpressionCounts()

    // Snapshots enviados
    val sentAppOpenByDay = JsonUtils.fromDayIntMap(prefs[MetricsKeys.SENT_APP_OPEN_BY_DAY]).toMap()
    val sentToolUseByDay = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_TOOL_USE_BY_DAY_JSON]).mapValues { it.value.toMap() }
    val sentAdImprByDay  = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_AD_IMPR_BY_DAY_JSON]).mapValues { it.value.toMap() }

    // Pending payload (idempotencia)
    val pendingBatchId     = prefs[MetricsKeys.PENDING_BATCH_ID].orEmpty()
    val pendingPayloadJson = prefs[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON].orEmpty()

    // Deltas “reales” (current - sent): lo que se enviaría si hoy corriera el worker
    val remaining = repo.buildDeltasSinceLastSent().map {
        DayDeltaView(it.day, it.appOpen, it.tools.toMap(), it.ads.toMap())
    }
    val remainingTotals = Triple(
        remaining.sumOf { it.appOpen },
        remaining.sumOf { it.tools.values.sum() },
        remaining.sumOf { it.ads.values.sum() }
    )

    // Si hay payload pendiente, reconstruimos sus deltas para ver qué contiene realmente ese batch.
    val pendingDeltas = parseDeltasFromPendingPayload(pendingPayloadJson)

    // Estado del scheduler y config
    val isDirty = UploadScheduler.isDirty(appCtx)
    val lastEnq = UploadScheduler.lastEnqueuedMs(appCtx)
    val endpoint = UploadConfig.getEndpoint(appCtx)
    val apiKeyPreview = previewKey(UploadConfig.getApiKey(appCtx))

    return DevSnapshot(
        appOpenByDay = appOpenByDay,
        toolUseByDay = toolUseByDay,
        adImprByDay = adImprByDay,
        sentAppOpenByDay = sentAppOpenByDay,
        sentToolUseByDay = sentToolUseByDay,
        sentAdImprByDay = sentAdImprByDay,
        pendingBatchId = pendingBatchId,
        pendingPayloadJson = pendingPayloadJson,
        remainingDeltas = remaining,
        remainingTotals = remainingTotals,
        pendingDeltasFromPayload = pendingDeltas,
        isDirty = isDirty,
        lastEnqueuedMs = lastEnq,
        endpoint = endpoint,
        apiKeyPreview = apiKeyPreview
    )
}

/** Pretty printer para JSON (objeto o array). */
fun prettyJson(json: String): String = try {
    val obj = org.json.JSONObject(json)
    obj.toString(2)
} catch (_: Throwable) {
    try { org.json.JSONArray(json).toString(2) } catch (_: Throwable) { json }
}

/** Reconstruye deltas desde el JSON del payload pendiente (si existe). */
private fun parseDeltasFromPendingPayload(json: String): List<DayDeltaView> {
    if (json.isBlank()) return emptyList()
    return try {
        val root = org.json.JSONObject(json)
        val items = root.optJSONArray("items") ?: return emptyList()
        val out = mutableListOf<DayDeltaView>()
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val day = item.optString("day", "")
            if (!isValidDay(day)) continue

            val app = item.optInt("app_open", 0)

            val toolsObj = item.optJSONObject("tools") ?: org.json.JSONObject()
            val toolsMap = mutableMapOf<String, Int>()
            val itTools = toolsObj.keys()
            while (itTools.hasNext()) {
                val k = itTools.next()
                toolsMap[k] = toolsObj.optInt(k, 0)
            }

            val adsObj = item.optJSONObject("ads") ?: org.json.JSONObject()
            val adsMap = mutableMapOf<String, Int>()
            val itAds = adsObj.keys()
            while (itAds.hasNext()) {
                val k = itAds.next()
                adsMap[k] = adsObj.optInt(k, 0)
            }

            out += DayDeltaView(day, app, toolsMap, adsMap)
        }
        out
    } catch (_: Throwable) {
        emptyList()
    }
}

private fun isValidDay(s: String): Boolean =
    Regex("""^\d{4}-\d{2}-\d{2}$""").matches(s)

/** Muestra solo un prefijo de la API key para debug sin exponerla entera. */
private fun previewKey(k: String): String =
    if (k.isBlank()) "" else k.take(6) + "…(${k.length})"
