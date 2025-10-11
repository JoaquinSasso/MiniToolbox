package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.joasasso.minitoolbox.MainActivity
import com.joasasso.minitoolbox.R

const val CHANNEL_RUNNING = "pomodoro_running"
const val CHANNEL_ALARM   = "pomodoro_alarm_v2"
const val CHANNEL_ALARM_SILENT = "pomodoro_alarm_silent_v3"
const val NOTIF_ID_RUNNING = 2001
const val NOTIFICATION_ID  = 2002 // alarma
const val NOTIF_ID_ALARM_SILENT = 2003


fun ensurePomodoroChannels(context: Context) {
    val nm = context.getSystemService(NotificationManager::class.java) ?: return

    // Canal "running" (sin sonido)
    if (nm.getNotificationChannel(CHANNEL_RUNNING) == null) {
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_RUNNING,
                context.getString(R.string.pomodoro_channel_running),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.pomodoro_channel_running_desc)
                setShowBadge(false)
            }
        )
    }

    // Canal "alarm" (con sonido). Usamos un **nuevo ID** para garantizar sonido correcto.
    if (nm.getNotificationChannel(CHANNEL_ALARM) == null) {
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALARM,
                context.getString(R.string.pomodoro_channel_alarm),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.pomodoro_channel_alarm_desc)
                setSound(alarmUri, attrs)
                enableVibration(true)
                enableLights(true)
            }
        )
    }

    // Canal ALARM sin sonido (para usar MediaPlayer manual)
    if (nm.getNotificationChannel(CHANNEL_ALARM_SILENT) == null) {
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALARM_SILENT,
                context.getString(R.string.pomodoro_channel_alarm),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.pomodoro_channel_alarm_desc)
                setSound(null, null)       // <- SIN sonido
                enableVibration(true)
                enableLights(true)
            }
        )
    }
}

private fun mainPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        // Abrí la app en la screen del Pomodoro (usa tu deep link si tenés)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
    return PendingIntent.getActivity(
        context, 991, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

/** Notif Ongoing mientras corre la fase (sin conteo por segundo). */
fun showRunningNotification(context: Context, title: String, endMs: Long) {
    ensurePomodoroChannels(context)

    val text = context.getString(R.string.pomodoro_running_until,
        android.text.format.DateFormat.getTimeFormat(context).format(endMs))

    val notif = NotificationCompat.Builder(context, CHANNEL_RUNNING)
        .setSmallIcon(R.drawable.ic_pomodoro)
        .setContentTitle(title)           // p.ej. "Trabajo" / "Descanso corto"
        .setContentText(text)             // "Hasta 14:35"
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setContentIntent(mainPendingIntent(context))
        .setCategory(Notification.CATEGORY_STATUS)
        .build()

    val nm = ContextCompat.getSystemService(context, NotificationManager::class.java)
    nm?.notify(NOTIF_ID_RUNNING, notif)
}

fun cancelRunningNotification(context: Context) {
    val nm = ContextCompat.getSystemService(context, NotificationManager::class.java)
    nm?.cancel(NOTIF_ID_RUNNING)
}

/** Notif de alarma al finalizar fase (con sonido del canal). */
fun showAlarmNotification(context: Context, title: String, text: String): Int {
    ensurePomodoroChannels(context)
    val notif = NotificationCompat.Builder(context, CHANNEL_ALARM_SILENT) // <- canal sin sonido
        .setSmallIcon(R.drawable.ic_pomodoro)
        .setContentTitle(title)
        .setContentText(text)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(false) // la cancelamos nosotros al silenciar
        .setCategory(Notification.CATEGORY_ALARM)
        .setContentIntent(mainPendingIntent(context))
        .build()
    val nm = ContextCompat.getSystemService(context, NotificationManager::class.java)
    nm?.notify(NOTIF_ID_ALARM_SILENT, notif)
    return NOTIF_ID_ALARM_SILENT
}

