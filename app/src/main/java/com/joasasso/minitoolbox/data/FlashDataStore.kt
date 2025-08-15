package com.joasasso.minitoolbox.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore para preferencias de la linterna
val Context.flashDataStore by preferencesDataStore(name = "flash_prefs")

// Clave para nivel de intensidad 0..20 (0=apagado)
private val KEY_FLASH_LEVEL = intPreferencesKey("flash_intensity_level")

// Nivel por defecto si aún no se guardó nada
private const val DEFAULT_LEVEL = 20 // podés cambiarlo

// Flujo de nivel persistido (0..20)
fun Context.flujoNivelLinterna(): Flow<Int> {
    return flashDataStore.data.map { prefs ->
        (prefs[KEY_FLASH_LEVEL] ?: DEFAULT_LEVEL).coerceIn(0, 20)
    }
}

// Guardar nivel (0..20)
suspend fun Context.setNivelLinterna(level: Int) {
    val safe = level.coerceIn(0, 20)
    flashDataStore.edit { prefs ->
        prefs[KEY_FLASH_LEVEL] = safe
    }
}

private val KEY_FLASH_ACTIVA = booleanPreferencesKey("flash_activa")

fun Context.flujoEstadoLinterna(): Flow<Boolean> {
    return flashDataStore.data.map { it[KEY_FLASH_ACTIVA] ?: false }
}

suspend fun Context.setEstadoLinterna(activa: Boolean) {
    flashDataStore.edit { it[KEY_FLASH_ACTIVA] = activa }
}
