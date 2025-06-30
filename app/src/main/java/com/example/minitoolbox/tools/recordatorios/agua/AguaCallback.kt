package com.example.minitoolbox.tools.recordatorios.agua

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.flow.first

class AgregarAguaCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val porVaso = context.flujoPorVaso().first()
        val actual = context.flujoAguaHoy().first()
        val nuevo = actual + porVaso
        context.guardarAguaHoy(nuevo)

        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[AguaWidget.KEY_AGUA] = nuevo
                this[AguaWidget.KEY_OBJETIVO] = context.flujoObjetivo().first()
                this[AguaWidget.KEY_POR_VASO] = porVaso
            }
        }

        AguaWidget().update(context, glanceId)
    }
}

class QuitarAguaCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val porVaso = context.flujoPorVaso().first()
        val actual = context.flujoAguaHoy().first()
        val nuevo = (actual - porVaso).coerceAtLeast(0)
        context.guardarAguaHoy(nuevo)

        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[AguaWidget.KEY_AGUA] = nuevo
                this[AguaWidget.KEY_OBJETIVO] = context.flujoObjetivo().first()
                this[AguaWidget.KEY_POR_VASO] = porVaso
            }
        }

        AguaWidget().update(context, glanceId)
    }
}

