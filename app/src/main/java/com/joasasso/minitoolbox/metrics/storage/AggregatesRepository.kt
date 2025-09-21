package com.joasasso.minitoolbox.metrics.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repositorio de métricas 100% agregadas (sin IDs, sin consentimiento).
 * Guarda contadores diarios por clave (tool_id, ad_type, etc.).
 *
 * Claves esperadas en MetricsKeys:
 *  - APP_OPEN_COUNT_BY_DAY       : Map<String /*yyyy-MM-dd*/, Int>
 *  - TOOL_USE_BY_DAY_JSON        : Map<String /*day*/, Map<String /*toolId*/, Int>>
 *  - AD_IMPRESSIONS_BY_DAY_JSON  : Map<String /*day*/, Map<String /*adType*/, Int>>
 *  - APP_VERSION_AT_UPLOAD       : String (opcional, si etiquetás las subidas)
 *  - LAST_UPLOAD_DAY             : String (opcional)
 */
class AggregatesRepository(private val context: Context) {

    // ---------- Utils de fecha ----------
    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private fun today(): String = dayFmt.format(Date())

    // ---------- Helpers JSON ----------
    private inline fun <reified K, reified V> fromJsonMap(json: String?): MutableMap<K, V> {
        return JsonUtils.fromJsonMap(json)
    }
    private fun <K, V> toJsonMap(map: Map<K, V>): String = JsonUtils.toJsonMap(map)

    // ---------- Extensión para borrar buckets anteriores a una fecha ----------
    private fun <V> MutableMap<String, V>.removeBefore(cutIsoDay: String) {
        val toRemove = keys.filter { it < cutIsoDay }
        toRemove.forEach { remove(it) }
    }

    // -------------------------------------------------------------------------
    // APP OPEN (aperturas globales por día)
    // -------------------------------------------------------------------------
    suspend fun incrementAppOpen() {
        val ds = context.metricsDataStore
        val day = today()
        ds.edit { e ->
            val byDay: MutableMap<String, Int> =
                fromJsonMap(e[MetricsKeys.APP_OPEN_COUNT_BY_DAY])
            byDay[day] = (byDay[day] ?: 0) + 1
            e[MetricsKeys.APP_OPEN_COUNT_BY_DAY] = toJsonMap(byDay)
        }
    }

    // -------------------------------------------------------------------------
    // TOOL USE (uso de herramientas por día y por toolId)
    // -------------------------------------------------------------------------
    suspend fun incrementToolUse(toolId: String) {
        val ds = context.metricsDataStore
        val day = today()
        ds.edit { e ->
            val byDay: MutableMap<String, MutableMap<String, Int>> =
                fromJsonMap(e[MetricsKeys.TOOL_USE_BY_DAY_JSON])
            val bucket = byDay.getOrPut(day) { mutableMapOf() }
            bucket[toolId] = (bucket[toolId] ?: 0) + 1
            e[MetricsKeys.TOOL_USE_BY_DAY_JSON] = toJsonMap(byDay)
        }
    }

    // -------------------------------------------------------------------------
    // AD IMPRESSIONS (impresiones por día y por tipo de anuncio)
    // -------------------------------------------------------------------------
    suspend fun incrementAdImpression(type: String) {
        val ds = context.metricsDataStore
        val day = today()
        ds.edit { e ->
            val byDay: MutableMap<String, MutableMap<String, Int>> =
                fromJsonMap(e[MetricsKeys.AD_IMPRESSIONS_BY_DAY_JSON])
            val bucket = byDay.getOrPut(day) { mutableMapOf() }
            bucket[type] = (bucket[type] ?: 0) + 1
            e[MetricsKeys.AD_IMPRESSIONS_BY_DAY_JSON] = toJsonMap(byDay)
        }
    }

    // -------------------------------------------------------------------------
    // Lecturas (para panel interno, debug o subida)
    // -------------------------------------------------------------------------
    suspend fun getAppOpenCounts(): Map<String, Int> {
        val prefs = try { context.metricsDataStore.data.first() } catch (_: Throwable) { emptyPreferences() }
        val byDay: MutableMap<String, Int> =
            fromJsonMap(prefs[MetricsKeys.APP_OPEN_COUNT_BY_DAY])
        return byDay.toMap()
    }

    suspend fun getToolUseByDay(): Map<String, Map<String, Int>> {
        val prefs = try { context.metricsDataStore.data.first() } catch (_: Throwable) { emptyPreferences() }
        val raw: MutableMap<String, MutableMap<String, Int>> =
            fromJsonMap(prefs[MetricsKeys.TOOL_USE_BY_DAY_JSON])
        // inmutable hacia afuera
        return raw.mapValues { it.value.toMap() }
    }

    suspend fun getAdImpressionsByDay(): Map<String, Map<String, Int>> {
        val prefs = try { context.metricsDataStore.data.first() } catch (_: Throwable) { emptyPreferences() }
        val raw: MutableMap<String, MutableMap<String, Int>> =
            fromJsonMap(prefs[MetricsKeys.AD_IMPRESSIONS_BY_DAY_JSON])
        return raw.mapValues { it.value.toMap() }
    }

}
