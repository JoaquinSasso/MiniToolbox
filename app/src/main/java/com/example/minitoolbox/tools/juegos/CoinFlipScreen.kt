// app/src/main/java/com/example/minitoolbox/tools/juegos/CoinFlipScreen.kt
package com.example.minitoolbox.tools.juegos

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinFlipScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vibrator = context.getSystemService(Vibrator::class.java)
    var showInfo by remember { mutableStateOf(false) }

    var result by remember { mutableStateOf("Cara") }
    var flipping by remember { mutableStateOf(false) }
    val scaleY = remember { Animatable(1f) }
    var flipTrigger by remember { mutableStateOf(0) }
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
            result = if (Random.nextBoolean()) "Cara" else "Cruz"
            currentColor = if (result == "Cara") flipColors.first() else flipColors[1]
            flipping = false
            vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cara o Cruz") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showInfo = true }) {
                        Icon(Icons.Filled.Info, contentDescription = "Información")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
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
                    enabled = !flipping
                ) {
                    Text("Lanzar")
                }
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Acerca de Cara o Cruz") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Para qué sirve: Realiza un lanzamiento de moneda virtual con animación y vibración.")
                    Text("• Guía rápida:")
                    Text("   – Pulsa sobre la moneda o el botón “Lanzar” para iniciar el giro.")
                    Text("   – Durante la animación, el botón queda deshabilitado.")
                    Text("   – Al terminar, verás el resultado de la tirada: “Cara” o “Cruz”")
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
