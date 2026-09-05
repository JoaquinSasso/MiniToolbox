package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "PomodoroBootReceiver"

/**
 * Reprograma la alarma pendiente cuando el sistema la borra por debajo.
 *
 * Android descarta TODAS las alarmas de una app en estos casos:
 *  - reboot
 *  - actualización de la app
 *  - revocación del permiso de alarmas exactas
 *
 * Y las alarmas RTC quedan desalineadas si el usuario cambia la hora o la zona
 * horaria. Sin este receiver, cualquiera de esas situaciones dejaba el pomodoro
 * corriendo en la UI pero muerto en el sistema.
 */
class PomodoroBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" -> Unit
            else -> return
        }
        Log.d(TAG, "onReceive: action=${intent.action}, reprogramando si hay pomodoro pendiente")
        val pendingResult = goAsync()
        PomodoroAlarmReceiver.rescheduleFromPersisted(context.applicationContext, pendingResult)
    }
}