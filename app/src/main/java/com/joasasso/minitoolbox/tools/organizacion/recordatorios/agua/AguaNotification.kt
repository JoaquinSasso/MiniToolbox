package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.joasasso.minitoolbox.MainActivity
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.KEY_NOTIF_ACTIVAS
import com.joasasso.minitoolbox.data.KEY_OBJETIVO
import com.joasasso.minitoolbox.data.aguaDataStore
import com.joasasso.minitoolbox.data.keyFecha
import com.joasasso.minitoolbox.metrics.MetricsSource
import com.joasasso.minitoolbox.nav.Screen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

const val WATER_REMINDER_NOTIFICATION_ID = 101
const val WATER_REMINDER_CHANNEL_ID = "water_reminder_channel"

class WaterReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                handleReminder(context)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("WaterReminderReceiver", "Error al procesar recordatorio de agua", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    internal suspend fun handleReminder(context: Context, today: LocalDate = LocalDate.now()) {
        val prefs = context.aguaDataStore.data.first()
        if (prefs[KEY_NOTIF_ACTIVAS] != 1) return

        val consumidoML = prefs[keyFecha(today)] ?: 0
        val objetivoML = prefs[KEY_OBJETIVO] ?: 2000
        showNotification(context, consumidoML, objetivoML)
    }

    internal fun showNotification(context: Context, consumido: Int, objetivo: Int) {
        createWaterReminderChannel(context)
        val manager = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(Screen.EXTRA_START_ROUTE_JSON, Screen.toJson(Screen.Water))
            putExtra(MetricsSource.EXTRA_START_SOURCE, MetricsSource.NOTIFICATION)
        }

        val pi = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, WATER_REMINDER_CHANNEL_ID)
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

        manager.notify(WATER_REMINDER_NOTIFICATION_ID, notification)
    }
}

/** Cancela cualquier notificación activa del recordatorio de agua en la barra de estado */
fun cancelarNotificacionAguaActiva(context: Context) {
    val mgr = ContextCompat.getSystemService(context, NotificationManager::class.java)
    mgr?.cancel(WATER_REMINDER_NOTIFICATION_ID)
}

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
