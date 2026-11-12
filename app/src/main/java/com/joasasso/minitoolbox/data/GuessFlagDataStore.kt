package com.joasasso.minitoolbox.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// El nombre del archivo y la clave no cambian: los récords guardados se conservan.
internal val Context.flagGameDataStore by preferencesDataStore("flag_game_prefs")
private val BEST_SCORE = intPreferencesKey("best_score")

interface FlagGameRepository {
    fun bestScore(context: Context): Flow<Int>

    /** Guarda [score] solo si supera el récord guardado. */
    suspend fun recordScore(context: Context, score: Int)
}

object DefaultFlagGameRepository : FlagGameRepository {
    override fun bestScore(context: Context): Flow<Int> =
        context.flagGameDataStore.data.map { prefs -> prefs[BEST_SCORE] ?: 0 }

    override suspend fun recordScore(context: Context, score: Int) {
        context.flagGameDataStore.edit { prefs ->
            if (score > (prefs[BEST_SCORE] ?: 0)) prefs[BEST_SCORE] = score
        }
    }
}
