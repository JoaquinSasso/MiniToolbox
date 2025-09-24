package com.joasasso.minitoolbox.metrics.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AggregatesRepository(private val context: Context) {

    data class DayDelta(
        val day: String,
        val appOpen: Int,
        val tools: MutableMap<String, Int>,
        val ads: MutableMap<String, Int>
    )

    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private fun today(): String = dayFmt.format(Date())

    // ---------------------------
    // Escrituras de contadores
    // ---------------------------

    suspend fun incrementAppOpen() {
        val ds = context.metricsDataStore
        val day = today()
        ds.edit { e ->
            val byDay = JsonUtils.fromDayIntMap(e[MetricsKeys.APP_OPEN_COUNT_BY_DAY])
            byDay[day] = (byDay[day] ?: 0) + 1
            e[MetricsKeys.APP_OPEN_COUNT_BY_DAY] = JsonUtils.toDayIntMap(byDay)
        }
    }

    suspend fun incrementToolUse(toolId: String) {
        val ds = context.metricsDataStore
        val day = today()
        ds.edit { e ->
            val byDay = JsonUtils.fromDayNestedIntMap(e[MetricsKeys.TOOL_USE_BY_DAY_JSON])
            val bucket = byDay.getOrPut(day) { mutableMapOf() }
            bucket[toolId] = (bucket[toolId] ?: 0) + 1
            e[MetricsKeys.TOOL_USE_BY_DAY_JSON] = JsonUtils.toDayNestedIntMap(byDay)
        }
    }

    suspend fun incrementAdImpression(type: String) {
        val ds = context.metricsDataStore
        val day = today()
        ds.edit { e ->
            val byDay = JsonUtils.fromDayNestedIntMap(e[MetricsKeys.AD_IMPRESSIONS_BY_DAY_JSON])
            val bucket = byDay.getOrPut(day) { mutableMapOf() }
            bucket[type] = (bucket[type] ?: 0) + 1
            e[MetricsKeys.AD_IMPRESSIONS_BY_DAY_JSON] = JsonUtils.toDayNestedIntMap(byDay)
        }
    }

    // ----------------------------------
    // Lecturas y cálculo de DELTAS
    // ----------------------------------

    suspend fun getAppOpenCounts(): Map<String, Int> {
        val prefs = try { context.metricsDataStore.data.first() } catch (_: Throwable) { emptyPreferences() }
        return JsonUtils.fromDayIntMap(prefs[MetricsKeys.APP_OPEN_COUNT_BY_DAY])
    }

    suspend fun getToolUseCounts(): Map<String, Map<String, Int>> {
        val prefs = try { context.metricsDataStore.data.first() } catch (_: Throwable) { emptyPreferences() }
        return JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.TOOL_USE_BY_DAY_JSON])
    }

    suspend fun getAdImpressionCounts(): Map<String, Map<String, Int>> {
        val prefs = try { context.metricsDataStore.data.first() } catch (_: Throwable) { emptyPreferences() }
        return JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.AD_IMPRESSIONS_BY_DAY_JSON])
    }

    suspend fun buildDeltasSinceLastSent(): List<DayDelta> {
        val prefs = try { context.metricsDataStore.data.first() } catch (_: Throwable) { emptyPreferences() }

        val currentApp  = JsonUtils.fromDayIntMap(prefs[MetricsKeys.APP_OPEN_COUNT_BY_DAY])
        val currentTool = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.TOOL_USE_BY_DAY_JSON])
        val currentAds  = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.AD_IMPRESSIONS_BY_DAY_JSON])

        val sentApp  = JsonUtils.fromDayIntMap(prefs[MetricsKeys.SENT_APP_OPEN_BY_DAY])
        val sentTool = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_TOOL_USE_BY_DAY_JSON])
        val sentAds  = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_AD_IMPR_BY_DAY_JSON])

        val allDays = (currentApp.keys + currentTool.keys + currentAds.keys + sentApp.keys + sentTool.keys + sentAds.keys).toSortedSet()

        val out = mutableListOf<DayDelta>()
        for (day in allDays) {
            val appDelta = (currentApp[day] ?: 0) - (sentApp[day] ?: 0)
            val toolsCur = currentTool[day] ?: emptyMap()
            val toolsSent = sentTool[day] ?: emptyMap()
            val adsCur = currentAds[day] ?: emptyMap()
            val adsSent = sentAds[day] ?: emptyMap()

            val toolKeys = (toolsCur.keys + toolsSent.keys).toSet()
            val adsKeys  = (adsCur.keys + adsSent.keys).toSet()

            val tMap = mutableMapOf<String, Int>()
            for (k in toolKeys) {
                val d = (toolsCur[k] ?: 0) - (toolsSent[k] ?: 0)
                if (d > 0) tMap[k] = d
            }

            val aMap = mutableMapOf<String, Int>()
            for (k in adsKeys) {
                val d = (adsCur[k] ?: 0) - (adsSent[k] ?: 0)
                if (d > 0) aMap[k] = d
            }

            if (appDelta > 0 || tMap.isNotEmpty() || aMap.isNotEmpty()) {
                out += DayDelta(day, appDelta.coerceAtLeast(0), tMap, aMap)
            }
        }
        return out
    }

    // --------------------------------------------------
    // Commit de envío (idempotencia local)
    // --------------------------------------------------

    /** Marca como "enviados" SOLO los deltas efectivamente enviados en el último batch. */
    suspend fun commitSent(deltas: List<DayDelta>) {
        val ds = context.metricsDataStore
        val prefs = try { ds.data.first() } catch (_: Throwable) { emptyPreferences() }

        val sentApp   = JsonUtils.fromDayIntMap(prefs[MetricsKeys.SENT_APP_OPEN_BY_DAY])
        val sentTools = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_TOOL_USE_BY_DAY_JSON])
        val sentAds   = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_AD_IMPR_BY_DAY_JSON])

        for (d in deltas) {
            // app
            sentApp[d.day] = (sentApp[d.day] ?: 0) + d.appOpen
            // tools
            val t = sentTools.getOrPut(d.day) { mutableMapOf() }
            for ((k, v) in d.tools) t[k] = (t[k] ?: 0) + v
            // ads
            val a = sentAds.getOrPut(d.day) { mutableMapOf() }
            for ((k, v) in d.ads) a[k] = (a[k] ?: 0) + v
        }

        ds.edit { e ->
            e[MetricsKeys.SENT_APP_OPEN_BY_DAY]      = JsonUtils.toDayIntMap(sentApp)
            e[MetricsKeys.SENT_TOOL_USE_BY_DAY_JSON] = JsonUtils.toDayNestedIntMap(sentTools)
            e[MetricsKeys.SENT_AD_IMPR_BY_DAY_JSON]  = JsonUtils.toDayNestedIntMap(sentAds)
            e[MetricsKeys.PENDING_BATCH_ID]          = ""
            e[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON]= ""
        }
    }
}
