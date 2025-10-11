// PomodoroTimerConfig.kt
package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import androidx.compose.ui.graphics.Color
import java.util.UUID

data class PomodoroTimerConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorArgb: Long,
    val workMin: Int,
    val shortBreakMin: Int,
    val longBreakMin: Int,
    val cyclesBeforeLong: Int
)

fun PomodoroTimerConfig.color(): Color = Color(colorArgb)
fun Color.toArgbLong(): Long = (this.value.toLong())
