package com.example.minitoolbox.tools.medicion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReglaScreen(onBack: () -> Unit) {
    var unidad by remember { mutableStateOf("cm") }
    var showInfo by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val metrics = context.resources.displayMetrics

    // DPI según unidad
    val dpi = metrics.ydpi
    val factor = if (unidad == "cm") (dpi / 2.54f) else dpi // px por unidad
    val mainColor = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Regla") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showInfo = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }) {
                        Icon(Icons.Filled.Info, contentDescription = "Información")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Regla
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(90.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(90.dp)
                            .align(Alignment.TopStart)
                    ) {
                        val pxPorUnidad = factor
                        val totalPx = size.height
                        val stepsPerUnidad = if (unidad == "cm") 10 else 16
                        val isCm = unidad == "cm"
                        val unidadesVisibles = totalPx / pxPorUnidad

                        for (i in 0..(unidadesVisibles * stepsPerUnidad).toInt()) {
                            val y = i * pxPorUnidad / stepsPerUnidad
                            val isUnidad = i % stepsPerUnidad == 0
                            val isHalf = isCm && i % 5 == 0 && !isUnidad
                            val isQuarter = !isCm && i % 4 == 0 && !isUnidad
                            val lineLength = when {
                                isUnidad -> size.width * 0.88f
                                isHalf || isQuarter -> size.width * 0.58f
                                else -> size.width * 0.32f
                            }
                            drawLine(
                                color = mainColor,
                                start = androidx.compose.ui.geometry.Offset(0f, y),
                                end = androidx.compose.ui.geometry.Offset(lineLength, y),
                                strokeWidth = if (isUnidad) 6f else 3f,
                                cap = StrokeCap.Round
                            )
                            if (isUnidad) {
                                drawContext.canvas.nativeCanvas.apply {
                                    val num = i / stepsPerUnidad
                                    drawText(
                                        "$num${if (isCm) "cm" else "\""}",
                                        lineLength + 8f,
                                        y + 12f,
                                        android.graphics.Paint().apply {
                                            color = mainColor.toArgb()
                                            textSize = 34f
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                // Botones alineados arriba a la derecha
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, end = 8.dp)
                            .align(Alignment.TopEnd),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.Top
                    ) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                unidad = if (unidad == "cm") "inch" else "cm"
                            },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(if (unidad == "cm") "Cambiar a pulgadas" else "Cambiar a cm")
                        }
                    }
                }
            }
        }
    }

    //Menu de ayuda con información sobre la tool
    if (showInfo) {
        AlertDialog(
            onDismissRequest = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showInfo = false
            },
            title = { Text("¿Cómo funciona la regla?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Esta herramienta dibuja una regla vertical calibrada según la pantalla de tu dispositivo.")
                    Text("• Puedes medir objetos pequeños apoyándolos sobre la pantalla, junto al borde izquierdo.")
                    Text("• El botón de la esquina permite alternar entre centímetros y pulgadas.")
                    Text("• La precisión depende del tamaño físico reportado por tu dispositivo, por lo que puede variar ligeramente entre modelos.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showInfo = false
                }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
