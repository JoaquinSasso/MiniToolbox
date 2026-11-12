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
import com.joasasso.minitoolbox.data.HORA_FIN_DEFAULT_MIN
import com.joasasso.minitoolbox.data.HORA_INICIO_DEFAULT_MIN
import com.joasasso.minitoolbox.data.KEY_HORA_FIN_MIN
import com.joasasso.minitoolbox.data.KEY_HORA_INICIO_MIN
import com.joasasso.minitoolbox.data.KEY_NOTIF_ACTIVAS
import com.joasasso.minitoolbox.data.KEY_OBJETIVO
import com.joasasso.minitoolbox.data.KEY_PAUSAR_AL_CUMPLIR
import com.joasasso.minitoolbox.data.KEY_POR_VASO
import com.joasasso.minitoolbox.data.aguaDataStore
import com.joasasso.minitoolbox.data.flujoPorVaso
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
import java.time.LocalTime

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

    internal suspend fun handleReminder(
        context: Context,
        today: LocalDate = LocalDate.now(),
        ahora: LocalTime = LocalTime.now()
    ) {
        val prefs = context.aguaDataStore.data.first()
        if (prefs[KEY_NOTIF_ACTIVAS] != 1) return

        val consumidoML = prefs[keyFecha(today)] ?: 0
        val objetivoML = prefs[KEY_OBJETIVO] ?: 2000
        val mlPorVaso = prefs[KEY_POR_VASO] ?: 250
        val debe = AguaMath.debeRecordar(
            minutoDelDia = ahora.hour * 60 + ahora.minute,
            inicioMin = prefs[KEY_HORA_INICIO_MIN] ?: HORA_INICIO_DEFAULT_MIN,
            finMin = prefs[KEY_HORA_FIN_MIN] ?: HORA_FIN_DEFAULT_MIN,
            pausarAlCumplir = (prefs[KEY_PAUSAR_AL_CUMPLIR] ?: 1) == 1,
            consumidoML = consumidoML,
            objetivoML = objetivoML,
            mlPorVaso = mlPorVaso
        )
        if (!debe) return
        showNotification(context, consumidoML, objetivoML, mlPorVaso)
    }

    internal fun showNotification(context: Context, consumido: Int, objetivo: Int, mlPorVaso: Int) {
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

        // Registrar sin abrir la app: es el atajo de mayor valor del recordatorio
        val sumarIntent = Intent(context, AguaNotificacionAccionReceiver::class.java)
            .setAction(AguaNotificacionAccionReceiver.ACTION_SUMAR_VASO)
        val sumarPi = PendingIntent.getBroadcast(
            context, 102, sumarIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, WATER_REMINDER_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.water_reminder_title))
            .setContentText(textoRecordatorio(context, consumido, objetivo, mlPorVaso))
            .setContentIntent(pi)
            .addAction(
                R.drawable.water_drop,
                context.getString(R.string.water_add, mlPorVaso),
                sumarPi
            )
            .setSmallIcon(R.drawable.ic_water)
            .setAutoCancel(true)
            .build()

        manager.notify(WATER_REMINDER_NOTIFICATION_ID, notification)
    }
}

/**
 * Acción "+vaso" de la notificación. Suma el vaso configurado y deja todo como si se hubiera
 * sumado desde la app: notificación cerrada, widgets al día y próximo recordatorio corrido.
 */
class AguaNotificacionAccionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SUMAR_VASO = "com.joasasso.minitoolbox.agua.SUMAR_VASO"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SUMAR_VASO) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val porVaso = context.flujoPorVaso().first()
                cambiarAguaFueraDeLaApp(context, porVaso)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("AguaNotifAccion", "Error al sumar agua desde la notificación", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/** "Llevas 0,75 L de 2 L. Te faltan 1,25 L." o, si ya se cumplió la meta, el mismo inicio y un cierre distinto. */
internal fun textoRecordatorio(context: Context, consumido: Int, objetivo: Int, mlPorVaso: Int): String {
    val consumidoTxt = AguaFormat.litros(consumido)
    val objetivoTxt = AguaFormat.litros(objetivo)
    if (AguaMath.objetivoAlcanzado(consumido, objetivo, mlPorVaso)) {
        return context.getString(R.string.water_reminder_content_done, consumidoTxt, objetivoTxt)
    }
    val faltan = (objetivo - consumido).coerceAtLeast(0)
    val faltanTxt = if (AguaFormat.usaLitros(faltan)) {
        context.getString(R.string.water_liters_value, AguaFormat.litros(faltan))
    } else {
        context.getString(R.string.water_settings_glass_value, faltan)
    }
    return context.getString(R.string.water_reminder_content, consumidoTxt, objetivoTxt, faltanTxt)
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
