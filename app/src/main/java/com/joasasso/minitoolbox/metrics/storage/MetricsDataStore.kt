package com.joasasso.minitoolbox.metrics.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.metricsDataStore: androidx.datastore.core.DataStore<Preferences> by preferencesDataStore(
    name = "metrics_prefs"
)

object MetricsKeys {
    // Agregados locales por día
    val APP_OPEN_COUNT_BY_DAY      = stringPreferencesKey("app_open_count_by_day")       // Map<day, Int>
    val TOOL_USE_BY_DAY_JSON       = stringPreferencesKey("tool_use_by_day_json")        // Map<day, Map<toolId, Int>>
    val AD_IMPRESSIONS_BY_DAY_JSON = stringPreferencesKey("ad_impr_by_day_json")         // Map<day, Map<type, Int>>

    // Envíos (idempotencia/deltas)
    val SENT_APP_OPEN_BY_DAY       = stringPreferencesKey("sent_app_open_by_day")        // Map<day, Int>
    val SENT_TOOL_USE_BY_DAY_JSON  = stringPreferencesKey("sent_tool_use_by_day_json")   // Map<day, Map<toolId, Int>>
    val SENT_AD_IMPR_BY_DAY_JSON   = stringPreferencesKey("sent_ad_impr_by_day_json")    // Map<day, Map<type, Int>>
    val PENDING_BATCH_ID           = stringPreferencesKey("pending_batch_id")            // String?
    val PENDING_BATCH_PAYLOAD_JSON = stringPreferencesKey("pending_batch_payload_json")  // String?
}
