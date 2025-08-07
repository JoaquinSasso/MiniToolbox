package com.joasasso.minitoolbox.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "favoritos")

object FavoritosManager {
    private val FAVORITOS_KEY = stringSetPreferencesKey("tools_favoritas")

    fun getFavoritosFlow(context: Context): Flow<Set<String>> {
        return context.dataStore.data.map { it[FAVORITOS_KEY] ?: emptySet() }
    }

    suspend fun toggleFavorito(context: Context, route: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITOS_KEY] ?: emptySet()
            prefs[FAVORITOS_KEY] = if (current.contains(route)) {
                current - route
            } else {
                current + route
            }
        }
    }
}
