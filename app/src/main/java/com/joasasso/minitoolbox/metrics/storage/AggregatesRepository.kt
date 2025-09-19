package com.joasasso.minitoolbox.metrics.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.joasasso.minitoolbox.metrics.storage.MetricsKeys.AD_IMPRESSIONS_JSON
import com.joasasso.minitoolbox.metrics.storage.MetricsKeys.INSTALL_DATE_MS
import com.joasasso.minitoolbox.metrics.storage.MetricsKeys.IS_PREMIUM
import com.joasasso.minitoolbox.metrics.storage.MetricsKeys.LAST_DAILY_OPEN_YYYYMMDD
import com.joasasso.minitoolbox.metrics.storage.MetricsKeys.METRICS_CONSENT_ENABLED
import com.joasasso.minitoolbox.metrics.storage.MetricsKeys.PREMIUM_SINCE_MS
import com.joasasso.minitoolbox.metrics.storage.MetricsKeys.TIME_TO_PURCHASE_MS
import com.joasasso.minitoolbox.metrics.storage.MetricsKeys.TOOL_USE_COUNTS_JSON
import com.joasasso.minitoolbox.metrics.storage.MetricsKeys.TOTAL_TOOL_OPEN_COUNT
import com.joasasso.minitoolbox.metrics.storage.MetricsKeys.USES_BY_DAY_JSON
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

private val ISO_DAY = SimpleDateFormat("yyyy-MM-dd", Locale.US)

class AggregatesRepository(private val context: Context) {

    // ---------- Helpers de fecha ----------
    fun todayIso(): String = ISO_DAY.format(Date())

    // ---------- Instalación ----------
    suspend fun markInstallIfNeeded(nowMs: Long = System.currentTimeMillis()): Boolean {
        val ds = context.metricsDataStore
        val prefs = try { ds.data.first() } catch (_: Throwable) { emptyPreferences() }
        if (prefs[INSTALL_DATE_MS] != null) return false

        ds.edit { it[INSTALL_DATE_MS] = nowMs }
        return true
    }

    suspend fun getInstallDateMs(): Long? {
        val prefs = context.metricsDataStore.data.first()
        return prefs[INSTALL_DATE_MS]
    }

    // ---------- Daily open ----------
    /**
     * Devuelve true si registró el daily_open (cambió de día).
     */
    suspend fun dailyOpenIfNeeded(): Boolean {
        val ds = context.metricsDataStore
        val today = todayIso()
        var fired = false
        ds.edit { e ->
            val last = e[LAST_DAILY_OPEN_YYYYMMDD]
            if (last != today) {
                e[LAST_DAILY_OPEN_YYYYMMDD] = today
                // Incrementa contador usesByDay[today]++
                val usesMap: MutableMap<String, Int> =
                    JsonUtils.fromJsonMap<String, Int>(e[USES_BY_DAY_JSON])
                usesMap[today] = (usesMap[today] ?: 0) + 1
                e[USES_BY_DAY_JSON] = JsonUtils.toJsonMap(usesMap)

                fired = true
            }
        }
        return fired
    }

    // ---------- Tool opens ----------
    suspend fun incrementToolOpen(toolId: String) {
        val ds = context.metricsDataStore
        ds.edit { e ->
            val map: MutableMap<String, Int> =
                JsonUtils.fromJsonMap<String, Int>(e[TOOL_USE_COUNTS_JSON])
            map[toolId] = (map[toolId] ?: 0) + 1
            e[TOOL_USE_COUNTS_JSON] = JsonUtils.toJsonMap(map)
        }

    }

    suspend fun getToolUseCounts(): Map<String, Int> {
        val prefs = context.metricsDataStore.data.first()
        val map: MutableMap<String, Int> =
            JsonUtils.fromJsonMap<String, Int>(prefs[TOOL_USE_COUNTS_JSON])
        return map.toMap()

    }

    // ---------- Total de aperturas de tools ----------
    suspend fun incrementTotalToolOpenAndGet(): Int {
        var newTotal = 0
        context.metricsDataStore.edit { e ->
            val current = e[TOTAL_TOOL_OPEN_COUNT] ?: 0
            newTotal = current + 1
            e[TOTAL_TOOL_OPEN_COUNT] = newTotal
        }
        return newTotal
    }

    suspend fun getTotalToolOpenCount(): Int {
        val prefs = context.metricsDataStore.data.first()
        return prefs[TOTAL_TOOL_OPEN_COUNT] ?: 0
    }


    // ---------- Ads ----------
    suspend fun incrementAdImpression(type: String) {
        val ds = context.metricsDataStore
        ds.edit { e ->
            val map: MutableMap<String, Int> =
                JsonUtils.fromJsonMap<String, Int>(e[AD_IMPRESSIONS_JSON])
            map[type] = (map[type] ?: 0) + 1
            e[AD_IMPRESSIONS_JSON] = JsonUtils.toJsonMap(map)
        }

    }

    suspend fun getAdImpressions(): Map<String, Int> {
        val prefs = context.metricsDataStore.data.first()
        val map: MutableMap<String, Int> =
            JsonUtils.fromJsonMap<String, Int>(prefs[AD_IMPRESSIONS_JSON])
        return map.toMap()

    }

    // ---------- Premium ----------
    /**
     * Activa premium si no lo estaba. Devuelve true si hubo cambio de estado.
     * Calcula premiumSince y timeToPurchase si corresponde.
     */
    suspend fun setPremiumActive(nowMs: Long = System.currentTimeMillis()): Boolean {
        val ds = context.metricsDataStore
        var changed = false
        ds.edit { e ->
            val wasPremium = e[IS_PREMIUM] ?: false
            if (!wasPremium) {
                e[IS_PREMIUM] = true
                if ((e[PREMIUM_SINCE_MS] ?: 0L) == 0L) {
                    e[PREMIUM_SINCE_MS] = nowMs
                    val install = e[INSTALL_DATE_MS] ?: nowMs
                    val delta = max(0L, nowMs - install)
                    e[TIME_TO_PURCHASE_MS] = delta
                }
                changed = true
            }
        }
        return changed
    }

    suspend fun getPremiumInfo(): PremiumInfo {
        val prefs = context.metricsDataStore.data.first()
        return PremiumInfo(
            isPremium = prefs[IS_PREMIUM] ?: false,
            premiumSinceMs = prefs[PREMIUM_SINCE_MS] ?: 0L,
            timeToPurchaseMs = prefs[TIME_TO_PURCHASE_MS] ?: 0L
        )
    }

    data class PremiumInfo(
        val isPremium: Boolean,
        val premiumSinceMs: Long,
        val timeToPurchaseMs: Long
    )

    // ---------- Consentimiento interno (toggle “Métricas anónimas”) ----------
    suspend fun setMetricsConsent(enabled: Boolean) {
        context.metricsDataStore.edit { it[METRICS_CONSENT_ENABLED] = enabled }
    }

    suspend fun isMetricsConsentEnabled(): Boolean {
        val prefs = context.metricsDataStore.data.first()
        return prefs[METRICS_CONSENT_ENABLED] ?: false
    }
}
