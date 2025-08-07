package com.joasasso.minitoolbox.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.BMIdataStore by preferencesDataStore(name = "imc_preferences")

object BMIDataStore {
    private val USE_IMPERIAL_KEY = booleanPreferencesKey("use_imperial")

    fun getUseImperial(context: Context): Flow<Boolean> {
        return context.BMIdataStore.data.map { preferences ->
            preferences[USE_IMPERIAL_KEY] ?: false
        }
    }

    suspend fun setUseImperial(context: Context, value: Boolean) {
        context.BMIdataStore.edit { preferences ->
            preferences[USE_IMPERIAL_KEY] = value
        }
    }
}
