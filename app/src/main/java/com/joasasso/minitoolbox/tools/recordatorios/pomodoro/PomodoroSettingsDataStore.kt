package com.joasasso.minitoolbox.tools.recordatorios.pomodoro

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.pomodoroSettingsStore by preferencesDataStore("pomodoro_settings")

object PomodoroSettingsKeys {
    val WORK_MINUTES = intPreferencesKey("work_minutes")
    val SHORT_BREAK  = intPreferencesKey("short_break_minutes")
    val LONG_BREAK   = intPreferencesKey("long_break_minutes")
}

class PomodoroSettingsRepository(context: Context) {
    private val ds = context.pomodoroSettingsStore

    val workMinFlow: Flow<Int> = ds.data.map { prefs ->
        prefs[PomodoroSettingsKeys.WORK_MINUTES] ?: 25
    }
    val shortBreakFlow: Flow<Int> = ds.data.map { prefs ->
        prefs[PomodoroSettingsKeys.SHORT_BREAK] ?: 5
    }
    val longBreakFlow: Flow<Int> = ds.data.map { prefs ->
        prefs[PomodoroSettingsKeys.LONG_BREAK] ?: 15
    }

    suspend fun updateWorkMin(value: Int)  = ds.edit { it[PomodoroSettingsKeys.WORK_MINUTES] = value }
    suspend fun updateShortBreak(v: Int)   = ds.edit { it[PomodoroSettingsKeys.SHORT_BREAK]  = v }
    suspend fun updateLongBreak(v: Int)    = ds.edit { it[PomodoroSettingsKeys.LONG_BREAK]   = v }
}
