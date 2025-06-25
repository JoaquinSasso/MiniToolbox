package com.example.minitoolbox.tools.juegos

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.scoreDataStore by preferencesDataStore(name = "truco_score")

object ScoreKeys {
    val OUR_POINTS = intPreferencesKey("our_points")
    val THEIR_POINTS = intPreferencesKey("their_points")
}

class ScoreRepository(private val context: Context) {
    val ourPointsFlow: Flow<Int> = context.scoreDataStore.data.map {
        it[ScoreKeys.OUR_POINTS] ?: 0
    }

    val theirPointsFlow: Flow<Int> = context.scoreDataStore.data.map {
        it[ScoreKeys.THEIR_POINTS] ?: 0
    }

    suspend fun savePoints(our: Int, their: Int) {
        context.scoreDataStore.edit { prefs ->
            prefs[ScoreKeys.OUR_POINTS] = our
            prefs[ScoreKeys.THEIR_POINTS] = their
        }
    }
}
