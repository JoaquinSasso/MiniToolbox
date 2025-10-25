package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.content.getSystemService
import com.joasasso.minitoolbox.MainActivity
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.PomodoroStateRepository
import com.joasasso.minitoolbox.nav.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch


// Alarm action
const val ACTION_FIRE_ALARM = "POMODORO_FIRE_ALARM"

// Request code único para el PendingIntent del Pomodoro activo
private const val REQ_ALARM = 1001

// Fases
private const val PHASE_WORK  = "WORK"
private const val PHASE_SHORT = "SHORT"
private const val PHASE_LONG  = "LONG"

// Extras para propagar la configuración completa entre fases
private const val EX_WORK = "ex_work"
private const val EX_SHORT = "ex_short"
private const val EX_LONG = "ex_long"
private const val EX_CYCLES_BEFORE_LONG = "ex_cbl"
private const val EX_CYCLE = "ex_cycle"
private const val EX_PHASE = "ex_phase"
private const val EX_TIMER_ID = "ex_timer_id"
private const val EX_TIMER_NAME = "ex_timer_name"
private const val EX_TIMER_COLOR = "ex_timer_color"

class PomodoroAlarmReceiver : BroadcastReceiver() {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext

        when (intent.action) {
            ACTION_POMODORO_ALARM_SILENCE -> {
                // Cortar audio + cancelar notificación + avisar UI + limpiar flag persistente
                silenceAlarm(app)
                return
            }
            ACTION_FIRE_ALARM -> { /* sigue abajo */ }
            else -> return
        }

        val pm = app.getSystemService<PowerManager>()
        val wl = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MiniToolbox:PomodoroAlarm")
        // 15s son más que suficientes para: notificación + sonido + persistir + reprogramar
        wl?.acquire(15_000L)

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                // --- 0) Leer config del Intent ---
                val workMin  = intent.getIntExtra(EX_WORK, 25)
                val shortMin = intent.getIntExtra(EX_SHORT, 5)
                val longMin  = intent.getIntExtra(EX_LONG, 15)
                val cbl      = intent.getIntExtra(EX_CYCLES_BEFORE_LONG, 4)
                val phase    = intent.getStringExtra(EX_PHASE) ?: PHASE_WORK
                val cycle    = intent.getIntExtra(EX_CYCLE, 0)
                val timerId  = intent.getStringExtra(EX_TIMER_ID) ?: ""
                val timerNm  = intent.getStringExtra(EX_TIMER_NAME) ?: app.getString(R.string.tool_pomodoro_timer)
                val colorInt = intent.getIntExtra(EX_TIMER_COLOR, 0xFF4DBC52.toInt())
                val startRoute = Screen.PomodoroDetail.createRoute(timerId)

                val repo = PomodoroStateRepository(app)
                val now  = System.currentTimeMillis()

                // --- 1) Idempotencia: si ya hay una fase activa, NO dupliques alarma ---
                val currentEnd = repo.phaseEndFlow.firstOrNull() ?: 0L
                if (currentEnd > now) return@launch

                // --- 2) Notificación fin de fase (HUN silenciosa) + flag + broadcast UI ---
                val finishedTitle = app.getString(R.string.pomodoro_finished, phaseNameLocalized(app, phase))
                showAlarmNotification(
                    context = app,
                    title   = finishedTitle,
                    text    = app.getString(R.string.pomodoro_tap_to_stop),
                    startRoute = startRoute
                )
                AlarmState.setActive(app, true)
                app.sendBroadcast(
                    Intent(ACTION_POMODORO_ALARM_START)
                        .setPackage(app.packageName)
                        .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY or Intent.FLAG_RECEIVER_FOREGROUND)
                )

                // --- 3) Sonido controlado (10s) sin duplicados ---
                AlarmSoundPlayer.play(app, durationMs = 10_000L) {
                    silenceAlarm(app) // al autostop, silencia “oficialmente”
                }

                // --- 4) Calcular próxima fase ---
                val (nextPhase, nextMin, nextCycle) = when (phase) {
                    PHASE_WORK -> {
                        val longNext = ((cycle + 1) % cbl == 0)
                        Triple(if (longNext) PHASE_LONG else PHASE_SHORT,
                            if (longNext) longMin else shortMin,
                            cycle + 1)
                    }
                    else -> Triple(PHASE_WORK, workMin, cycle)
                }

                // --- 5) Persistir próximo estado + notificación “en curso” ---
                val endMs = now + nextMin * 60_000L
                val nextTitle = when (nextPhase) {
                    PHASE_WORK  -> app.getString(R.string.pomodoro_work)
                    PHASE_SHORT -> app.getString(R.string.pomodoro_short_break)
                    PHASE_LONG  -> app.getString(R.string.pomodoro_long_break)
                    else        -> timerNm
                }
                repo.updatePhase(nextTitle, endMs, nextMin * 60L)
                showRunningNotification(app, nextTitle, endMs, startRoute = startRoute)

                // --- 6) Reprogramar respaldo (INEXACTO) con la MISMA config ---
                scheduleExactWithConfig(
                    context = app,
                    triggerAtMs = endMs,
                    phase = nextPhase,
                    cycle = nextCycle,
                    config = PomodoroTimerConfig(
                        id = timerId, name = timerNm, colorInt = colorInt,
                        workMin = workMin, shortBreakMin = shortMin,
                        longBreakMin = longMin, cyclesBeforeLong = cbl
                    )
                )
            } finally {
                try { wl?.release() } catch (_: Exception) {}
                pending.finish()
            }
        }
    }


    private fun phaseNameLocalized(context: Context, phase: String) = when (phase) {
        PHASE_WORK  -> context.getString(R.string.pomodoro_work)
        PHASE_SHORT -> context.getString(R.string.pomodoro_short_break)
        PHASE_LONG  -> context.getString(R.string.pomodoro_long_break)
        else        -> phase
    }

    companion object {

        fun startPomodoro(context: Context, config: PomodoroTimerConfig) {
            val app = context.applicationContext
            val endMs = System.currentTimeMillis() + config.workMin * 60_000L

            CoroutineScope(Dispatchers.IO).launch {
                PomodoroStateRepository(app).updatePhase(
                    app.getString(R.string.pomodoro_work),
                    endMs,
                    config.workMin * 60L
                )
            }

            showRunningNotification(
                app, app.getString(R.string.pomodoro_work), endMs,
                startRoute = Screen.PomodoroDetail.createRoute(config.id)
            )

            scheduleExactWithConfig(
                context = app,
                triggerAtMs = endMs,
                phase = PHASE_WORK,
                cycle = 0,
                config = config
            )
        }

        fun stopPomodoro(context: Context) {
            val app = context.applicationContext
            cancelAlarm(app)
            cancelRunningNotification(app)
            val nm = ContextCompat.getSystemService(app, NotificationManager::class.java)
            nm?.cancel(NOTIF_ID_ALARM_SILENT)
            AlarmSoundPlayer.stop()
            CoroutineScope(Dispatchers.IO).launch {
                PomodoroStateRepository(app).clearPhase()
            }
        }

        fun silenceAlarm(context: Context) {
            val nm = ContextCompat.getSystemService(context, NotificationManager::class.java)
            nm?.cancel(NOTIF_ID_ALARM_SILENT)
            AlarmSoundPlayer.stop()
            AlarmState.setActive(context, false)
            context.sendBroadcast(
                Intent(ACTION_POMODORO_ALARM_STOP)
                    .setPackage(context.packageName)
                    .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY or Intent.FLAG_RECEIVER_FOREGROUND)
            )
        }

        /**
         * Programa la próxima alarma propagando SIEMPRE la configuración completa.
         * Camino principal: AlarmClockInfo (exacta sin permisos).
         * Respaldo: setAndAllowWhileIdle / set (inexactas) si algo falla.
         */
        private fun scheduleExactWithConfig(
            context: Context,
            triggerAtMs: Long,
            phase: String,
            cycle: Int,
            config: PomodoroTimerConfig
        ) {
            val am = context.getSystemService(AlarmManager::class.java) ?: return

            // 1) Intent que dispara el cambio de fase (BroadcastReceiver)
            val alarmIntent = Intent(context, PomodoroAlarmReceiver::class.java).apply {
                action = ACTION_FIRE_ALARM
                putExtra(EX_PHASE, phase)
                putExtra(EX_CYCLE, cycle)
                putExtra(EX_WORK, config.workMin)
                putExtra(EX_SHORT, config.shortBreakMin)
                putExtra(EX_LONG, config.longBreakMin)
                putExtra(EX_CYCLES_BEFORE_LONG, config.cyclesBeforeLong)
                putExtra(EX_TIMER_ID, config.id)
                putExtra(EX_TIMER_NAME, config.name)
                putExtra(EX_TIMER_COLOR, config.colorInt)
            }
            val piAlarm = PendingIntent.getBroadcast(
                context,
                REQ_ALARM,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 2) Intent de “mostrar” (para AlarmClock): abre la pantalla de detalle del timer
            val startRoute = "pomodoro/detail/${config.id}"
            val showIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                putExtra("startRoute", startRoute)
            }
            val piShow = PendingIntent.getActivity(
                context,
                startRoute.hashCode(),
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 3) Programar como “alarma de usuario” (exacta, policy-safe)
            try {
                val info = AlarmManager.AlarmClockInfo(triggerAtMs, piShow)
                am.setAlarmClock(info, piAlarm)
                return
            } catch (_: Exception) {
                // cae a respaldo si algún OEM raro falla
            }

            // 4) Respaldo inexacto (aceptado por políticas)
            try {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, piAlarm)
            } catch (_: Exception) {
                am.set(AlarmManager.RTC_WAKEUP, triggerAtMs, piAlarm)
            }
        }

        fun scheduleFromUiFallback(
            context: Context,
            triggerAtMs: Long,
            phase: String,
            cycle: Int,
            config: PomodoroTimerConfig
        ) {
            val am = context.getSystemService(AlarmManager::class.java) ?: return
            val intent = Intent(context, PomodoroAlarmReceiver::class.java).apply {
                action = ACTION_FIRE_ALARM
                putExtra(EX_PHASE, phase)
                putExtra(EX_CYCLE, cycle)
                putExtra(EX_WORK, config.workMin)
                putExtra(EX_SHORT, config.shortBreakMin)
                putExtra(EX_LONG, config.longBreakMin)
                putExtra(EX_CYCLES_BEFORE_LONG, config.cyclesBeforeLong)
                putExtra(EX_TIMER_ID, config.id)
                putExtra(EX_TIMER_NAME, config.name)
                putExtra(EX_TIMER_COLOR, config.colorInt)
            }
            val pi = PendingIntent.getBroadcast(
                context, REQ_ALARM, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            } catch (_: Exception) {
                am.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            }
        }

        private fun cancelAlarm(context: Context) {
            val am = context.getSystemService(AlarmManager::class.java)
            val i = Intent(context, PomodoroAlarmReceiver::class.java).apply { action = ACTION_FIRE_ALARM }
            val pi = PendingIntent.getBroadcast(
                context, REQ_ALARM, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )
            if (pi != null) am?.cancel(pi)
        }

        suspend fun forceAdvanceFromUi(
            context: Context,
            currentPhaseName: String,
            config: PomodoroTimerConfig
        ) {
            val app = context.applicationContext
            val repo = PomodoroStateRepository(app)
            val now = System.currentTimeMillis()

            val currentEnd = repo.phaseEndFlow.firstOrNull() ?: 0L
            if (currentEnd > now) return

            val finishedTitle = app.getString(R.string.pomodoro_finished, currentPhaseName)
            val startRoute = "pomodoro/detail/${config.id}"
            showAlarmNotification(
                app,
                title = finishedTitle,
                text  = app.getString(R.string.pomodoro_tap_to_stop),
                startRoute = startRoute
            )
            AlarmState.setActive(context, true)
            app.sendBroadcast(
                Intent(ACTION_POMODORO_ALARM_START)
                    .setPackage(app.packageName)
                    .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY or Intent.FLAG_RECEIVER_FOREGROUND)
            )
            AlarmSoundPlayer.play(app, durationMs = 10_000L) {
                silenceAlarm(app)
            }

            val phase = when (currentPhaseName) {
                app.getString(R.string.pomodoro_work)        -> PHASE_WORK
                app.getString(R.string.pomodoro_short_break) -> PHASE_SHORT
                app.getString(R.string.pomodoro_long_break)  -> PHASE_LONG
                else                                         -> PHASE_WORK
            }

            val sp  = app.getSharedPreferences("pomodoro_cycle", Context.MODE_PRIVATE)
            val key = "cycle_${config.id}"
            var cycle = sp.getInt(key, 0)

            val (nextPhase, nextMin, nextCycle) = when (phase) {
                PHASE_WORK -> {
                    val longNext = ((cycle + 1) % config.cyclesBeforeLong == 0)
                    Triple(
                        if (longNext) PHASE_LONG else PHASE_SHORT,
                        if (longNext) config.longBreakMin else config.shortBreakMin,
                        cycle + 1
                    )
                }
                else -> Triple(PHASE_WORK, config.workMin, cycle)
            }
            if (phase == PHASE_WORK) sp.edit { putInt(key, nextCycle) }

            val endMs = now + nextMin * 60_000L
            val nextTitle = when (nextPhase) {
                PHASE_WORK  -> app.getString(R.string.pomodoro_work)
                PHASE_SHORT -> app.getString(R.string.pomodoro_short_break)
                PHASE_LONG  -> app.getString(R.string.pomodoro_long_break)
                else        -> currentPhaseName
            }
            repo.updatePhase(nextTitle, endMs, nextMin * 60L)

            showRunningNotification(app, nextTitle, endMs, startRoute = startRoute)

            scheduleFromUiFallback(
                context = app,
                triggerAtMs = endMs,
                phase = nextPhase,
                cycle = nextCycle,
                config = config
            )
        }
    }
}
