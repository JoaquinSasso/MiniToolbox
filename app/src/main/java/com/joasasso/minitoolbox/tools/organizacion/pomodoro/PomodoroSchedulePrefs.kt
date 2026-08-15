package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.content.Context
import android.util.Log
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

    private const val TAG = "PomodoroSchedulePrefs"
    private const val PREFS = "pomodoro_schedule"

    private const val K_ACTIVE  = "active"
    private const val K_TRIGGER = "trigger_at"
    private const val K_PHASE   = "phase"
    private const val K_CYCLE   = "cycle"
    private const val K_ID      = "cfg_id"
    private const val K_COLOR   = "cfg_color"
    private const val K_WORK    = "cfg_work"
    private const val K_SHORT   = "cfg_short"
    private const val K_LONG    = "cfg_long"
    private const val K_CBL     = "cfg_cbl"
    private const val K_CLAIMED = "claimed_trigger"

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

    /**
     * Reclama el procesamiento del vencimiento de fase en [triggerAtMs].
     *
     * Hay dos caminos que pueden llegar a "esta fase venció, hay que sonar y
     * avanzar": el BroadcastReceiver real (disparado por AlarmManager) y el
     * watchdog de la UI (forceAdvanceFromUi, que corre mientras el proceso siga
     * vivo, sin importar si la pantalla está apagada). Ambos vigilan el mismo
     * reloj, así que pueden despertar casi al mismo instante para el mismo
     * [triggerAtMs]. Sin este chequeo, los dos procesan la fase: suenan dos
     * alarmas superpuestas y se programan dos veces la fase siguiente.
     *
     * @return true si quien llama es el primero en reclamar este trigger (debe
     *         proceder); false si ya fue reclamado por el otro camino (debe
     *         abortar sin hacer nada más).
     *
     * synchronized: los dos caminos corren en el mismo proceso (uno en el hilo
     * principal, el otro en una corrutina de IO), así que un lock de Kotlin
     * alcanza para que el chequeo-y-marcado sea atómico entre ellos.
     */
    private val claimLock = Any()

    fun claimTrigger(context: Context, triggerAtMs: Long): Boolean = synchronized(claimLock) {
        val sp = prefs(context)
        val already = sp.getLong(K_CLAIMED, -1L)
        if (already == triggerAtMs) {
            Log.d(TAG, "claimTrigger($triggerAtMs): ya estaba reclamado, se descarta")
            return@synchronized false
        }
        sp.edit(commit = true) { putLong(K_CLAIMED, triggerAtMs) }
        Log.d(TAG, "claimTrigger($triggerAtMs): reclamado con éxito")
        true
    }
}