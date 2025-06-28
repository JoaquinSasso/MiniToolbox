package com.example.minitoolbox.tools.recordatorios.agua

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.minitoolbox.R

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

/**
 * Envía una notificación de recordatorio de agua indicando el progreso.
 * @param context Contexto de la app.
 * @param consumidoML Cantidad de agua consumida en mililitros.
 * @param objetivoML Objetivo diario en mililitros.
 */
@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
fun sendWaterReminderNotification(
    context: Context,
    consumidoML: Int,
    objetivoML: Int
) {
    // Formatea los litros: 1500 → 1.5L
    fun formatoLitros(ml: Int) = "%.2f".format(ml / 1000f).trimEnd('0').trimEnd('.') + "L"

    val progreso = "${formatoLitros(consumidoML)} / ${formatoLitros(objetivoML)}"
    val builder = NotificationCompat.Builder(context, WATER_REMINDER_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_water) // Usa tu propio ícono aquí
        .setContentTitle("¡Hora de beber agua!")
        .setContentText("Llevas $progreso hoy 💧")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)

    NotificationManagerCompat.from(context)
        .notify(WATER_REMINDER_NOTIFICATION_ID, builder.build())
}
