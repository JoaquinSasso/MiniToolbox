package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.PomodoroStateRepository
import com.joasasso.minitoolbox.nav.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import androidx.core.content.edit

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
        if (intent.action != ACTION_FIRE_ALARM) return

        val appContext = context.applicationContext

        // 0) Leer configuración desde el Intent (con fallbacks)
        val workMin  = intent.getIntExtra(EX_WORK, 25)
        val shortMin = intent.getIntExtra(EX_SHORT, 5)
        val longMin  = intent.getIntExtra(EX_LONG, 15)
        val cbl      = intent.getIntExtra(EX_CYCLES_BEFORE_LONG, 4)
        val phase    = intent.getStringExtra(EX_PHASE) ?: PHASE_WORK
        val cycle    = intent.getIntExtra(EX_CYCLE, 0)

        val timerId    = intent.getStringExtra(EX_TIMER_ID) ?: ""
        val timerName  = intent.getStringExtra(EX_TIMER_NAME) ?: appContext.getString(R.string.tool_pomodoro_timer)
        val timerColorInt = intent.getIntExtra(EX_TIMER_COLOR, 0xFF4DBC52.toInt())

        val startRoute = Screen.PomodoroDetail.createRoute(timerId)

        // Trabajo asíncrono seguro en Receiver
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val repo = PomodoroStateRepository(context)
                val now = System.currentTimeMillis()
                val currentEnd = repo.phaseEndFlow.firstOrNull() ?: 0L
                if (currentEnd > now) return@launch
                // 1) Mostrar notificación de fin de fase (canal sin sonido) + avisar a la UI (START)
                val finishedTitle = appContext.getString(
                    R.string.pomodoro_finished,
                    phaseNameLocalized(appContext, phase)
                )
                showAlarmNotification(
                    appContext,
                    finishedTitle,
                    appContext.getString(R.string.pomodoro_tap_to_stop),
                    startRoute = startRoute
                )

                AlarmState.setActive(context, true)
                appContext.sendBroadcast(
                    Intent(ACTION_POMODORO_ALARM_START)
                        .setPackage(appContext.packageName)
                        .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY or Intent.FLAG_RECEIVER_FOREGROUND)
                )

                // 1.1) Reproducir sonido controlado (auto-stop) y, al cortar, notificar STOP
                AlarmSoundPlayer.play(appContext, durationMs = 10_000L) {
                    // callback de auto-silencio: cancela notificación + emite STOP
                    silenceAlarm(appContext)
                }

                // 2) Calcular próxima fase respetando cyclesBeforeLong (cbl)
                val (nextPhase, nextMin, nextCycle) = when (phase) {
                    PHASE_WORK -> {
                        val longNext = ((cycle + 1) % cbl == 0)
                        Triple(
                            if (longNext) PHASE_LONG else PHASE_SHORT,
                            if (longNext) longMin else shortMin,
                            cycle + 1
                        )
                    }
                    else -> Triple(PHASE_WORK, workMin, cycle)
                }

                // 3) Persistir estado de la próxima fase
                val endMs = System.currentTimeMillis() + nextMin * 60_000L
                PomodoroStateRepository(appContext).updatePhase(
                    phaseNameLocalized(appContext, nextPhase),
                    endMs,
                    nextMin * 60L
                )

                // 4) Notificación "en curso" de la nueva fase (sin sonido)
                showRunningNotification(
                    appContext,
                    phaseNameLocalized(appContext, nextPhase),
                    endMs,
                    startRoute = startRoute
                )

                // 5) Agendar la próxima alarma exacta con la MISMA configuración
                val cfg = PomodoroTimerConfig(
                    id = timerId,
                    name = timerName,
                    colorInt = timerColorInt,               // ← Int
                    workMin = workMin, shortBreakMin = shortMin,
                    longBreakMin = longMin, cyclesBeforeLong = cbl
                )

                scheduleExactWithConfig(
                    context = appContext,
                    triggerAtMs = endMs,
                    phase = nextPhase,
                    cycle = nextCycle,
                    config = cfg
                )
            } finally {
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

        /**
         * Start con configuración completa del timer seleccionado.
         */
        fun startPomodoro(context: Context, config: PomodoroTimerConfig) {
            val app = context.applicationContext
            val endMs = System.currentTimeMillis() + config.workMin * 60_000L

            // Persistir primera fase (WORK)
            CoroutineScope(Dispatchers.IO).launch {
                PomodoroStateRepository(app).updatePhase(
                    app.getString(R.string.pomodoro_work),
                    endMs,
                    config.workMin * 60L
                )
            }

            // Notificación "en curso"
            showRunningNotification(
                app, app.getString(R.string.pomodoro_work), endMs,
                startRoute = Screen.PomodoroDetail.createRoute(config.id)
            )

            // Programar primera alarma con la config completa
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
            // Cancelar notificación "running"
            cancelRunningNotification(app)
            // Cancelar notificación de alarma (si estuviera visible)
            val nm = ContextCompat.getSystemService(app, NotificationManager::class.java)
            nm?.cancel(NOTIF_ID_ALARM_SILENT)

            // Detener audio por si estuviera sonando
            AlarmSoundPlayer.stop()

            CoroutineScope(Dispatchers.IO).launch {
                PomodoroStateRepository(app).clearPhase()
            }
        }

        /**
         * Silenciar alarma actual: cancela notificación de alarma, detiene audio y emite STOP.
         */
        fun silenceAlarm(context: Context) {
            val nm = ContextCompat.getSystemService(context, NotificationManager::class.java)
            nm?.cancel(NOTIF_ID_ALARM_SILENT)

            // detener audio por si estaba sonando
            AlarmSoundPlayer.stop()

            // avisar a la UI
            AlarmState.setActive(context, false)
            context.sendBroadcast(
                Intent(ACTION_POMODORO_ALARM_STOP)
                    .setPackage(context.packageName)
                    .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY or Intent.FLAG_RECEIVER_FOREGROUND)
            )
        }

        /**
         * Programa la próxima alarma propagando SIEMPRE la configuración completa.
         * Usa solo alarmas inexactas (permitidas por políticas de Google Play).
         */
        private fun scheduleExactWithConfig(
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

            // Un único PendingIntent para el Pomodoro activo
            val pi = PendingIntent.getBroadcast(
                context,
                REQ_ALARM,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Solo alarmas inexactas: cumplen políticas y son suficientes para Pomodoro
            try {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            } catch (_: Exception) {
                // Fallback básico si algún fabricante falla
                am.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
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

        /**
         * Avanza la fase inmediatamente desde la UI, sin depender del broadcast del AlarmManager.
         * - Dispara notificación + sonido de fin de fase de inmediato (como hacía el Receiver).
         * - Idempotente: si ya hay una fase activa (endMs > now), no duplica nada.
         * - Reprograma respaldo con alarmas inexactas (policy-safe).
         */
        suspend fun forceAdvanceFromUi(
            context: Context,
            currentPhaseName: String,
            config: PomodoroTimerConfig
        ) {
            val app = context.applicationContext
            val repo = PomodoroStateRepository(app)
            val now = System.currentTimeMillis()

            // Si ya hay una fase activa con end en el futuro, no hacer nada
            val currentEnd = repo.phaseEndFlow.firstOrNull() ?: 0L
            if (currentEnd > now) return

            // === 1) DISPARAR ALARMA (notificación + sonido) ===
            val finishedTitle = app.getString(R.string.pomodoro_finished, currentPhaseName)
            // Notificación de fin (tap abre la detail del timer activo)
            val startRoute = "pomodoro/detail/${config.id}"
            showAlarmNotification(
                app,
                title = finishedTitle,
                text  = app.getString(R.string.pomodoro_tap_to_stop),
                startRoute = startRoute
            )
            // Avisar a la UI que la alarma está sonando (icono de parlante tachado)
            AlarmState.setActive(context, true)
            app.sendBroadcast(
                Intent(ACTION_POMODORO_ALARM_START)
                    .setPackage(app.packageName)
                    .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY or Intent.FLAG_RECEIVER_FOREGROUND)
            )
            // Reproducir sonido por ~10s y, al terminar, silenciar + avisar a UI
            AlarmSoundPlayer.play(app, durationMs = 10_000L) {
                PomodoroAlarmReceiver.silenceAlarm(app)
            }

            // === 2) CALCULAR PRÓXIMA FASE ===
            val phase = when (currentPhaseName) {
                app.getString(R.string.pomodoro_work)        -> PHASE_WORK
                app.getString(R.string.pomodoro_short_break) -> PHASE_SHORT
                app.getString(R.string.pomodoro_long_break)  -> PHASE_LONG
                else                                         -> PHASE_WORK
            }

            // Ciclo por timerId
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

            // === 3) PERSISTIR PRÓXIMA FASE + NOTIF. EN CURSO ===
            val endMs = now + nextMin * 60_000L
            val nextTitle = when (nextPhase) {
                PHASE_WORK  -> app.getString(R.string.pomodoro_work)
                PHASE_SHORT -> app.getString(R.string.pomodoro_short_break)
                PHASE_LONG  -> app.getString(R.string.pomodoro_long_break)
                else        -> currentPhaseName
            }
            repo.updatePhase(nextTitle, endMs, nextMin * 60L)

            // Notificación de fase en curso (sin sonido) + deep link a detail
            showRunningNotification(app, nextTitle, endMs, startRoute = startRoute)

            // === 4) REPROGRAMAR RESPALDO (INEXACTO) ===
            PomodoroAlarmReceiver.scheduleFromUiFallback(
                context = app,
                triggerAtMs = endMs,
                phase = nextPhase,
                cycle = nextCycle,
                config = config
            )
        }
    }
}
