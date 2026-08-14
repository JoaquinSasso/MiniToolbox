package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.PomodoroStateRepository
import com.joasasso.minitoolbox.nav.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Acción de la alarma
const val ACTION_FIRE_ALARM = "POMODORO_FIRE_ALARM"

// Estas dos vivían en PomodoroService.kt (que se elimina). Las usa PomodoroActionReceiver.
const val ACTION_STOP    = "STOP_POMODORO"
const val ACTION_SILENCE = "SILENCE_ALARM"

private const val REQ_ALARM = 1001
private const val TAG = "PomodoroAlarm"

internal const val PHASE_WORK  = "WORK"
internal const val PHASE_SHORT = "SHORT"
internal const val PHASE_LONG  = "LONG"

/**
 * Único extra que viaja en el PendingIntent: el instante para el que se programó.
 * Sirve para descartar disparos duplicados o de alarmas ya canceladas.
 * El resto del estado vive en PomodoroSchedulePrefs.
 */
private const val EX_TRIGGER = "ex_trigger"

class PomodoroAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext

        when (intent.action) {
            ACTION_POMODORO_ALARM_SILENCE -> { silenceAlarm(app); return }
            ACTION_FIRE_ALARM -> Unit
            else -> return
        }

        val trigger = intent.getLongExtra(EX_TRIGGER, 0L)
        val pending = PomodoroSchedulePrefs.load(app)

        // Idempotencia: si el pomodoro se paró, o esta alarma ya se consumió,
        // el trigger guardado no coincide y no hacemos nada.
        if (pending == null || pending.triggerAtMs != trigger) {
            Log.d(TAG, "Disparo descartado (trigger=$trigger, guardado=${pending?.triggerAtMs})")
            return
        }

        val wl = app.getSystemService(PowerManager::class.java)
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MiniToolbox:PomodoroAlarm")
        try { wl?.acquire(20_000L) } catch (_: Exception) { }

        // 1) Sonar YA, sincrónicamente. Venimos de una alarma exacta, así que
        //    estamos dentro de la exención que permite arrancar un FGS desde background.
        startRinging(app, pending)

        // 2) Encadenar la fase siguiente (IO por el DataStore del repo)
        val result = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                advanceToNextPhase(app, pending)
            } catch (t: Throwable) {
                Log.e(TAG, "Error al encadenar la fase siguiente", t)
            } finally {
                try { if (wl?.isHeld == true) wl.release() } catch (_: Exception) { }
                result.finish()
            }
        }
    }

    companion object {

        // ---------------------------------------------------------------------
        // API pública (la que usa la UI)
        // ---------------------------------------------------------------------

        fun startPomodoro(context: Context, config: PomodoroTimerConfig) {
            val app = context.applicationContext
            val endMs = System.currentTimeMillis() + config.workMin * 60_000L
            val route = Screen.PomodoroDetail.createRoute(config.id)

            CoroutineScope(Dispatchers.IO).launch {
                PomodoroStateRepository(app).updatePhase(
                    app.getString(R.string.pomodoro_work), endMs, config.workMin * 60L
                )
            }

            showRunningNotification(app, app.getString(R.string.pomodoro_work), endMs, route)
            schedule(app, PomodoroSchedulePrefs.Pending(endMs, PHASE_WORK, 0, config))
        }

        fun stopPomodoro(context: Context) {
            val app = context.applicationContext
            cancelAlarm(app)
            PomodoroSchedulePrefs.clear(app)
            cancelRunningNotification(app)
            PomodoroAlarmService.stop(app)
            ContextCompat.getSystemService(app, NotificationManager::class.java)
                ?.cancel(NOTIF_ID_ALARM_SILENT)
            AlarmState.setActive(app, false)
            CoroutineScope(Dispatchers.IO).launch {
                PomodoroStateRepository(app).clearPhase()
            }
        }

        fun silenceAlarm(context: Context) {
            val app = context.applicationContext
            PomodoroAlarmService.stop(app)
            // Por si sonó por el plan B (notificación con sonido de canal, sin servicio)
            ContextCompat.getSystemService(app, NotificationManager::class.java)
                ?.cancel(NOTIF_ID_ALARM_SILENT)
            AlarmState.setActive(app, false)
            app.sendBroadcast(
                Intent(ACTION_POMODORO_ALARM_STOP)
                    .setPackage(app.packageName)
                    .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY or Intent.FLAG_RECEIVER_FOREGROUND)
            )
        }

        /**
         * Recuperación: reprograma la alarma guardada.
         * La llama PomodoroBootReceiver tras un reboot, un cambio de hora, o
         * cuando el usuario concede el permiso de alarmas exactas.
         */
        fun rescheduleFromPersisted(context: Context) {
            val app = context.applicationContext
            val pending = PomodoroSchedulePrefs.load(app) ?: return
            val now = System.currentTimeMillis()

            if (pending.triggerAtMs > now) {
                schedule(app, pending)
                showRunningNotification(
                    app,
                    phaseTitle(app, pending.phase),
                    pending.triggerAtMs,
                    Screen.PomodoroDetail.createRoute(pending.config.id)
                )
            } else {
                // La fase venció mientras el teléfono estaba apagado. No se puede
                // arrancar un FGS de tipo mediaPlayback desde BOOT_COMPLETED, así
                // que sólo dejamos el aviso y cortamos la cadena.
                cancelRunningNotification(app)
                showAlarmNotification(
                    app,
                    app.getString(R.string.pomodoro_finished, phaseTitle(app, pending.phase)),
                    app.getString(R.string.pomodoro_tap_to_stop),
                    Screen.PomodoroDetail.createRoute(pending.config.id)
                )
                PomodoroSchedulePrefs.clear(app)
                CoroutineScope(Dispatchers.IO).launch {
                    PomodoroStateRepository(app).clearPhase()
                }
            }
        }

        /**
         * Red de seguridad desde la UI: si el usuario abre la app y la fase ya
         * venció pero la alarma nunca llegó (permiso denegado, OEM agresivo),
         * avanzamos igual. Con alarmas exactas esto casi nunca debería dispararse.
         */
        suspend fun forceAdvanceFromUi(
            context: Context,
            currentPhaseName: String,
            config: PomodoroTimerConfig
        ) {
            val app = context.applicationContext
            val pending = PomodoroSchedulePrefs.load(app)
                ?: PomodoroSchedulePrefs.Pending(
                    triggerAtMs = System.currentTimeMillis(),
                    phase = phaseKeyOf(app, currentPhaseName),
                    cycle = 0,
                    config = config
                )
            if (pending.triggerAtMs > System.currentTimeMillis()) return

            cancelAlarm(app)
            startRinging(app, pending)          // la app está en foreground: FGS permitido
            advanceToNextPhase(app, pending)
        }

        // ---------------------------------------------------------------------
        // Interno
        // ---------------------------------------------------------------------

        /**
         * Programa la alarma y persiste el estado.
         *
         * Camino principal: setAlarmClock(). Es el único que el sistema saca de
         * Doze de forma garantizada (sale de Doze poco antes del disparo) y el
         * único que NO tiene el límite de "un disparo cada 9 minutos por app" que
         * afecta a setAndAllowWhileIdle / setExactAndAllowWhileIdle. Con descansos
         * cortos de 5 minutos ese límite importa.
         *
         * Requiere permiso de alarmas exactas. Si no lo tenemos, degradamos a
         * inexacta y lo dejamos anotado en el log: llegará tarde, pero al menos
         * no se pierde silenciosamente como antes.
         */
        internal fun schedule(context: Context, pending: PomodoroSchedulePrefs.Pending) {
            val app = context.applicationContext
            PomodoroSchedulePrefs.save(app, pending)   // persistir primero

            val am = app.getSystemService(AlarmManager::class.java) ?: return
            val pi = firePendingIntent(app, pending.triggerAtMs)

            if (ExactAlarmPermission.canSchedule(app)) {
                try {
                    val show = mainPendingIntent(app, Screen.PomodoroDetail.createRoute(pending.config.id))
                    am.setAlarmClock(AlarmManager.AlarmClockInfo(pending.triggerAtMs, show), pi)
                    return
                } catch (e: SecurityException) {
                    // Carrera: el usuario revocó el permiso entre el chequeo y el uso
                    Log.w(TAG, "setAlarmClock rechazado, degradando a inexacta", e)
                }
            } else {
                Log.w(TAG, "Sin permiso de alarmas exactas: la alarma puede llegar tarde")
            }

            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, pending.triggerAtMs, pi)
        }

        private fun firePendingIntent(context: Context, triggerAtMs: Long): PendingIntent {
            val i = Intent(context, PomodoroAlarmReceiver::class.java).apply {
                action = ACTION_FIRE_ALARM
                putExtra(EX_TRIGGER, triggerAtMs)
            }
            return PendingIntent.getBroadcast(
                context, REQ_ALARM, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun cancelAlarm(context: Context) {
            val am = context.getSystemService(AlarmManager::class.java)
            val i = Intent(context, PomodoroAlarmReceiver::class.java).apply {
                action = ACTION_FIRE_ALARM
            }
            val pi = PendingIntent.getBroadcast(
                context, REQ_ALARM, i,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )
            if (pi != null) {
                am?.cancel(pi)
                pi.cancel()
            }
        }

        /** Arranca el servicio que suena. Nunca lanza: si falla, plan B con sonido de canal. */
        internal fun startRinging(context: Context, pending: PomodoroSchedulePrefs.Pending) {
            val app = context.applicationContext
            val title = app.getString(R.string.pomodoro_finished, phaseTitle(app, pending.phase))
            val text  = app.getString(R.string.pomodoro_tap_to_stop)
            val route = Screen.PomodoroDetail.createRoute(pending.config.id)

            // Android 17 / targetSdk 37: un FGS arrancado desde background NO tiene
            // capacidades while-in-use, así que nuestro MediaPlayer sólo puede sonar
            // gracias a la exención "permiso de alarma exacta + stream USAGE_ALARM".
            // Sin ese permiso el audio falla EN SILENCIO: las APIs no lanzan excepción,
            // simplemente no suena. Por eso ni lo intentamos y vamos directo a la
            // notificación con sonido de canal, que la reproduce el sistema y no está
            // sujeta a esta restricción.
            if (!ExactAlarmPermission.canSchedule(app)) {
                Log.w(TAG, "Sin permiso de alarmas exactas: sonando por canal de notificación")
                ringWithChannelSound(app, title, text, route)
                return
            }

            try {
                PomodoroAlarmService.ring(app, title, text, route)
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo arrancar el FGS de alarma, usando notificación con sonido", e)
                ringWithChannelSound(app, title, text, route)
            }
        }

        private fun ringWithChannelSound(
            app: Context,
            title: String,
            text: String,
            route: String
        ) {
            AlarmState.setActive(app, true)
            app.sendBroadcast(
                Intent(ACTION_POMODORO_ALARM_START)
                    .setPackage(app.packageName)
                    .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY or Intent.FLAG_RECEIVER_FOREGROUND)
            )
            showAlarmNotification(app, title, text, route, withChannelSound = true)
        }

        private suspend fun advanceToNextPhase(
            context: Context,
            pending: PomodoroSchedulePrefs.Pending
        ) {
            val app = context.applicationContext
            val cfg = pending.config

            val (nextPhase, nextMin, nextCycle) = when (pending.phase) {
                PHASE_WORK -> {
                    val cycle = pending.cycle + 1
                    val isLong = cfg.cyclesBeforeLong > 0 && cycle % cfg.cyclesBeforeLong == 0
                    Triple(
                        if (isLong) PHASE_LONG else PHASE_SHORT,
                        if (isLong) cfg.longBreakMin else cfg.shortBreakMin,
                        cycle
                    )
                }
                else -> Triple(PHASE_WORK, cfg.workMin, pending.cycle)
            }

            val endMs = System.currentTimeMillis() + nextMin * 60_000L
            val title = phaseTitle(app, nextPhase)
            val route = Screen.PomodoroDetail.createRoute(cfg.id)

            PomodoroStateRepository(app).updatePhase(title, endMs, nextMin * 60L)
            showRunningNotification(app, title, endMs, route)
            schedule(app, PomodoroSchedulePrefs.Pending(endMs, nextPhase, nextCycle, cfg))
        }

        private fun phaseTitle(context: Context, phase: String): String = when (phase) {
            PHASE_WORK  -> context.getString(R.string.pomodoro_work)
            PHASE_SHORT -> context.getString(R.string.pomodoro_short_break)
            PHASE_LONG  -> context.getString(R.string.pomodoro_long_break)
            else        -> phase
        }

        private fun phaseKeyOf(context: Context, localizedName: String): String = when (localizedName) {
            context.getString(R.string.pomodoro_short_break) -> PHASE_SHORT
            context.getString(R.string.pomodoro_long_break)  -> PHASE_LONG
            else -> PHASE_WORK
        }
    }
}