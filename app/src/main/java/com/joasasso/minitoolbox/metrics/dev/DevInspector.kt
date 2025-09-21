package com.joasasso.minitoolbox.metrics.dev

import android.content.Context
import com.joasasso.minitoolbox.metrics.storage.AggregatesRepository
import com.joasasso.minitoolbox.metrics.storage.JsonUtils
import com.joasasso.minitoolbox.metrics.storage.MetricsKeys
import com.joasasso.minitoolbox.metrics.storage.metricsDataStore
import kotlinx.coroutines.flow.first

data class DevSnapshot(
    val appOpenByDay: Map<String, Int>,
    val toolUseByDay: Map<String, Map<String, Int>>,
    val adImprByDay: Map<String, Map<String, Int>>,
    val sentAppOpenByDay: Map<String, Int>,
    val sentToolUseByDay: Map<String, Map<String, Int>>,
    val sentAdImprByDay: Map<String, Map<String, Int>>,
    val pendingBatchId: String,
    val pendingPayloadJson: String
)

suspend fun loadDevSnapshot(context: Context): DevSnapshot {
    val repo = AggregatesRepository(context.applicationContext)
    val prefs = context.applicationContext.metricsDataStore.data.first()
    return DevSnapshot(
        appOpenByDay = repo.getAppOpenCounts(),
        toolUseByDay = repo.getToolUseByDay(),
        adImprByDay  = repo.getAdImpressionsByDay(),
        sentAppOpenByDay = JsonUtils.fromDayIntMap(prefs[MetricsKeys.SENT_APP_OPEN_BY_DAY]).toMap(),
        sentToolUseByDay  = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_TOOL_USE_BY_DAY_JSON]).mapValues { it.value.toMap() },
        sentAdImprByDay   = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_AD_IMPR_BY_DAY_JSON]).mapValues { it.value.toMap() },
        pendingBatchId    = prefs[MetricsKeys.PENDING_BATCH_ID].orEmpty(),
        pendingPayloadJson= prefs[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON].orEmpty()
    )
}

fun prettyJson(json: String): String = try {
    val obj = org.json.JSONObject(json)
    obj.toString(2)
} catch (_: Throwable) {
    try { org.json.JSONArray(json).toString(2) } catch (_: Throwable) { json }
}

