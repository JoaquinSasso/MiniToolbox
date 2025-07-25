// app/src/main/java/com/example/minitoolbox/tools/juegos/CoinFlipScreen.kt
package com.joasasso.minitoolbox.tools.entretenimiento.aleatorio

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinFlipScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vibrator = context.getSystemService(Vibrator::class.java)
    val haptic = LocalHapticFeedback.current
    var showInfo by remember { mutableStateOf(false) }

    var result by remember { mutableStateOf(context.getString(R.string.coin_heads)) }
    var flipping by remember { mutableStateOf(false) }
    val scaleY = remember { Animatable(1f) }
    var flipTrigger by remember { mutableIntStateOf(0) }
    var currentColor by remember { mutableStateOf(Color(0xFFBCAAA4)) }

    val flipColors = listOf(
        Color(0xFFBCAAA4), // dorado
        Color(0xFF90CAF9), // celeste
        Color(0xFFF48FB1), // rosa
        Color(0xFFA5D6A7), // verde
        Color(0xFFFFF59D)  // amarillo
    )

    LaunchedEffect(flipTrigger) {
        if (flipping) {
            vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            val flips = 4
            val durationPerFlip = 150
            repeat(flips) { i ->
                currentColor = flipColors[i % flipColors.size]
                scaleY.animateTo(0f, tween(durationPerFlip / 2, easing = LinearEasing))
                scaleY.animateTo(1f, tween(durationPerFlip / 2, easing = LinearEasing))
                vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            }
            val newResult = if (Random.nextBoolean()) {
                context.getString(R.string.coin_heads)
            } else {
                context.getString(R.string.coin_tails)
            }
            result = newResult
            currentColor = if (newResult == context.getString(R.string.coin_heads)) flipColors[0] else flipColors[1]
            flipping = false
            vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    Scaffold(
        topBar = {
            TopBarReusable(
                stringResource(R.string.tool_coin_flip),
                onBack,
                { showInfo = true }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .scale(1f, scaleY.value)
                    .clip(CircleShape)
                    .background(currentColor)
                    .clickable(enabled = !flipping) {
                        flipping = true
                        flipTrigger++
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!flipping) {
                    Text(
                        text = result,
                        fontSize = 64.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Button(
                    onClick = {
                        flipping = true
                        flipTrigger++
                    },
                    enabled = !flipping,
                    modifier = Modifier.height(75.dp)
                        .width(200.dp)
                ) {
                    Text(stringResource(R.string.coin_flip_button), style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = {
                showInfo = false
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            title = { Text(stringResource(R.string.coin_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.coin_help_line1))
                    Text(stringResource(R.string.coin_help_line2))
                    Text(stringResource(R.string.coin_help_line3))
                    Text(stringResource(R.string.coin_help_line4))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showInfo = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

