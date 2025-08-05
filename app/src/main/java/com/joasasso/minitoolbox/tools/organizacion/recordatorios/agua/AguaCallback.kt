package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.joasasso.minitoolbox.data.flujoAguaHoy
import com.joasasso.minitoolbox.data.flujoFrecuenciaMinutos
import com.joasasso.minitoolbox.data.flujoObjetivo
import com.joasasso.minitoolbox.data.flujoPorVaso
import com.joasasso.minitoolbox.data.guardarAguaHoy
import kotlinx.coroutines.flow.first

class AgregarAguaCallback : ActionCallback {
    //Obtener el valor actual de agua desde el dataStore
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val porVaso = context.flujoPorVaso().first()
        val actual = context.flujoAguaHoy().first()
        val nuevo = actual + porVaso
        context.guardarAguaHoy(nuevo)

        actualizarWidgetAgua(context)

        //Reprogramar la notificacion para beber agua
        val frecuenciaMinutos = context.flujoFrecuenciaMinutos().first()
        val objetivo = context.flujoObjetivo().first()
        programarRecordatorioAgua(context, frecuenciaMinutos, nuevo, objetivo)
    }
}

class QuitarAguaCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        //Obtener el valor actual de agua desde el dataStore
        val porVaso = context.flujoPorVaso().first()
        val actual = context.flujoAguaHoy().first()
        val nuevo = (actual - porVaso).coerceAtLeast(0)
        context.guardarAguaHoy(nuevo)

        actualizarWidgetAgua(context)

        //Reprogramar la notificacion para beber agua
        val frecuenciaMinutos = context.flujoFrecuenciaMinutos().first()
        val objetivo = context.flujoObjetivo().first()
        programarRecordatorioAgua(context, frecuenciaMinutos, nuevo, objetivo)
    }
}

