package com.joasasso.minitoolbox.utils.pro

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "calculos_rapidos")

object ProRepository {
    private val IS_PRO = booleanPreferencesKey("is_pro")

    fun isProFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[IS_PRO] ?: false
        }

    suspend fun setProStatus(context: Context, isPro: Boolean) {
        context.dataStore.edit { settings ->
            settings[IS_PRO] = isPro
        }
    }
}