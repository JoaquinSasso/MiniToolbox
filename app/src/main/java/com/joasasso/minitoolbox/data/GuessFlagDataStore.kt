package com.joasasso.minitoolbox.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("flag_game_prefs")

object FlagGameDataStore {
    private val BEST_SCORE = intPreferencesKey("best_score")

    fun getBestScore(context: Context): Flow<Int> {
        return context.dataStore.data.map { prefs -> prefs[BEST_SCORE] ?: 0 }
    }

    suspend fun setBestScore(context: Context, newScore: Int) {
        context.dataStore.edit { prefs -> prefs[BEST_SCORE] = newScore }
    }
}
