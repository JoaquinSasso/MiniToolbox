package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.joasasso.minitoolbox.data.flujoFrecuenciaMinutos
import com.joasasso.minitoolbox.data.flujoNotificacionesActivas
import com.joasasso.minitoolbox.data.flujoPorVaso
import com.joasasso.minitoolbox.data.incrementarAguaHoy
import com.joasasso.minitoolbox.metrics.storage.AggregatesRepository
import com.joasasso.minitoolbox.metrics.widgetUse
import com.joasasso.minitoolbox.widgets.WidgetWaterKindResolver
import kotlinx.coroutines.flow.first

class AgregarAguaCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val porVaso = context.flujoPorVaso().first()
        context.incrementarAguaHoy(porVaso)

        cancelarNotificacionAguaActiva(context)
        actualizarWidgetAguaSuspend(context)

        // Agregar uso a las métricas
        val kind = WidgetWaterKindResolver.resolve(context, glanceId)
        widgetUse(context, kind)

        // Reprogramar el recordatorio de agua solo si las notificaciones están activas
        if (context.flujoNotificacionesActivas().first()) {
            val frecuenciaMinutos = context.flujoFrecuenciaMinutos().first()
            programarRecordatorioAgua(context, frecuenciaMinutos)
        }
    }
}

class QuitarAguaCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val porVaso = context.flujoPorVaso().first()
        context.incrementarAguaHoy(-porVaso)

        cancelarNotificacionAguaActiva(context)
        actualizarWidgetAguaSuspend(context)

        // Agregar uso a las métricas
        val kind = WidgetWaterKindResolver.resolve(context, glanceId)
        AggregatesRepository(context).incrementWidgetUse(kind)

        // Reprogramar el recordatorio de agua solo si las notificaciones están activas
        if (context.flujoNotificacionesActivas().first()) {
            val frecuenciaMinutos = context.flujoFrecuenciaMinutos().first()
            programarRecordatorioAgua(context, frecuenciaMinutos)
        }
    }
}
