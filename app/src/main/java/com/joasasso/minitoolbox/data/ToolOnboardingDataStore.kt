package com.joasasso.minitoolbox.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.toolOnboardingDataStore by preferencesDataStore(name = "tool_onboarding")

object ToolOnboardingKeys {
    const val COMPASS = "compass"
    const val BUBBLE_LEVEL = "bubble_level"
    const val LIGHT_SENSOR = "light_sensor"
    const val RULER = "ruler"
}

fun onboardingKey(toolKey: String) = booleanPreferencesKey("onboarding_dismissed_$toolKey")

/**
 * Flujo reactivo que indica si la sugerencia inicial / onboarding de una herramienta
 * ya fue vista o descartada por el usuario.
 */
fun Context.flujoOnboardingVisto(toolKey: String): Flow<Boolean> =
    toolOnboardingDataStore.data.map { prefs ->
        prefs[onboardingKey(toolKey)] ?: false
    }

/**
 * Marca atómicamente la sugerencia de una herramienta como vista/descartada en DataStore.
 */
suspend fun Context.marcarOnboardingVisto(toolKey: String) {
    toolOnboardingDataStore.edit { prefs ->
        prefs[onboardingKey(toolKey)] = true
    }
}

/**
 * Reinicia el estado de sugerencia de una herramienta específica o de todas las herramientas.
 */
suspend fun Context.resetOnboarding(toolKey: String? = null) {
    toolOnboardingDataStore.edit { prefs ->
        if (toolKey != null) {
            prefs.remove(onboardingKey(toolKey))
        } else {
            prefs.clear()
        }
    }
}
