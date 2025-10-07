package com.joasasso.minitoolbox.metrics.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.metricsDataStore: androidx.datastore.core.DataStore<Preferences> by preferencesDataStore(
    name = "metrics_prefs"
)

object MetricsKeys {
    // Agregados locales por día (existentes)
    val APP_OPEN_COUNT_BY_DAY      = stringPreferencesKey("app_open_count_by_day")       // Map<day, Int>
    val TOOL_USE_BY_DAY_JSON       = stringPreferencesKey("tool_use_by_day_json")        // Map<day, Map<toolId, Int>>
    val AD_IMPRESSIONS_BY_DAY_JSON = stringPreferencesKey("ad_impr_by_day_json")         // Map<day, Map<type, Int>>

    // -------- NUEVOS: Versiones, Idiomas, Widgets (agregados por día) --------
    // Versiones
    val VERSION_DAU_BY_DAY_JSON            = stringPreferencesKey("version_dau_by_day_json")             // Map<day, Map<version, Int>>
    val VERSION_FIRST_SEEN_BY_DAY_JSON     = stringPreferencesKey("version_first_seen_by_day_json")      // Map<day, Map<version, Int>>

    // Idiomas (principal y secundario)
    val LANG_PRIMARY_BY_DAY_JSON           = stringPreferencesKey("lang_primary_by_day_json")            // Map<day, Map<lang, Int>>
    val LANG_SECONDARY_BY_DAY_JSON         = stringPreferencesKey("lang_secondary_by_day_json")          // Map<day, Map<lang, Int>>

    // Widgets (interacciones por tipo)
    val WIDGET_USE_BY_DAY_JSON             = stringPreferencesKey("widget_use_by_day_json")              // Map<day, Map<widgetType, Int>>

    // Envíos (idempotencia/deltas) para Usos de app / Usos de Tools / Impresiones de anuncios
    val SENT_APP_OPEN_BY_DAY       = stringPreferencesKey("sent_app_open_by_day")        // Map<day, Int>
    val SENT_TOOL_USE_BY_DAY_JSON  = stringPreferencesKey("sent_tool_use_by_day_json")   // Map<day, Map<toolId, Int>>
    val SENT_AD_IMPR_BY_DAY_JSON   = stringPreferencesKey("sent_ad_impr_by_day_json")    // Map<day, Map<type, Int>>
    val PENDING_BATCH_ID           = stringPreferencesKey("pending_batch_id")            // String?
    val PENDING_BATCH_PAYLOAD_JSON = stringPreferencesKey("pending_batch_payload_json")  // String?

    // Envíos (idempotencia/deltas) para Versiones / Idiomas / Widgets --------
    val SENT_VERSION_DAU_BY_DAY_JSON        = stringPreferencesKey("sent_version_dau_by_day_json")       // Map<day, Map<version, Int>>
    val SENT_VERSION_FIRST_SEEN_BY_DAY_JSON = stringPreferencesKey("sent_version_first_seen_by_day_json")// Map<day, Map<version, Int>>

    val SENT_LANG_PRIMARY_BY_DAY_JSON       = stringPreferencesKey("sent_lang_primary_by_day_json")      // Map<day, Map<lang, Int>>
    val SENT_LANG_SECONDARY_BY_DAY_JSON     = stringPreferencesKey("sent_lang_secondary_by_day_json")    // Map<day, Map<lang, Int>>

    val SENT_WIDGET_USE_BY_DAY_JSON         = stringPreferencesKey("sent_widget_use_by_day_json")        // Map<day, Map<widgetType, Int>>

    //Flags locales para “una vez por día / primera vez” --------
    val LAST_VERSION_HEARTBEAT_DAY = stringPreferencesKey("last_version_heartbeat_day")  // "yyyy-MM-dd"
    val LAST_LANG_HEARTBEAT_DAY    = stringPreferencesKey("last_lang_heartbeat_day")     // "yyyy-MM-dd"
    val FIRST_SEEN_VERSIONS_JSON   = stringPreferencesKey("first_seen_versions_json")    // JSON Array de versiones ya registradas
}
