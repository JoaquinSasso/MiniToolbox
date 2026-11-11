package com.joasasso.minitoolbox.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * `POST_NOTIFICATIONS` existe desde API 33. En API 28-32 las notificaciones no
 * requieren permiso en runtime: pedirlo devuelve "denegado" siempre, porque el sistema
 * no conoce el permiso.
 */
object NotificationPermission {

    /** true solo si el sistema exige el permiso y todavía no está concedido. */
    fun shouldRequest(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
}
