package com.joasasso.minitoolbox.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.joasasso.minitoolbox.tools.organizacion.pomodoro.PomodoroTimerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

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

private val Context.pomodoroSettingsStore by preferencesDataStore("pomodoro_settings")

object PomodoroSettingsKeys {
    val WORK_MINUTES = intPreferencesKey("work_minutes")
    val SHORT_BREAK  = intPreferencesKey("short_break_minutes")
    val LONG_BREAK   = intPreferencesKey("long_break_minutes")
}

class PomodoroSettingsRepository(context: Context) {
    private val ds = context.pomodoroSettingsStore

    val workMinFlow: Flow<Int> = ds.data.map { prefs ->
        prefs[PomodoroSettingsKeys.WORK_MINUTES] ?: 1
    }
    val shortBreakFlow: Flow<Int> = ds.data.map { prefs ->
        prefs[PomodoroSettingsKeys.SHORT_BREAK] ?: 1
    }
    val longBreakFlow: Flow<Int> = ds.data.map { prefs ->
        prefs[PomodoroSettingsKeys.LONG_BREAK] ?: 1
    }

    suspend fun updateWorkMin(value: Int) =
        ds.edit { it[PomodoroSettingsKeys.WORK_MINUTES] = value }

    suspend fun updateShortBreak(v: Int) = ds.edit { it[PomodoroSettingsKeys.SHORT_BREAK] = v }
    suspend fun updateLongBreak(v: Int) = ds.edit { it[PomodoroSettingsKeys.LONG_BREAK] = v }
}

object PomodoroTimersPrefs {
    private const val PREFS = "pomodoro_timers_prefs"
    private const val KEY = "timers"

    fun loadAll(context: Context): List<PomodoroTimerConfig> {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY, "[]") ?: "[]"
        val arr = try { JSONArray(raw) } catch (_: Exception) {
            JSONArray("[]")
        }
        val list = mutableListOf<PomodoroTimerConfig>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            list += PomodoroTimerConfig(
                id = o.optString("id"),
                name = o.optString("name"),
                colorArgb = o.optLong("colorArgb"),
                workMin = o.optInt("workMin", 25),
                shortBreakMin = o.optInt("shortBreakMin", 5),
                longBreakMin = o.optInt("longBreakMin", 15),
                cyclesBeforeLong = o.optInt("cyclesBeforeLong", 4),
            )
        }
        return list
    }

    fun saveAll(context: Context, items: List<PomodoroTimerConfig>) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = JSONArray()
        items.forEach { t ->
            val o = JSONObject().apply {
                put("id", t.id)
                put("name", t.name)
                put("colorArgb", t.colorArgb)
                put("workMin", t.workMin)
                put("shortBreakMin", t.shortBreakMin)
                put("longBreakMin", t.longBreakMin)
                put("cyclesBeforeLong", t.cyclesBeforeLong)
            }
            arr.put(o)
        }
        sp.edit().putString(KEY, arr.toString()).apply()
    }
}
