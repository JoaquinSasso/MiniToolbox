package com.joasasso.minitoolbox.tools.generadores

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlin.random.Random


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomColorGeneratorScreen(onBack: () -> Unit) {
    var color by remember { mutableStateOf(generateRandomColor()) }
    val clipboardManager = LocalClipboardManager.current
    val hex = color.toHex()
    val contrastColor = if (color.isDark()) Color.White else Color.Black
    val haptic = LocalHapticFeedback.current
    var showInfo    by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {TopBarReusable(stringResource(R.string.tool_color_generator), onBack, {showInfo = true})},
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
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Acerca de Generador de Colores") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Para qué sirve: Crea de un solo toque un color aleatorio y te muestra su código hexadecimal.")
                    Text("• Guía rápida:")
                    Text("   – Pulsa “Generar nuevo” para cambiar el color de fondo.")
                    Text("   – Pulsa el boton de copiar para llevar el código hexadecimal al portapapeles.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showInfo = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) {
                    Text("Cerrar")
                }
            }
        )
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
