package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ResetAguaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                cancelarNotificacionAguaActiva(context)
                actualizarWidgetAguaSuspend(context)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("ResetAguaReceiver", "Error al reiniciar widget de agua", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
