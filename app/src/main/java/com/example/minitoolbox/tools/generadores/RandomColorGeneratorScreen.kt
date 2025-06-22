package com.example.minitoolbox.tools.generadores

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalHapticFeedback



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomColorGeneratorScreen(onBack: () -> Unit) {
    var color by remember { mutableStateOf(generateRandomColor()) }
    val clipboardManager = LocalClipboardManager.current
    val textColor = if (color.isDark()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary
    val iconTint = textColor
    val hex = color.toHex()
    val contrastColor = if (color.isDark()) Color.White else Color.Black
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generador de colores") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color)
                .padding(32.dp)
                .padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = hex,
                style = MaterialTheme.typography.headlineMedium,
                color = contrastColor // Contraste blanco o negro
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    color = generateRandomColor()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                          },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) { Text("Generar nuevo") }
            Spacer(Modifier.height(16.dp))
            IconButton(onClick = {
                clipboardManager.setText(AnnotatedString(hex))
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = contrastColor)
            }
        }
    }
}


fun generateRandomColor(): Color {
    return Color(
        red = Random.nextFloat(),
        green = Random.nextFloat(),
        blue = Random.nextFloat(),
        alpha = 1f
    )
}

fun Color.toHex(): String {
    return String.format(
        "#%02X%02X%02X",
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}

fun Color.isDark(): Boolean {
    val luminance = 0.299 * red + 0.587 * green + 0.114 * blue
    return luminance < 0.5
}
