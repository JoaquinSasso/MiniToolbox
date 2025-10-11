package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.UUID

data class PomodoroTimerConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorInt: Int,            // ← ARGB Int
    val workMin: Int,
    val shortBreakMin: Int,
    val longBreakMin: Int,
    val cyclesBeforeLong: Int
)

// Helpers
fun PomodoroTimerConfig.color(): Color = Color(colorInt)
fun Color.toArgbInt(): Int = this.toArgb()