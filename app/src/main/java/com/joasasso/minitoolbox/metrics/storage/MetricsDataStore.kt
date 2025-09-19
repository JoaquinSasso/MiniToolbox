package com.joasasso.minitoolbox.metrics.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

// Nombre del archivo de preferencias
private const val METRICS_PREFS_NAME = "metrics_prefs"

// Extension para obtener el DataStore desde un Context
val Context.metricsDataStore by preferencesDataStore(name = METRICS_PREFS_NAME)

// Claves de DataStore (todas en un solo lugar)
object MetricsKeys {
    // Identidad anónima
    val INSTANCE_ID        = stringPreferencesKey("instance_id")         // UUID local (no se envía)
    val INSTANCE_SALT_B64  = stringPreferencesKey("instance_salt_b64")   // salt en Base64 (no se envía)
    val INSTANCE_HASH_HEX  = stringPreferencesKey("instance_hash_hex")   // hash que sí podrías enviar

    // Instalación y uso diario
    val INSTALL_DATE_MS         = longPreferencesKey("install_date_ms")       // primera vez que abrió
    val LAST_DAILY_OPEN_YYYYMMDD= stringPreferencesKey("last_daily_open")     // última fecha YYYY-MM-DD

    // Estado premium y tiempos
    val IS_PREMIUM          = booleanPreferencesKey("is_premium")
    val PREMIUM_SINCE_MS    = longPreferencesKey("premium_since_ms")
    val TIME_TO_PURCHASE_MS = longPreferencesKey("time_to_purchase_ms") // derivado (premiumSince - install)

    // Contadores agregados (JSON serializado)
    // tool_id -> count
    val TOOL_USE_COUNTS_JSON    = stringPreferencesKey("tool_use_counts_json")
    // yyyy-MM-dd -> count de aperturas de app ese día
    val USES_BY_DAY_JSON        = stringPreferencesKey("uses_by_day_json")
    // tipo anuncio (banner/interstitial/rewarded/appopen/...) -> impresiones
    val AD_IMPRESSIONS_JSON     = stringPreferencesKey("ad_impressions_json")

    // Privacidad / consentimiento (tu toggle interno)
    val METRICS_CONSENT_ENABLED = booleanPreferencesKey("metrics_consent_enabled")

    //Cantidad total de tools abiertas
    val TOTAL_TOOL_OPEN_COUNT = intPreferencesKey("total_tool_open_count")

}
