package com.joasasso.minitoolbox.tools.herramientas.instrumentos

import android.graphics.Paint
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable

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
        topBar = {TopBarReusable(stringResource(R.string.tool_ruler), onBack, {showInfo = true})},
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
                                start = Offset(0f, y),
                                end = Offset(lineLength, y),
                                strokeWidth = if (isUnidad) 6f else 3f,
                                cap = StrokeCap.Round
                            )
                            if (isUnidad) {
                                drawContext.canvas.nativeCanvas.apply {
                                    val num = i / stepsPerUnidad
                                    drawText(
                                        "$num${if (isCm) " cm" else " in"}",
                                        lineLength + 8f,
                                        y + 12f,
                                        Paint().apply {
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
                            Text(
                                stringResource(
                                    if (unidad == "cm") R.string.ruler_switch_to_inches
                                    else R.string.ruler_switch_to_cm
                                )
                            )
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
            title = { Text(stringResource(R.string.ruler_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.ruler_help_line1))
                    Text(stringResource(R.string.ruler_help_line2))
                    Text(stringResource(R.string.ruler_help_line3))
                    Text(stringResource(R.string.ruler_help_line4))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showInfo = false
                }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}
