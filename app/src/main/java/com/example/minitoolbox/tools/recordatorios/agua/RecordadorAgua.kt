package com.example.minitoolbox.tools.recordatorios.agua

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission

class WaterReminderReceiver : BroadcastReceiver() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent?) {
        // Obtén los extras enviados (o carga los datos desde DataStore/SharedPrefs)
        val consumidoML = intent?.getIntExtra("agua_consumida_ml", 0) ?: 0
        val objetivoML = intent?.getIntExtra("agua_objetivo_ml", 2000) ?: 2000

        createWaterReminderChannel(context)
        sendWaterReminderNotification(context, consumidoML, objetivoML)
    }
}
