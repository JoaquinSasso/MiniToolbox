package com.joasasso.minitoolbox.metrics.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.metricsDataStore: androidx.datastore.core.DataStore<Preferences> by preferencesDataStore(
    name = "metrics_prefs"
)

object MetricsKeys {

    // ---------------------------------------------------------------------
    // Versionado del esquema local
    // ---------------------------------------------------------------------
    /**
     * Versión del esquema de datos guardado en este DataStore.
     * La compara [MetricsSanitizer] para decidir si hace falta una pasada de saneo.
     * Subir la constante en el sanitizador fuerza una migración en todos los dispositivos.
     */
    val SCHEMA_VERSION = intPreferencesKey("schema_version")

    // ---------------------------------------------------------------------
    // Agregados locales por día
    // ---------------------------------------------------------------------
    val APP_OPEN_COUNT_BY_DAY      = stringPreferencesKey("app_open_count_by_day")       // Map<day, Int>
    val DAILY_ACTIVE_BY_DAY        = stringPreferencesKey("daily_active_by_day")         // Map<day, Int>
    val TOOL_USE_BY_DAY_JSON       = stringPreferencesKey("tool_use_by_day_json")        // Map<day, Map<toolId, Int>>
    val AD_IMPRESSIONS_BY_DAY_JSON = stringPreferencesKey("ad_impr_by_day_json")         // Map<day, Map<type, Int>>

    // Versiones
    val VERSION_DAU_BY_DAY_JSON            = stringPreferencesKey("version_dau_by_day_json")             // Map<day, Map<version, Int>>
    val VERSION_FIRST_SEEN_BY_DAY_JSON     = stringPreferencesKey("version_first_seen_by_day_json")      // Map<day, Map<version, Int>>

    // Idiomas (principal y secundario)
    val LANG_PRIMARY_BY_DAY_JSON           = stringPreferencesKey("lang_primary_by_day_json")            // Map<day, Map<lang, Int>>
    val LANG_SECONDARY_BY_DAY_JSON         = stringPreferencesKey("lang_secondary_by_day_json")          // Map<day, Map<lang, Int>>

    // Widgets (interacciones por tipo)
    val WIDGET_USE_BY_DAY_JSON             = stringPreferencesKey("widget_use_by_day_json")              // Map<day, Map<widgetType, Int>>

    // ---------------------------------------------------------------------
    // Envíos (idempotencia / cálculo de deltas)
    // ---------------------------------------------------------------------
    val SENT_APP_OPEN_BY_DAY       = stringPreferencesKey("sent_app_open_by_day")        // Map<day, Int>
    val SENT_DAILY_ACTIVE_BY_DAY   = stringPreferencesKey("sent_daily_active_by_day")    // Map<day, Int>
    val SENT_TOOL_USE_BY_DAY_JSON  = stringPreferencesKey("sent_tool_use_by_day_json")   // Map<day, Map<toolId, Int>>
    val SENT_AD_IMPR_BY_DAY_JSON   = stringPreferencesKey("sent_ad_impr_by_day_json")    // Map<day, Map<type, Int>>

    val SENT_VERSION_DAU_BY_DAY_JSON        = stringPreferencesKey("sent_version_dau_by_day_json")       // Map<day, Map<version, Int>>
    val SENT_VERSION_FIRST_SEEN_BY_DAY_JSON = stringPreferencesKey("sent_version_first_seen_by_day_json")// Map<day, Map<version, Int>>

    val SENT_LANG_PRIMARY_BY_DAY_JSON       = stringPreferencesKey("sent_lang_primary_by_day_json")      // Map<day, Map<lang, Int>>
    val SENT_LANG_SECONDARY_BY_DAY_JSON     = stringPreferencesKey("sent_lang_secondary_by_day_json")    // Map<day, Map<lang, Int>>

    val SENT_WIDGET_USE_BY_DAY_JSON         = stringPreferencesKey("sent_widget_use_by_day_json")        // Map<day, Map<widgetType, Int>>

    // ---------------------------------------------------------------------
    // Lote congelado pendiente de envío
    // ---------------------------------------------------------------------
    val PENDING_BATCH_ID           = stringPreferencesKey("pending_batch_id")            // String?
    val PENDING_BATCH_PAYLOAD_JSON = stringPreferencesKey("pending_batch_payload_json")  // String?

    /**
     * Momento en que se congeló el lote pendiente, en millis epoch.
     * Permite descartar lotes demasiado viejos en lugar de reintentarlos para siempre.
     */
    val PENDING_BATCH_CREATED_AT   = longPreferencesKey("pending_batch_created_at")

    // ---------------------------------------------------------------------
    // Flags locales para "una vez por día / primera vez"
    // ---------------------------------------------------------------------
    val LAST_VERSION_HEARTBEAT_DAY = stringPreferencesKey("last_version_heartbeat_day")  // "yyyy-MM-dd"
    val LAST_LANG_HEARTBEAT_DAY    = stringPreferencesKey("last_lang_heartbeat_day")     // "yyyy-MM-dd"
    val FIRST_SEEN_VERSIONS_JSON   = stringPreferencesKey("first_seen_versions_json")    // JSON Array de versiones ya registradas

    // ---------------------------------------------------------------------
    // Diagnóstico del pipeline de envío
    //
    // Sin estos contadores no hay forma de saber que un dispositivo dejó de
    // reportar: el worker puede fallar indefinidamente sin dejar ningún rastro
    // observable ni en el cliente ni en el servidor.
    // ---------------------------------------------------------------------
    /** Último código HTTP recibido del endpoint. 0 si nunca se intentó. */
    val LAST_UPLOAD_CODE          = intPreferencesKey("last_upload_code")

    /** Último error de envío, recortado. Vacío si el último intento fue exitoso. */
    val LAST_UPLOAD_ERROR         = stringPreferencesKey("last_upload_error")

    /** Millis epoch del último envío exitoso. 0 si nunca hubo uno. */
    val LAST_SUCCESS_AT           = longPreferencesKey("last_success_at")

    /** Fallos consecutivos desde el último éxito. Se reinicia en cada 2xx. */
    val CONSECUTIVE_FAILURES      = intPreferencesKey("consecutive_failures")

    /** Lotes descartados por rechazo permanente o por agotar los reintentos. */
    val DROPPED_BATCHES           = intPreferencesKey("dropped_batches")

    /** Claves corregidas o descartadas por el sanitizador, acumulado histórico. */
    val SANITIZED_KEYS_TOTAL      = intPreferencesKey("sanitized_keys_total")

    /** Si el último envío llevó token de App Check. Diagnóstico para la DevScreen. */
    val LAST_UPLOAD_USED_APPCHECK = booleanPreferencesKey("last_upload_used_appcheck")
}