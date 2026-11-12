package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.joasasso.minitoolbox.data.flujoFrecuenciaMinutos
import com.joasasso.minitoolbox.data.flujoNotificacionesActivas
import com.joasasso.minitoolbox.data.flujoPorVaso
import com.joasasso.minitoolbox.data.incrementarAguaHoy
import com.joasasso.minitoolbox.metrics.widgetUse
import com.joasasso.minitoolbox.widgets.WidgetWaterKindResolver
import kotlinx.coroutines.flow.first

class AgregarAguaCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        cambiarAguaFueraDeLaApp(context, context.flujoPorVaso().first())
        widgetUse(context, WidgetWaterKindResolver.resolve(context, glanceId))
    }
}

class QuitarAguaCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        cambiarAguaFueraDeLaApp(context, -context.flujoPorVaso().first())
        widgetUse(context, WidgetWaterKindResolver.resolve(context, glanceId))
    }
}

/**
 * Registra [delta] ml en el día de hoy desde fuera de la app (widget o notificación) y deja
 * el resto en el mismo estado que si se hubiera hecho desde la pantalla: notificación visible
 * cerrada, widgets actualizados y recordatorio reprogramado si está activo.
 */
suspend fun cambiarAguaFueraDeLaApp(context: Context, delta: Int) {
    context.incrementarAguaHoy(delta)

    cancelarNotificacionAguaActiva(context)
    actualizarWidgetAguaSuspend(context)

    if (context.flujoNotificacionesActivas().first()) {
        programarRecordatorioAgua(context, context.flujoFrecuenciaMinutos().first())
    }
}
