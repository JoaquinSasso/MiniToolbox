package com.joasasso.minitoolbox.metrics.storage

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import android.content.Context
import com.joasasso.minitoolbox.metrics.MetricsContract
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Saneo automático del almacenamiento local de métricas.
 *
 * Existe por un incidente concreto: una clave de herramienta generada a partir de
 * una ruta de navegación ("pomodoro/detail/<uuid>") no cumplía el contrato del
 * backend, el lote quedaba congelado esperando reintento, y como el payload nunca
 * se reconstruía el dispositivo dejaba de reportar métricas de forma permanente.
 *
 * Este objeto garantiza dos invariantes:
 *
 * 1. Todo lo que está en disco cumple [MetricsContract] antes de que el worker lo lea.
 * 2. Ningún dato corrupto puede dejar el pipeline detenido: se corrige o se descarta,
 *    pero nunca se bloquea.
 *
 * Es idempotente: correrlo dos veces seguidas no cambia nada la segunda vez.
 */
object MetricsSanitizer {

    /**
     * Versión del esquema local. Subir este número fuerza una pasada de saneo en
     * todos los dispositivos la próxima vez que abran la app.
     *
     * Historial:
     *  1 (implícito) — esquema previo, sin saneo.
     *  2 — primer saneo: normaliza claves de ruta y descarta días mal formados.
     */
    const val SCHEMA_VERSION = 2

    /**
     * Días de historia que se conservan en los contadores locales.
     * Un dispositivo que no puede enviar durante meses no debe hacer crecer el
     * DataStore sin techo.
     */
    private const val MAX_HISTORY_DAYS = 60

    /** Mapas día -> (clave -> contador). Incluye los acumulados y sus pares "enviados". */
    private val NESTED_KEYS = listOf(
        MetricsKeys.TOOL_USE_BY_DAY_JSON,
        MetricsKeys.AD_IMPRESSIONS_BY_DAY_JSON,
        MetricsKeys.VERSION_DAU_BY_DAY_JSON,
        MetricsKeys.VERSION_FIRST_SEEN_BY_DAY_JSON,
        MetricsKeys.LANG_PRIMARY_BY_DAY_JSON,
        MetricsKeys.LANG_SECONDARY_BY_DAY_JSON,
        MetricsKeys.WIDGET_USE_BY_DAY_JSON,
        MetricsKeys.SENT_TOOL_USE_BY_DAY_JSON,
        MetricsKeys.SENT_AD_IMPR_BY_DAY_JSON,
        MetricsKeys.SENT_VERSION_DAU_BY_DAY_JSON,
        MetricsKeys.SENT_VERSION_FIRST_SEEN_BY_DAY_JSON,
        MetricsKeys.SENT_LANG_PRIMARY_BY_DAY_JSON,
        MetricsKeys.SENT_LANG_SECONDARY_BY_DAY_JSON,
        MetricsKeys.SENT_WIDGET_USE_BY_DAY_JSON
    )

    /** Mapas día -> contador. */
    private val FLAT_KEYS = listOf(
        MetricsKeys.APP_OPEN_COUNT_BY_DAY,
        MetricsKeys.DAILY_ACTIVE_BY_DAY,
        MetricsKeys.SENT_APP_OPEN_BY_DAY,
        MetricsKeys.SENT_DAILY_ACTIVE_BY_DAY
    )

    /**
     * Resultado de una pasada de saneo.
     *
     * @property fixedKeys claves normalizadas (por ejemplo "a/b" -> "a_b")
     * @property droppedKeys claves descartadas por no ser recuperables
     * @property droppedDays días descartados por formato inválido o por antigüedad
     * @property clearedPendingPayload si se descartó el payload congelado
     */
    data class Report(
        val fixedKeys: Int = 0,
        val droppedKeys: Int = 0,
        val droppedDays: Int = 0,
        val clearedPendingPayload: Boolean = false
    ) {
        val changed: Boolean
            get() = fixedKeys > 0 || droppedKeys > 0 || droppedDays > 0
    }

    /**
     * Corre el saneo solo si el esquema en disco está desactualizado.
     * Es barato de llamar en cada arranque y en cada ejecución del worker.
     */
    suspend fun runIfNeeded(context: Context): Report? {
        val prefs = readPrefs(context)
        if (prefs[MetricsKeys.SCHEMA_VERSION] == SCHEMA_VERSION) return null

        val report = sanitizeNow(context, discardPendingPayload = true)
        context.metricsDataStore.edit { it[MetricsKeys.SCHEMA_VERSION] = SCHEMA_VERSION }
        return report
    }

    /**
     * Sanea todos los mapas del DataStore. Idempotente.
     *
     * @param discardPendingPayload borra el JSON del lote congelado pero CONSERVA su
     *  batch_id, para que el worker lo reconstruya con el mismo identificador. Reusar
     *  el id evita el doble conteo si el servidor sí llegó a persistir el lote anterior
     *  y la respuesta no llegó al cliente: en ese caso la deduplicación del backend
     *  descarta el reenvío. Perder un delta parcial es preferible a duplicar agregados.
     */
    suspend fun sanitizeNow(context: Context, discardPendingPayload: Boolean): Report {
        var fixed = 0
        var dropped = 0
        var droppedDays = 0
        val cutoff = cutoffDay()

        context.metricsDataStore.edit { prefs ->
            for (key in NESTED_KEYS) {
                val byDay = JsonUtils.fromDayNestedIntMap(prefs[key])
                val out = LinkedHashMap<String, MutableMap<String, Int>>()

                for ((day, counters) in byDay) {
                    if (!MetricsContract.isValidDay(day) || day < cutoff) {
                        droppedDays++
                        continue
                    }
                    val report = MetricsContract.sanitizeCounters(counters)
                    fixed += report.fixedKeys
                    dropped += report.droppedKeys
                    if (report.clean.isNotEmpty()) out[day] = report.clean
                }

                prefs[key] = JsonUtils.toDayNestedIntMap(out)
            }

            for (key in FLAT_KEYS) {
                val byDay = JsonUtils.fromDayIntMap(prefs[key])
                val out = LinkedHashMap<String, Int>()

                for ((day, value) in byDay) {
                    if (!MetricsContract.isValidDay(day) || day < cutoff || value < 0) {
                        droppedDays++
                        continue
                    }
                    out[day] = value
                }

                prefs[key] = JsonUtils.toDayIntMap(out)
            }

            if (discardPendingPayload) {
                prefs[MetricsKeys.PENDING_BATCH_PAYLOAD_JSON] = ""
                prefs[MetricsKeys.PENDING_BATCH_CREATED_AT] = 0L
                // PENDING_BATCH_ID se conserva a propósito: ver el KDoc del parámetro.
            }

            val previous = prefs[MetricsKeys.SANITIZED_KEYS_TOTAL] ?: 0
            prefs[MetricsKeys.SANITIZED_KEYS_TOTAL] = previous + fixed + dropped
        }

        return Report(
            fixedKeys = fixed,
            droppedKeys = dropped,
            droppedDays = droppedDays,
            clearedPendingPayload = discardPendingPayload
        )
    }

    /** Día más antiguo que se conserva, en formato "yyyy-MM-dd". */
    private fun cutoffDay(): String {
        val cal = Calendar.getInstance()
        cal.time = Date()
        cal.add(Calendar.DAY_OF_YEAR, -MAX_HISTORY_DAYS)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    private suspend fun readPrefs(context: Context): Preferences = try {
        context.metricsDataStore.data.first()
    } catch (_: Throwable) {
        emptyPreferences()
    }
}