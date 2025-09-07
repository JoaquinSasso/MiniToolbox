package com.joasasso.minitoolbox.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore de favoritos (tuya)
val Context.favoritesDataStore by preferencesDataStore(name = "tool_favorites")

// Claves para un máximo de 100 herramientas favoritas (ordenadas)
val FAVORITOS_KEYS = (1..100).map { stringPreferencesKey("favorita_$it") }

// ---- Lectura en ORDEN (Flow<List<String>>)
fun Context.flujoToolsFavoritas(): Flow<List<String>> =
    favoritesDataStore.data.map { prefs -> FAVORITOS_KEYS.mapNotNull { prefs[it] } }

// ---- Helper: escribir lista ordenada en las claves favoritas_#
private fun writeFavoritasOrdenadas(prefs: MutablePreferences, orden: List<String>) {
    FAVORITOS_KEYS.forEachIndexed { index, key ->
        if (index < orden.size) {
            prefs[key] = orden[index]
        } else {
            prefs.remove(key)
        }
    }
}

// ---- Alternar favorito (agrega al final si no está; quita si está) [ATÓMICO]
suspend fun Context.toogleFavorite(route: String) {
    favoritesDataStore.edit { prefs ->
        val actuales = FAVORITOS_KEYS.mapNotNull { prefs[it] }.toMutableList()
        if (actuales.contains(route)) {
            actuales.remove(route)
        } else {
            actuales.add(route) // se agrega al final para mantener orden de agregado
        }
        writeFavoritasOrdenadas(prefs, actuales)
    }
}

// ---- Reordenar por índices (from -> to) [ATÓMICO]
suspend fun Context.reorderFavorite(from: Int, to: Int) {
    favoritesDataStore.edit { prefs ->
        val actuales = FAVORITOS_KEYS.mapNotNull { prefs[it] }.toMutableList()
        if (from in actuales.indices && to in actuales.indices && from != to) {
            val item = actuales.removeAt(from)
            actuales.add(to, item)
            writeFavoritasOrdenadas(prefs, actuales)
        }
    }
}

// ---- Establecer orden absoluto (lista completa de rutas) [ATÓMICO]
suspend fun Context.setFavoritesOrder(newOrder: List<String>) {
    favoritesDataStore.edit { prefs ->
        val orden = newOrder.distinct().take(FAVORITOS_KEYS.size)
        writeFavoritasOrdenadas(prefs, orden)
    }
}
