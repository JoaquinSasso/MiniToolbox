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

object PomodoroTimersPrefs {
    private const val PREFS = "pomodoro_timers_prefs"
    private const val KEY = "timers"

    fun loadAll(context: Context): List<PomodoroTimerConfig> {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = sp.getString(KEY, "[]") ?: "[]"
        val arr = try { JSONArray(raw) } catch (_: Exception) { JSONArray("[]") }

        fun ensureOpaque(intColor: Int): Int {
            // Si no hay alpha (0x00------), forzamos 0xFF------
            return if ((intColor ushr 24) == 0) intColor or 0xFF000000.toInt() else intColor
        }

        val list = mutableListOf<PomodoroTimerConfig>()
        var needsMigration = false

        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue

            // 1) Intentar leer en este orden: "color" (Int) → "colorInt" (Int) → "colorArgb" (Long)
            val colorFromInt = if (o.has("color")) o.optInt("color")
            else if (o.has("colorInt")) o.optInt("colorInt")
            else 0

            val colorFromLong = if (o.has("colorArgb")) {
                // ¡Ojo! era Long antes: convertir y conservar bits
                (o.optLong("colorArgb").toInt())
            } else 0

            var colorInt = when {
                colorFromInt != 0 -> colorFromInt
                colorFromLong != 0 -> colorFromLong
                else -> 0xFF4DBC52.toInt() // fallback visible
            }

            val before = colorInt
            colorInt = ensureOpaque(colorInt)
            if (before != colorInt || (colorFromInt == 0 && colorFromLong.toLong() != 0L)) {
                needsMigration = true
            }

            list += PomodoroTimerConfig(
                id = o.optString("id"),
                name = o.optString("name"),
                colorInt = colorInt,
                workMin = o.optInt("workMin", 25),
                shortBreakMin = o.optInt("shortBreakMin", 5),
                longBreakMin = o.optInt("longBreakMin", 15),
                cyclesBeforeLong = o.optInt("cyclesBeforeLong", 4),
            )
        }

        // 2) Si migramos algún color, guardamos de vuelta con el key nuevo y alpha forzada
        if (needsMigration) {
            saveAll(context, list)
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
                put("color", t.colorInt)                     // ← guardar Int ARGB
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
