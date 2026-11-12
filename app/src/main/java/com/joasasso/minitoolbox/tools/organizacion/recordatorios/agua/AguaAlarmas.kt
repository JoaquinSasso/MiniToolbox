package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.joasasso.minitoolbox.data.flujoAguaHoy
import com.joasasso.minitoolbox.data.flujoObjetivo
import com.joasasso.minitoolbox.data.flujoPorVaso
import com.joasasso.minitoolbox.widgets.AguaMiniWidget
import com.joasasso.minitoolbox.widgets.AguaWidget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

// Alarmas del recordatorio y refresco de los widgets de agua. Antes vivían en AguaReminderScreen.kt.

/**Funcion para re/programar el recordatorio de agua cada vez que cambia el progreso de consumo de agua*/
fun programarRecordatorioAgua(
    context: Context,
    frecuenciaMinutos: Int
) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val intent = Intent(context, WaterReminderReceiver::class.java).apply {
        putExtra("frecuencia_minutos", frecuenciaMinutos)
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context, 101,
        intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val triggerTime = System.currentTimeMillis() + frecuenciaMinutos * 60 * 1000L

    alarmManager.cancel(pendingIntent) // Cancela cualquier alarma previa
    alarmManager.setInexactRepeating(
        AlarmManager.RTC_WAKEUP,
        triggerTime,
        frecuenciaMinutos * 60 * 1000L,
        pendingIntent
    )
}


/**Funcion para cancelar el recordatorio de agua cuando se desactivan las notificaciones*/
fun cancelarRecordatorioAgua(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, WaterReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 101,
        intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
    cancelarNotificacionAguaActiva(context)
}

/**Funcion suspendida para actualizar el progreso del consumo de agua en el widget desde la app*/
suspend fun actualizarWidgetAguaSuspend(context: Context) {
    try {
        val agua = context.flujoAguaHoy().first()
        val objetivo = context.flujoObjetivo().first()
        val porVaso = context.flujoPorVaso().first()

        val glanceManager = GlanceAppWidgetManager(context)

        // Actualiza AguaWidget (el largo)
        val idsLargos = glanceManager.getGlanceIds(AguaWidget::class.java)
        idsLargos.forEach { glanceId ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[AguaWidget.KEY_AGUA] = agua
                    this[AguaWidget.KEY_OBJETIVO] = objetivo
                    this[AguaWidget.KEY_POR_VASO] = porVaso
                }
            }
        }
        AguaWidget().updateAll(context)

        // Actualiza AguaMiniWidget (el compacto)
        val idsMini = glanceManager.getGlanceIds(AguaMiniWidget::class.java)
        idsMini.forEach { glanceId ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[AguaMiniWidget.KEY_AGUA] = agua
                    this[AguaMiniWidget.KEY_OBJETIVO] = objetivo
                    this[AguaMiniWidget.KEY_POR_VASO] = porVaso
                }
            }
        }
        AguaMiniWidget().updateAll(context)
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.e("AguaReminder", "Error al actualizar widget de agua", e)
    }
}

/**Funcion para actualizar el progreso del consumo de agua en el widget desde la app*/
fun actualizarWidgetAgua(context: Context) {
    CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
        actualizarWidgetAguaSuspend(context)
    }
}


fun programarResetAguaDiario(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ResetAguaReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val calendar = Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 1)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (before(Calendar.getInstance())) {
            add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    alarmManager.setRepeating(
        AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        AlarmManager.INTERVAL_DAY,
        pendingIntent
    )
}
