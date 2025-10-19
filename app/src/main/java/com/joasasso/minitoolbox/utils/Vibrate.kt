package com.joasasso.minitoolbox.utils

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

fun vibrate(context: Context, duration: Long = 300, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    vibrator?.vibrate(VibrationEffect.createOneShot(duration, amplitude))
}