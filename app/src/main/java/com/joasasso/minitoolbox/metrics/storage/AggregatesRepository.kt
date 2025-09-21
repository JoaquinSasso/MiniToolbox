package com.joasasso.minitoolbox.metrics.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AggregatesRepository(private val context: Context) {

    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private fun today(): String = dayFmt.format(Date())

    // ---- Escrituras agregadas ----

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

    // ---- Lecturas (para el worker) ----

    suspend fun getAppOpenCounts(): Map<String, Int> {
        val prefs = try { context.metricsDataStore.data.first() } catch (_: Throwable) { emptyPreferences() }
        return JsonUtils.fromDayIntMap(prefs[MetricsKeys.APP_OPEN_COUNT_BY_DAY]).toMap()
    }

    suspend fun getToolUseByDay(): Map<String, Map<String, Int>> {
        val prefs = try { context.metricsDataStore.data.first() } catch (_: Throwable) { emptyPreferences() }
        val raw = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.TOOL_USE_BY_DAY_JSON])
        return raw.mapValues { it.value.toMap() }
    }

    suspend fun getAdImpressionsByDay(): Map<String, Map<String, Int>> {
        val prefs = try { context.metricsDataStore.data.first() } catch (_: Throwable) { emptyPreferences() }
        val raw = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.AD_IMPRESSIONS_BY_DAY_JSON])
        return raw.mapValues { it.value.toMap() }
    }

    // ---- DELTAS e idempotencia ----

    data class DayDelta(
        val day: String,
        val appOpen: Int,
        val tools: Map<String, Int>,
        val ads: Map<String, Int>
    )

    suspend fun buildDeltasSinceLastSent(): List<DayDelta> {
        val prefs = context.metricsDataStore.data.first()

        // actuales
        val appOpen = JsonUtils.fromDayIntMap(prefs[MetricsKeys.APP_OPEN_COUNT_BY_DAY])
        val tools   = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.TOOL_USE_BY_DAY_JSON])
        val ads     = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.AD_IMPRESSIONS_BY_DAY_JSON])

        // últimos enviados
        val sentApp   = JsonUtils.fromDayIntMap(prefs[MetricsKeys.SENT_APP_OPEN_BY_DAY])
        val sentTools = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_TOOL_USE_BY_DAY_JSON])
        val sentAds   = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.SENT_AD_IMPR_BY_DAY_JSON])

        val allDays = (appOpen.keys + tools.keys + ads.keys).toSortedSet()
        val out = mutableListOf<DayDelta>()

        for (day in allDays) {
            val aNow = appOpen[day] ?: 0
            val aSent = sentApp[day] ?: 0
            val appDelta = (aNow - aSent).coerceAtLeast(0)

            val tNow = tools[day] ?: mutableMapOf()
            val tSent = sentTools[day] ?: mutableMapOf()
            val tKeys = (tNow.keys + tSent.keys).toSet()
            val tDelta = buildMap<String, Int> {
                for (k in tKeys) {
                    val d = (tNow[k] ?: 0) - (tSent[k] ?: 0)
                    if (d > 0) put(k, d)
                }
            }

            val adNow = ads[day] ?: mutableMapOf()
            val adSent = sentAds[day] ?: mutableMapOf()
            val adKeys = (adNow.keys + adSent.keys).toSet()
            val adDelta = buildMap<String, Int> {
                for (k in adKeys) {
                    val d = (adNow[k] ?: 0) - (adSent[k] ?: 0)
                    if (d > 0) put(k, d)
                }
            }

            if (appDelta > 0 || tDelta.isNotEmpty() || adDelta.isNotEmpty()) {
                out += DayDelta(day, appDelta, tDelta, adDelta)
            }
        }
        return out
    }

    /** Llamar SOLO si el servidor confirmó éxito del batch. */
    suspend fun commitSentUpToCurrent() {
        val ds = context.metricsDataStore
        val prefs = ds.data.first()

        val appOpen = JsonUtils.fromDayIntMap(prefs[MetricsKeys.APP_OPEN_COUNT_BY_DAY])
        val tools   = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.TOOL_USE_BY_DAY_JSON])
        val ads     = JsonUtils.fromDayNestedIntMap(prefs[MetricsKeys.AD_IMPRESSIONS_BY_DAY_JSON])

        ds.edit { e ->
            e[MetricsKeys.SENT_APP_OPEN_BY_DAY]      = JsonUtils.toDayIntMap(appOpen)
            e[MetricsKeys.SENT_TOOL_USE_BY_DAY_JSON] = JsonUtils.toDayNestedIntMap(tools)
            e[MetricsKeys.SENT_AD_IMPR_BY_DAY_JSON]  = JsonUtils.toDayNestedIntMap(ads)
            e[MetricsKeys.PENDING_BATCH_ID]          = ""
            e[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON]= ""
        }
    }
}
