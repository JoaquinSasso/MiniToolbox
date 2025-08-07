package com.joasasso.minitoolbox.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// DataStore de favoritos
val Context.favoritesDataStore by preferencesDataStore(name = "tool_favorites")

// Claves para un máximo de 100 herramientas favoritas
val FAVORITOS_KEYS = (1..100).map { stringPreferencesKey("favorita_$it") }

// Lectura de favoritos (en orden)
fun Context.flujoToolsFavoritas(): Flow<List<String>> {
    return favoritesDataStore.data.map { prefs ->
        FAVORITOS_KEYS.mapNotNull { prefs[it] }
    }
}

// Alternar herramienta como favorita o no (agrega si no está, elimina si ya está)
suspend fun Context.toogleFavorite(route: String) {
    val prefs = favoritesDataStore.data.first()
    val actuales = FAVORITOS_KEYS.mapNotNull { prefs[it] }.toMutableList()

    if (actuales.contains(route)) {
        actuales.remove(route)
    } else {
        actuales.add(route)
    }

    // Reescribir las claves FAVORITOS_KEYS con la nueva lista
    favoritesDataStore.edit { editor ->
        FAVORITOS_KEYS.forEachIndexed { index, key ->
            if (index < actuales.size) {
                editor[key] = actuales[index]
            } else {
                editor.remove(key)
            }
        }
    }
}
