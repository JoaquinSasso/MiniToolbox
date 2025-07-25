package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.joasasso.minitoolbox.tools.data.flujoAguaHoy
import com.joasasso.minitoolbox.tools.data.flujoFrecuenciaMinutos
import com.joasasso.minitoolbox.tools.data.flujoObjetivo
import com.joasasso.minitoolbox.tools.data.flujoPorVaso
import com.joasasso.minitoolbox.tools.data.guardarAguaHoy
import kotlinx.coroutines.flow.first

class AgregarAguaCallback : ActionCallback {
    //Obtener el valor actual de agua desde el dataStore
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val porVaso = context.flujoPorVaso().first()
        val actual = context.flujoAguaHoy().first()
        val nuevo = actual + porVaso
        context.guardarAguaHoy(nuevo)

        //Actualizar los datos del widget
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[AguaWidget.KEY_AGUA] = nuevo
                this[AguaWidget.KEY_OBJETIVO] = context.flujoObjetivo().first()
                this[AguaWidget.KEY_POR_VASO] = porVaso
            }
        }
        AguaWidget().update(context, glanceId)

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

        //Actualizar los datos del widget
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[AguaWidget.KEY_AGUA] = nuevo
                this[AguaWidget.KEY_OBJETIVO] = context.flujoObjetivo().first()
                this[AguaWidget.KEY_POR_VASO] = porVaso
            }
        }
        AguaWidget().update(context, glanceId)

        //Reprogramar la notificacion para beber agua
        val frecuenciaMinutos = context.flujoFrecuenciaMinutos().first()
        val objetivo = context.flujoObjetivo().first()
        programarRecordatorioAgua(context, frecuenciaMinutos, nuevo, objetivo)
    }
}

