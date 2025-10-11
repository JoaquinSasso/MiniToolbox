package com.joasasso.minitoolbox.ui.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

fun getContrastingTextColor(bg: Color): Color {
    return if (bg.luminance() > 0.5f) Color.Black else Color.White
}