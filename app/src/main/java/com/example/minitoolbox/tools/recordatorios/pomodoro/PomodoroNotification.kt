package com.example.minitoolbox.tools.pomodoro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.minitoolbox.MainActivity
import com.example.minitoolbox.R

// IDs de canales
const val COUNTDOWN_CHANNEL_ID = "pomodoro_countdown_channel"
const val ALARM_CHANNEL_ID     = "pomodoro_alarm_channel"
// ID único de notificación (mismo para ambas, las actualiza)
const val NOTIFICATION_ID       = 1337

fun createPomodoroChannels(context: Context) {
    val mgr = ContextCompat.getSystemService(context, NotificationManager::class.java)
        ?: return

    // Canal para conteo (baja importancia: sólo bandeja)
    val countdownChan = NotificationChannel(
        COUNTDOWN_CHANNEL_ID,
        "Pomodoro (Conteo)",
        NotificationManager.IMPORTANCE_LOW
    ).apply {
        description = "Actualizaciones de tiempo restante del Pomodoro"
        setShowBadge(false)
    }
    mgr.createNotificationChannel(countdownChan)

    // Canal para alarma (alta importancia: heads-up banner)
    val alarmChan = NotificationChannel(
        ALARM_CHANNEL_ID,
        "Pomodoro (Alarma)",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "Alertas de fin de ciclo Pomodoro"
        enableLights(true)
        lightColor = Color.RED
        setShowBadge(false)
    }
    mgr.createNotificationChannel(alarmChan)
}

/** Notificación de conteo: MM:SS, sólo en bandeja */
fun buildCountdownNotification(context: Context, title: String, timeLeft: String): Notification {
    // Intent que abre PomodoroScreen
    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra("startRoute", "pomodoro")
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pi = PendingIntent.getActivity(
        context, 0, intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    return NotificationCompat.Builder(context, COUNTDOWN_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_pomodoro)
        .setContentTitle(title)
        .setContentText(timeLeft)
        .setContentIntent(pi)
        .setOngoing(true)
        .setOnlyAlertOnce(true)                // no volver a sonar/vibrar
        .addAction(R.drawable.ic_silence, "Silenciar",  // siempre disponibles
            PendingIntent.getService(
                context, 1,
                Intent(context, PomodoroService::class.java).setAction(ACTION_SILENCE),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            ))
        .addAction(R.drawable.ic_stop,    "Detener",
            PendingIntent.getService(
                context, 2,
                Intent(context, PomodoroService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            ))
        .setPriority(NotificationCompat.PRIORITY_LOW)     // no heads-up en conteo
        .build()
}

/** Notificación de alarma: heads-up banner, sonido/vibración */
fun buildAlarmNotification(context: Context, title: String, text: String): Notification {
    // Intents de acción
    val stopPI = PendingIntent.getService(
        context, 1,
        Intent(context, PomodoroService::class.java).setAction(ACTION_STOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val silencePI = PendingIntent.getService(
        context, 2,
        Intent(context, PomodoroService::class.java).setAction(ACTION_SILENCE),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    // Intent para banner que abra PomodoroScreen
    val fsIntent = Intent(context, MainActivity::class.java).apply {
        putExtra("startRoute", "pomodoro")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val fsPI = PendingIntent.getActivity(
        context, 3, fsIntent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    return NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_pomodoro)
        .setContentTitle(title)
        .setContentText(text)
        .setFullScreenIntent(fsPI, false)     // false para banner, true pondría pantalla completa
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .addAction(R.drawable.ic_silence, "Silenciar", silencePI)
        .addAction(R.drawable.ic_stop,    "Detener",  stopPI)
        .setOngoing(true)
        .setDefaults(NotificationCompat.DEFAULT_ALL)  // sonido y vibración
        .setPriority(NotificationCompat.PRIORITY_HIGH) // heads-up
        .build()
}
