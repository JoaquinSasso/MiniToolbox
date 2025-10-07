// ui/utils/Orientation.kt
package com.joasasso.minitoolbox.ui.utils

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun LockScreenOrientationIfAllowed(
    orientation: Int = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
    enabled: Boolean = true
) {
    val ctx = LocalContext.current
    val activity = ctx.findActivity()

    DisposableEffect(activity, enabled, orientation) {
        val prev = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.requestedOrientation = orientation
        onDispose {
            activity?.requestedOrientation = prev
        }
    }
}
