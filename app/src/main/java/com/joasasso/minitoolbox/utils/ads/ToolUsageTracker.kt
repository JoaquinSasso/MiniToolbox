// ToolUsageTracker.kt
package com.joasasso.minitoolbox.utils.ads

import android.content.Context
import androidx.core.content.edit

object ToolUsageTracker {
    private const val PREFS = "tool_usage_prefs"
    private const val KEY_GLOBAL_COUNT = "global_tool_open_count"
    private const val KEY_LAST_OPEN_PREFIX = "last_open_" // + toolId

    // Cambiá si querés 45s, 60s, etc.
    private const val TOOL_COOLDOWN_MS = 30_000L

    /**
     * Registra la apertura de una tool con cooldown por herramienta.
     * @return true si se considera un "nuevo acceso" (se incrementa el contador global).
     */
    fun onToolOpened(context: Context, toolId: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = sp.getLong(KEY_LAST_OPEN_PREFIX + toolId, 0L)
        val isNewAccess = nowMs - last > TOOL_COOLDOWN_MS
        if (isNewAccess) {
            val newCount = sp.getInt(KEY_GLOBAL_COUNT, 0) + 1
            sp.edit {
                putLong(KEY_LAST_OPEN_PREFIX + toolId, nowMs)
                putInt(KEY_GLOBAL_COUNT, newCount)
            }
        } else {
            // Igual actualizamos el last open para evitar loops de back/forward muy rápidos
            sp.edit { putLong(KEY_LAST_OPEN_PREFIX + toolId, nowMs) }
        }
        return isNewAccess
    }

    fun getGlobalOpenCount(context: Context): Int {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return sp.getInt(KEY_GLOBAL_COUNT, 0)
    }
}
