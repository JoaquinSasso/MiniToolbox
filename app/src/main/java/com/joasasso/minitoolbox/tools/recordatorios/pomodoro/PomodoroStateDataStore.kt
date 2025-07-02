package com.joasasso.minitoolbox.tools.recordatorios.pomodoro

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.pomodoroStateStore by preferencesDataStore("pomodoro_state")

object PomodoroStateKeys {
    val PHASE_NAME  = stringPreferencesKey("phase_name")
    val PHASE_END   = longPreferencesKey("phase_end_ms")
    val PHASE_TOTAL = longPreferencesKey("phase_total_s")
}

class PomodoroStateRepository(context: Context) {
    private val ds = context.pomodoroStateStore

    val phaseNameFlow: Flow<String> = ds.data.map { it[PomodoroStateKeys.PHASE_NAME]  ?: "" }
    val phaseEndFlow:  Flow<Long>   = ds.data.map { it[PomodoroStateKeys.PHASE_END]   ?: 0L }
    val phaseTotalFlow:Flow<Long>   = ds.data.map { it[PomodoroStateKeys.PHASE_TOTAL] ?: 0L }

    suspend fun updatePhase(name: String, endMs: Long, totalSec: Long) {
        ds.edit { prefs ->
            prefs[PomodoroStateKeys.PHASE_NAME]  = name
            prefs[PomodoroStateKeys.PHASE_END]   = endMs
            prefs[PomodoroStateKeys.PHASE_TOTAL] = totalSec
        }
    }
    suspend fun clearPhase() {
        ds.edit { it.clear() }
    }
}
