package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.joasasso.minitoolbox.MainActivity
import com.joasasso.minitoolbox.R

const val CHANNEL_RUNNING = "pomodoro_running"
const val CHANNEL_ALARM   = "pomodoro_alarm_v2"
const val CHANNEL_ALARM_SILENT = "pomodoro_alarm_silent_v3"
const val NOTIF_ID_RUNNING = 2001
const val NOTIFICATION_ID  = 2002 // alarma (legacy)
const val NOTIF_ID_ALARM_SILENT = 2003
const val ACTION_POMODORO_ALARM_SILENCE = "POMODORO_ALARM_SILENCE"


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

    // Canal "alarm" CON sonido del sistema.
    // Se usa sólo como plan B: cuando no pudimos arrancar el servicio en primer
    // plano y necesitamos que al menos suene algo.
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

    // Canal ALARM sin sonido: el audio lo maneja PomodoroAlarmService con ExoPlayer.
    //
    // Este canal es la ÚNICA fuente del silencio de la notificación de alarma.
    // Desde Android 8 el canal manda sobre el builder en todo lo que sea sonido
    // y vibración, así que no hace falta (ni conviene) volver a silenciar del
    // lado del builder — ver el comentario en buildAlarmNotification().
    if (nm.getNotificationChannel(CHANNEL_ALARM_SILENT) == null) {
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALARM_SILENT,
                context.getString(R.string.pomodoro_channel_alarm),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.pomodoro_channel_alarm_desc)
                setSound(null, null)
                enableVibration(false)          // vibra el servicio, no el canal
                enableLights(true)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
    }
}

internal fun mainPendingIntent(context: Context, startRoute: String?): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
        if (!startRoute.isNullOrBlank()) putExtra("startRoute", startRoute)
    }
    val reqCode = (startRoute ?: "default_route").hashCode()
    return PendingIntent.getActivity(
        context,
        reqCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

/**
 * PendingIntent hacia la pantalla completa de alarma (PomodoroAlarmActivity),
 * no hacia MainActivity. Se usa tanto para el toque manual de la notificación
 * como para el full-screen intent automático — las dos vías tienen que llevar
 * al mismo lugar: un botón grande de "apagar", no al detalle del timer.
 */
internal fun alarmActivityPendingIntent(
    context: Context,
    title: String,
    text: String,
    startRoute: String?
): PendingIntent {
    val intent = Intent(context, PomodoroAlarmActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(EX_ALARM_TITLE, title)
        putExtra(EX_ALARM_TEXT, text)
        if (!startRoute.isNullOrBlank()) putExtra(EX_ALARM_ROUTE, startRoute)
    }
    val reqCode = 9918
    return PendingIntent.getActivity(
        context,
        reqCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

/** Notificación "en curso" mientras corre la fase (sin conteo por segundo). */
fun showRunningNotification(context: Context, title: String, endMs: Long, startRoute: String? = null) {
    ensurePomodoroChannels(context)

    val text = context.getString(
        R.string.pomodoro_running_until,
        android.text.format.DateFormat.getTimeFormat(context).format(endMs)
    )

    val notif = NotificationCompat.Builder(context, CHANNEL_RUNNING)
        .setSmallIcon(R.drawable.ic_pomodoro)
        .setContentTitle(title)
        .setContentText(text)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setContentIntent(mainPendingIntent(context, startRoute))
        .setCategory(Notification.CATEGORY_STATUS)
        .build()

    val nm = ContextCompat.getSystemService(context, NotificationManager::class.java)
    nm?.notify(NOTIF_ID_RUNNING, notif)
}

fun cancelRunningNotification(context: Context) {
    val nm = ContextCompat.getSystemService(context, NotificationManager::class.java)
    nm?.cancel(NOTIF_ID_RUNNING)
}

/**
 * Construye la notificación de alarma.
 *
 * Es la MISMA notificación que usa PomodoroAlarmService como notificación de
 * primer plano, por eso se separó del `notify()`: un FGS necesita el objeto
 * Notification, no que ya esté publicada.
 *
 * @param withChannelSound si true usa el canal CON sonido del sistema (plan B,
 *        cuando no pudimos arrancar el servicio).
 */
fun buildAlarmNotification(
    context: Context,
    title: String,
    text: String,
    startRoute: String? = null,
    withChannelSound: Boolean = false
): Notification {
    ensurePomodoroChannels(context)

    val channel = if (withChannelSound) CHANNEL_ALARM else CHANNEL_ALARM_SILENT

    // Un solo PendingIntent para las dos vías (toque manual y full-screen
    // automático): las dos tienen que abrir la pantalla de "apagar alarma",
    // no MainActivity ni el detalle del timer.
    val ringingIntent = alarmActivityPendingIntent(context, title, text, startRoute)

    val builder = NotificationCompat.Builder(context, channel)
        .setSmallIcon(R.drawable.ic_pomodoro)
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setOngoing(true)
        .setAutoCancel(false)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setContentIntent(ringingIntent)
        .addAction(
            R.drawable.volume_off,
            context.getString(R.string.pomodoro_silence),
            silencePendingIntent(context)
        )

    if (!withChannelSound) {
        // NO volver a poner setSilent(true) acá. Fue la causa de que la alarma
        // nunca hiciera heads-up ni saltara a pantalla completa, durante toda
        // una tanda de tests en los que el permiso, el canal y el DND daban
        // bien.
        //
        // setSilent(true) no se limita a sacar el sonido: además setea el
        // group alert behavior en GROUP_ALERT_SUMMARY y, al no haber grupo
        // asignado, mete la notificación en el grupo "silent". Queda entonces
        // como hija de un grupo cuya política dice "sólo alerta el resumen" —
        // y ese resumen nunca se publica. Notification.suppressAlertingDueToGrouping()
        // pasa a devolver true, y con eso el sistema bloquea las dos cosas:
        // NotificationManagerService la silencia y SystemUI le niega el
        // heads-up y el full-screen intent (lo registra como
        // NO_FSI_SUPPRESSIVE_GROUP_ALERT_BEHAVIOR).
        //
        // El silencio que queríamos ya lo garantiza CHANNEL_ALARM_SILENT, que
        // se crea con setSound(null, null) y enableVibration(false).
        builder.setDefaults(0)
    }

    // Full screen intent: en Android 14+ el permiso USE_FULL_SCREEN_INTENT sólo
    // se autoconcede a apps de alarma/llamadas. Si no lo tenemos, el sistema
    // degrada la notificación a heads-up; pedirlo igual no rompe nada, pero
    // chequeamos para no depender de él.
    val nm = context.getSystemService(NotificationManager::class.java)
    val canFullScreen = if (Build.VERSION.SDK_INT >= 34) {
        nm?.canUseFullScreenIntent() == true
    } else true

    // Diagnóstico: sin esto, un permiso denegado o un canal con la
    // importancia rebajada por el sistema fallan en silencio — igual que nos
    // pasó con el audio.
    //
    // OJO con la lección de este bug: estas dos métricas miden el PERMISO y el
    // CANAL, no el objeto Notification. La supresión por grupo no aparece acá.
    // Si alguna vez vuelve a no saltar con estas dos líneas en verde, mirar la
    // notificación en sí con `adb shell dumpsys notification --noredact`.
    val liveImportance = nm?.getNotificationChannel(channel)?.importance
    Log.d(
        "PomodoroNotification",
        "buildAlarmNotification: canFullScreen=$canFullScreen " +
                "channelImportance=$liveImportance (HIGH=${NotificationManager.IMPORTANCE_HIGH}) " +
                "channel=$channel"
    )

    if (canFullScreen) {
        builder.setFullScreenIntent(ringingIntent, true)
    }

    return builder.build()
}

/**
 * Publica la notificación de alarma directamente, sin servicio.
 * Sólo para casos donde no se puede arrancar un FGS (por ejemplo desde
 * BOOT_COMPLETED, o si el arranque en background fue rechazado).
 */
fun showAlarmNotification(
    context: Context,
    title: String,
    text: String,
    startRoute: String? = null,
    withChannelSound: Boolean = false
): Int {
    val nm = ContextCompat.getSystemService(context, NotificationManager::class.java)
    nm?.notify(
        NOTIF_ID_ALARM_SILENT,
        buildAlarmNotification(context, title, text, startRoute, withChannelSound)
    )
    return NOTIF_ID_ALARM_SILENT
}

private fun silencePendingIntent(context: Context): PendingIntent {
    val i = Intent(context, PomodoroAlarmReceiver::class.java).apply {
        action = ACTION_POMODORO_ALARM_SILENCE
    }
    val reqCode = 9917
    return PendingIntent.getBroadcast(
        context,
        reqCode,
        i,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}