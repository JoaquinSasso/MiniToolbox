package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ResetAguaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
                notificationManager?.cancel(101)

                actualizarWidgetAguaSuspend(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
