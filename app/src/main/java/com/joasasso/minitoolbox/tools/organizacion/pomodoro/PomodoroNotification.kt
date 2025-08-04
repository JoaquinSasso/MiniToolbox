package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.joasasso.minitoolbox.MainActivity
import com.joasasso.minitoolbox.R

// IDs de canales
const val COUNTDOWN_CHANNEL_ID = "pomodoro_countdown_channel"
const val ALARM_CHANNEL_ID     = "pomodoro_alarm_channel"
// ID único de notificación (mismo para ambas, las actualiza)
const val NOTIFICATION_ID       = 1337

fun createPomodoroChannels(context: Context) {
    val mgr = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return

    val countdownChan = NotificationChannel(
        COUNTDOWN_CHANNEL_ID,
        context.getString(R.string.pomodoro_channel_countdown_name),
        NotificationManager.IMPORTANCE_LOW
    ).apply {
        description = context.getString(R.string.pomodoro_channel_countdown_desc)
        setShowBadge(false)
    }
    mgr.createNotificationChannel(countdownChan)

    val alarmChan = NotificationChannel(
        ALARM_CHANNEL_ID,
        context.getString(R.string.pomodoro_channel_alarm_name),
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = context.getString(R.string.pomodoro_channel_alarm_desc)
        enableLights(true)
        lightColor = Color.RED
        setShowBadge(false)
    }
    mgr.createNotificationChannel(alarmChan)
}


/** Notificación de conteo: MM:SS, sólo en bandeja */
fun buildCountdownNotification(context: Context, title: String, timeLeft: String): Notification {
    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra("startRoute", "pomodoro")
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
        .setOnlyAlertOnce(true)
        .addAction(
            R.drawable.ic_silence,
            context.getString(R.string.pomodoro_action_silence),
            PendingIntent.getService(
                context, 1,
                Intent(context, PomodoroService::class.java).setAction(ACTION_SILENCE),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        .addAction(
            R.drawable.ic_stop,
            context.getString(R.string.pomodoro_action_stop),
            PendingIntent.getService(
                context, 2,
                Intent(context, PomodoroService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()
}


/** Notificación de alarma: heads-up banner, sonido/vibración */
fun buildAlarmNotification(context: Context, title: String, text: String): Notification {
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
        .setFullScreenIntent(fsPI, false)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .addAction(R.drawable.ic_silence, context.getString(R.string.pomodoro_action_silence), silencePI)
        .addAction(R.drawable.ic_stop, context.getString(R.string.pomodoro_action_stop), stopPI)
        .setOngoing(true)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()
}

