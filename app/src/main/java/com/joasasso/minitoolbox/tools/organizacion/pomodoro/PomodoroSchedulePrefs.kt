package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.content.Context
import androidx.core.content.edit

/**
 * Fuente de verdad persistente de la alarma pendiente.
 *
 * Antes toda la información del pomodoro viajaba únicamente dentro de los extras
 * del PendingIntent. Si el sistema perdía esa alarma (reboot, force stop, el
 * usuario revoca "Alarmas y recordatorios") el pomodoro quedaba colgado sin
 * ninguna forma de recuperarse. Guardándolo acá podemos reprogramar siempre.
 *
 * Semántica de [Pending]:
 *  - [Pending.phase]      = la fase que está corriendo AHORA
 *  - [Pending.triggerAtMs]= el instante en que esa fase termina (y suena la alarma)
 *  - [Pending.cycle]      = cuántos bloques de trabajo se completaron hasta ahora
 */
object PomodoroSchedulePrefs {

    private const val PREFS = "pomodoro_schedule"

    private const val K_ACTIVE  = "active"
    private const val K_TRIGGER = "trigger_at"
    private const val K_PHASE   = "phase"
    private const val K_CYCLE   = "cycle"
    private const val K_ID      = "cfg_id"
    private const val K_NAME    = "cfg_name"
    private const val K_COLOR   = "cfg_color"
    private const val K_WORK    = "cfg_work"
    private const val K_SHORT   = "cfg_short"
    private const val K_LONG    = "cfg_long"
    private const val K_CBL     = "cfg_cbl"

    data class Pending(
        val triggerAtMs: Long,
        val phase: String,
        val cycle: Int,
        val config: PomodoroTimerConfig
    )

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** commit = true a propósito: esto se escribe desde un BroadcastReceiver. */
    fun save(context: Context, pending: Pending) {
        prefs(context).edit(commit = true) {
            putBoolean(K_ACTIVE, true)
            putLong(K_TRIGGER, pending.triggerAtMs)
            putString(K_PHASE, pending.phase)
            putInt(K_CYCLE, pending.cycle)
            putString(K_ID, pending.config.id)
            putString(K_NAME, pending.config.name)
            putInt(K_COLOR, pending.config.colorInt)
            putInt(K_WORK, pending.config.workMin)
            putInt(K_SHORT, pending.config.shortBreakMin)
            putInt(K_LONG, pending.config.longBreakMin)
            putInt(K_CBL, pending.config.cyclesBeforeLong)
        }
    }

    fun load(context: Context): Pending? {
        val sp = prefs(context)
        if (!sp.getBoolean(K_ACTIVE, false)) return null
        val phase = sp.getString(K_PHASE, null) ?: return null
        return Pending(
            triggerAtMs = sp.getLong(K_TRIGGER, 0L),
            phase = phase,
            cycle = sp.getInt(K_CYCLE, 0),
            config = PomodoroTimerConfig(
                id = sp.getString(K_ID, "").orEmpty(),
                name = sp.getString(K_NAME, "").orEmpty(),
                colorInt = sp.getInt(K_COLOR, 0xFF4DBC52.toInt()),
                workMin = sp.getInt(K_WORK, 25),
                shortBreakMin = sp.getInt(K_SHORT, 5),
                longBreakMin = sp.getInt(K_LONG, 15),
                cyclesBeforeLong = sp.getInt(K_CBL, 4)
            )
        )
    }

    fun clear(context: Context) {
        prefs(context).edit(commit = true) { clear() }
    }
}
