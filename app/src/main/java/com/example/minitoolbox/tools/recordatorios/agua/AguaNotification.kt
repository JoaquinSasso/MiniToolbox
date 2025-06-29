package com.example.minitoolbox.tools.recordatorios.agua

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.minitoolbox.R

class WaterReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val consumidoML = intent.getIntExtra("agua_consumida_ml", 0)
        val objetivoML = intent.getIntExtra("agua_objetivo_ml", 2000)

        showNotification(context, consumidoML, objetivoML)
    }

    private fun showNotification(context: Context, consumido: Int, objetivo: Int) {
        val channelId = "agua_reminder_channel"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, "Recordatorio de Agua", NotificationManager.IMPORTANCE_DEFAULT)
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Hora de beber agua 💧")
            .setContentText("Has consumido ${(consumido / 1000f).let { "%.2f".format(it) }}L de ${(objetivo / 1000f).let { "%.2f".format(it) }}L")
            .setSmallIcon(R.drawable.ic_water)
            .setAutoCancel(true)
            .build()

        manager.notify(101, notification)
    }
}

const val WATER_REMINDER_CHANNEL_ID = "water_reminder_channel"
const val WATER_REMINDER_NOTIFICATION_ID = 2025

/** Crea el canal de notificación para el recordatorio de agua */
fun createWaterReminderChannel(context: Context) {
    val mgr = ContextCompat.getSystemService(context, NotificationManager::class.java)
        ?: return
    val channel = NotificationChannel(
        WATER_REMINDER_CHANNEL_ID,
        "Recordatorio de Agua",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "Recordatorios para beber agua durante el día"
        enableLights(true)
        lightColor = android.graphics.Color.BLUE
        setShowBadge(false)
    }
    mgr.createNotificationChannel(channel)
}
