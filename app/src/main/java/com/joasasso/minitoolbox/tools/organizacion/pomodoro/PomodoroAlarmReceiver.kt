package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.PomodoroStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

const val ACTION_FIRE_ALARM = "POMODORO_FIRE_ALARM"
private const val REQ_ALARM = 1001

private const val PHASE_WORK  = "WORK"
private const val PHASE_SHORT = "SHORT"
private const val PHASE_LONG  = "LONG"

private const val ALARM_AUTO_SILENCE_MS = 8000L // 8 segundos, es el tiempo necesario para que la alarma se silencie automaticamente

class PomodoroAlarmReceiver : BroadcastReceiver() {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE_ALARM) return

        val workMin  = intent.getIntExtra("WORK_MINUTES", 25)
        val shortMin = intent.getIntExtra("SHORT_BREAK", 5)
        val longMin  = intent.getIntExtra("LONG_BREAK", 15)
        val phase    = intent.getStringExtra("PHASE") ?: PHASE_WORK
        val cycle    = intent.getIntExtra("CYCLE", 0)

        // Para trabajo asíncrono en Receiver:
        val pending = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                // 1) Notificación de fin de fase (canal sin sonido) + avisar a la UI (START)
                val finishedTitle = appContext.getString(
                    R.string.pomodoro_finished,
                    phaseNameLocalized(appContext, phase)
                )
                showAlarmNotification(
                    appContext,
                    finishedTitle,
                    appContext.getString(R.string.pomodoro_tap_to_stop)
                )
                appContext.sendBroadcast(
                    Intent(ACTION_POMODORO_ALARM_START)
                        .setPackage(appContext.packageName)
                        .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY or Intent.FLAG_RECEIVER_FOREGROUND)
                )

                // 1.1) Reproducir sonido de alarma controlado (auto-stop) y, al cortar, notificar STOP
                AlarmSoundPlayer.play(appContext, durationMs = 10_000L) {
                    // callback de auto-silencio: detiene audio (por si aún suena),
                    // cancela la notificación y emite ACTION_POMODORO_ALARM_STOP
                    silenceAlarm(appContext)
                }

                // 2) Calcular próxima fase
                val (nextPhase, nextMin, nextCycle) = when (phase) {
                    PHASE_WORK -> {
                        val longNext = ((cycle + 1) % 4 == 0)
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
                    endMs
                )

                // 5) Agendar la próxima alarma exacta
                scheduleExact(appContext, endMs, workMin, shortMin, longMin, nextPhase, nextCycle)

                // (Sin delay ni auto-silencio aquí: lo maneja AlarmSoundPlayer + callback)
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
        fun startPomodoro(context: Context, workMin: Int, shortMin: Int, longMin: Int) {
            val appContext = context.applicationContext
            val endMs = System.currentTimeMillis() + workMin * 60_000L

            CoroutineScope(Dispatchers.IO).launch {
                PomodoroStateRepository(appContext).updatePhase(
                    appContext.getString(R.string.pomodoro_work),
                    endMs,
                    workMin * 60L
                )
            }

            showRunningNotification(
                appContext,
                appContext.getString(R.string.pomodoro_work),
                endMs
            )

            scheduleExact(appContext, endMs, workMin, shortMin, longMin, PHASE_WORK, 0)
        }

        fun stopPomodoro(context: Context) {
            val appContext = context.applicationContext
            cancelAlarm(appContext)
            // Cancelar ambas notificaciones
            cancelRunningNotification(appContext)
            val nm = ContextCompat.getSystemService(appContext, NotificationManager::class.java)
            nm?.cancel(NOTIFICATION_ID)

            CoroutineScope(Dispatchers.IO).launch {
                PomodoroStateRepository(appContext).clearPhase()
            }
        }

        @Volatile private var alarmActive: Boolean = false // si lo usás, marcá true tras showAlarmNotification

        fun silenceAlarm(context: Context) {
            val nm = ContextCompat.getSystemService(context, NotificationManager::class.java)
            nm?.cancel(NOTIF_ID_ALARM_SILENT)

            // detener audio por si estaba sonando
            AlarmSoundPlayer.stop()

            // avisar a la UI
            context.sendBroadcast(
                Intent(ACTION_POMODORO_ALARM_STOP)
                    .setPackage(context.packageName)
                    .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY or Intent.FLAG_RECEIVER_FOREGROUND)
            )
        }


        private fun scheduleExact(
            context: Context,
            triggerAtMs: Long,
            workMin: Int,
            shortMin: Int,
            longMin: Int,
            phase: String,
            cycle: Int
        ) {
            val am = context.getSystemService(AlarmManager::class.java)
            val i = Intent(context, PomodoroAlarmReceiver::class.java).apply {
                action = ACTION_FIRE_ALARM
                putExtra("WORK_MINUTES", workMin)
                putExtra("SHORT_BREAK", shortMin)
                putExtra("LONG_BREAK", longMin)
                putExtra("PHASE", phase)
                putExtra("CYCLE", cycle)
            }
            val pi = PendingIntent.getBroadcast(
                context, REQ_ALARM, i,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
            )
            try {
                am?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            } catch (se: SecurityException) {
                am?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
                // (opcional) avisá a la UI para sugerir activar "Alarmas y recordatorios"
            }
        }

        private fun cancelAlarm(context: Context) {
            val am = context.getSystemService(AlarmManager::class.java)
            val i = Intent(context, PomodoroAlarmReceiver::class.java).apply { action = ACTION_FIRE_ALARM }
            val pi = PendingIntent.getBroadcast(
                context, REQ_ALARM, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )
            if (pi != null) {
                am?.cancel(pi)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
fun ensureExactAlarmAccess(context: Context): Boolean {
    val am = context.getSystemService(AlarmManager::class.java) ?: return false
    val can = am.canScheduleExactAlarms()
    if (!can) {
        // Llevar al usuario a la pantalla de "Alarms & reminders"
        // Solo invocalo desde UI (ej: al tocar Start). Desde un Receiver no abras activities.
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Si estás llamando desde una Activity/Screen, usá startActivity(intent).
        // Si estás en un Context genérico, devolvé false y que la UI lo maneje.
    }
    return can
}
