package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Chequeo del permiso "Mostrar avisos de pantalla completa" (USE_FULL_SCREEN_INTENT).
 *
 * Desde Android 14, este permiso NO se autoconcede a apps cuya funcionalidad
 * central no sea llamadas o alarmas — el mismo criterio de política que
 * USE_EXACT_ALARM. Sin este permiso, `setFullScreenIntent()` no lanza
 * excepción (por eso el código anterior nunca lo notaba): simplemente el
 * sistema degrada la notificación a una normal, y el salto automático a
 * pantalla completa no ocurre nunca. Hay que pedírselo al usuario a mano.
 *
 * A diferencia de ExactAlarmPermission, este NO bloquea el arranque del
 * timer si falta: la alarma sigue sonando y la notificación sigue
 * funcionando igual, sólo que sin el salto automático — es una mejora de
 * experiencia, no algo esencial.
 */
object FullScreenIntentPermission {

    fun canUse(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        val nm = context.getSystemService(NotificationManager::class.java) ?: return false
        return nm.canUseFullScreenIntent()
    }

    /** Intent a la pantalla de Ajustes donde el usuario concede este permiso puntual. */
    fun settingsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        return Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
            .setData(Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
