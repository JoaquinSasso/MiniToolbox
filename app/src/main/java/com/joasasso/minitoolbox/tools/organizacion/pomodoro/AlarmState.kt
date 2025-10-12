// AlarmState.kt
package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.content.Context
import androidx.core.content.edit

object AlarmState {
    private const val PREFS = "pomodoro_alarm_state"
    private const val KEY_ACTIVE = "active"

    fun setActive(context: Context, active: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit(commit = true) { putBoolean(KEY_ACTIVE, active) }
    }

    fun isActive(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ACTIVE, false)
}
