package com.joasasso.minitoolbox.metrics.storage

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

// Nombre del archivo de preferencias
private const val METRICS_PREFS_NAME = "metrics_prefs"

// Extension para obtener el DataStore desde un Context
val Context.metricsDataStore by preferencesDataStore(name = METRICS_PREFS_NAME)

// Claves de DataStore (todas en un solo lugar)
object MetricsKeys {
    val APP_OPEN_COUNT_BY_DAY       = stringPreferencesKey("app_open_count_by_day")        // Map<String, Int>
    val TOOL_USE_BY_DAY_JSON        = stringPreferencesKey("tool_use_by_day_json")         // Map<String, Map<String,Int>>
    val AD_IMPRESSIONS_BY_DAY_JSON  = stringPreferencesKey("ad_impr_by_day_json")          // Map<String, Map<String,Int>>
    val APP_VERSION_AT_UPLOAD       = stringPreferencesKey("app_version_at_upload")
    val LAST_UPLOAD_DAY             = stringPreferencesKey("last_upload_day")

    val SENT_APP_OPEN_BY_DAY        = stringPreferencesKey("sent_app_open_by_day")         // Map<String,Int>
    val SENT_TOOL_USE_BY_DAY_JSON   = stringPreferencesKey("sent_tool_use_by_day_json")    // Map<String,Map<String,Int>>
    val SENT_AD_IMPR_BY_DAY_JSON    = stringPreferencesKey("sent_ad_impr_by_day_json")     // Map<String,Map<String,Int>>
    val PENDING_BATCH_ID            = stringPreferencesKey("pending_batch_id")             // String? (idempotencia)
    val PENDING_BATCH_PAYLOAD_JSON  = stringPreferencesKey("pending_batch_payload_json")   // String? (reintentos)


}
