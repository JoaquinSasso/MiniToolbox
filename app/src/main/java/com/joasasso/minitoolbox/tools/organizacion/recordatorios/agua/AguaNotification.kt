package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.joasasso.minitoolbox.MainActivity
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.flujoAguaHoy
import com.joasasso.minitoolbox.data.flujoObjetivo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class WaterReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val consumidoML = runBlocking { context.flujoAguaHoy().first() }
        val objetivoML = runBlocking { context.flujoObjetivo().first() }
        showNotification(context, consumidoML, objetivoML)
    }

    private fun showNotification(context: Context, consumido: Int, objetivo: Int) {
        val channelId = WATER_REMINDER_CHANNEL_ID
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            context.getString(R.string.water_reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.water_reminder_channel_description)
        }
        manager.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("startRoute", "agua")
        }

        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(context.getString(R.string.water_reminder_title))
            .setContentText(
                context.getString(
                    R.string.water_reminder_content,
                    consumido / 1000f,
                    objetivo / 1000f
                )
            )
            .setContentIntent(pi)
            .setSmallIcon(R.drawable.ic_water)
            .setAutoCancel(true)
            .build()

        manager.notify(101, notification)
    }
}


const val WATER_REMINDER_CHANNEL_ID = "water_reminder_channel"

/** Crea el canal de notificación para el recordatorio de agua */
fun createWaterReminderChannel(context: Context) {
    val mgr = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return

    val channel = NotificationChannel(
        WATER_REMINDER_CHANNEL_ID,
        context.getString(R.string.water_reminder_channel_name),
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = context.getString(R.string.water_reminder_channel_description)
        enableLights(true)
        lightColor = android.graphics.Color.BLUE
        setShowBadge(false)
    }
    mgr.createNotificationChannel(channel)
}

