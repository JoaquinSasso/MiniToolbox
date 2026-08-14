package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Chequeo del permiso "Alarmas y recordatorios".
 *
 * - API < 31 (Android 11 o menor): no existe el permiso, siempre se puede.
 * - API >= 31: hace falta SCHEDULE_EXACT_ALARM (que concede el usuario)
 *   o USE_EXACT_ALARM (que se concede en la instalación pero está restringido
 *   por política de Google Play a apps de alarma / temporizador / calendario).
 *
 * canScheduleExactAlarms() devuelve true en ambos casos, así que este helper
 * sirve para las dos estrategias sin cambiar nada.
 */
object ExactAlarmPermission {

    /** ¿Podemos programar una alarma exacta en este preciso momento? */
    fun canSchedule(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(AlarmManager::class.java) ?: return false
        return am.canScheduleExactAlarms()
    }

    /**
     * Intent a la pantalla de Ajustes donde el usuario concede el permiso.
     * Sólo hace falta si vas por el camino SCHEDULE_EXACT_ALARM.
     * Devuelve null en versiones donde no aplica.
     */
    fun settingsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            .setData(Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
