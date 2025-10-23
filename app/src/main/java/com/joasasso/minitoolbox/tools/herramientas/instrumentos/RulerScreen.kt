package com.joasasso.minitoolbox.tools.herramientas.instrumentos

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private enum class UnitSys { CM, IN }
private enum class Phase { Idle, Measuring, Paused }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReglaScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val density = LocalDensity.current
    val dm = remember { context.resources.displayMetrics }

    var showInfo by remember { mutableStateOf(false) }
    var unit by remember { mutableStateOf(UnitSys.CM) }

    // UI
    var showSideRuler by remember { mutableStateOf(true) }
    val anchorColor = MaterialTheme.colorScheme.secondary
    val cursorColor = Color.Red
    val textColor = MaterialTheme.colorScheme.onBackground

    // Estado de flujo
    var phase by remember { mutableStateOf(Phase.Idle) }

    // Medición (solo a lo largo = eje Y)
    var totalPx by remember { mutableStateOf(0f) }
    var lastPos by remember { mutableStateOf<Offset?>(null) }
    var fingerPos by remember { mutableStateOf<Offset?>(null) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Ancla (línea INICIO)
    var anchorPos by remember { mutableStateOf<Offset?>(null) }

    // Hápticos por cm / 5 cm
    var lastHapticCm by remember { mutableStateOf(-1) }
    var lastHaptic5 by remember { mutableStateOf(-1) }

    // Conversión px -> cm/in
    val factorCalibracion = 1f // TODO: persistir en DataStore
    fun pxPerUnit(): Float {
        val ydpi = dm.ydpi.coerceAtLeast(1f)
        return if (unit == UnitSys.CM) ydpi / 2.54f else ydpi
    }
    fun pxToCm(px: Float): Float {
        val ydpi = dm.ydpi.coerceAtLeast(1f)
        return px * (2.54f / ydpi) * factorCalibracion
    }
    fun pxToIn(px: Float): Float {
        val ydpi = dm.ydpi.coerceAtLeast(1f)
        return (px / ydpi) * factorCalibracion
    }
    fun formattedValue(total: Float = totalPx): String {
        val value = if (unit == UnitSys.CM) pxToCm(total) else pxToIn(total)
        val num = if (unit == UnitSys.CM) String.format("%.2f", value) else String.format("%.3f", value)
        return "$num ${if (unit == UnitSys.CM) "cm" else "in"}"
    }

    Scaffold(
        topBar = {
            TopBarReusable(
                title = stringResource(R.string.tool_ruler),
                onBack = onBack,
                onShowInfo = { showInfo = true }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .onSizeChanged { containerSize = it }
        ) {
            // Regla lateral (útil para objetos pequeños)
            if (showSideRuler) {
                SideRuler(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .width(80.dp)
                        .padding(start = 4.dp),
                    unit = unit
                )
            }

            // Zona de medición (solo eje Y)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)

                            // Inicio de tramo
                            phase = Phase.Measuring
                            lastPos = down.position
                            fingerPos = down.position
                            anchorPos = down.position       // INICIO

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.find { it.id == down.id } ?: break

                                val pos = change.position
                                val delta = pos - (lastPos ?: pos)

                                // Solo ΔY
                                val projected = delta.y
                                totalPx += projected

                                // Haptics por múltiplos de cm / 5 cm
                                val cmNow = pxToCm(totalPx)
                                val wholeCm = floor(cmNow).toInt()
                                if (wholeCm > lastHapticCm) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    lastHapticCm = wholeCm
                                }
                                if (wholeCm / 5 > lastHaptic5) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    lastHaptic5 = wholeCm / 5
                                }

                                fingerPos = pos
                                lastPos = pos

                                change.consume()
                                if (!change.pressed) break
                            }

                            // Pausa (dedo levantado pero conserva acumulado)
                            phase = Phase.Paused
                            lastPos = null
                            fingerPos = null
                        }
                    }
            ) {
                // Dibujo: INICIO (verde), ACTUAL (roja), conector punteado central con ticks de 1 unidad
                Canvas(Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val centerX = width / 2f

                    // Líneas INICIO y ACTUAL
                    val startY = anchorPos?.y
                    val currentY = fingerPos?.y

                    // INICIO (verde) + etiqueta
                    startY?.let { y ->
                        drawLine(
                            color = anchorColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = with(density) { 2.dp.toPx() },
                            cap = StrokeCap.Round
                        )
                        // Etiqueta "INICIO"
                        drawContext.canvas.nativeCanvas.apply {
                            val txt = "INICIO"
                            val paint = Paint().apply {
                                color = anchorColor.toArgb()
                                textSize = with(density) { 12.sp.toPx() }
                                isAntiAlias = true
                            }
                            drawText(
                                txt,
                                12f,
                                y - 8f,
                                paint
                            )
                        }
                    }

                    // ACTUAL (roja) + etiqueta "FINAL"
                    currentY?.let { y ->
                        drawLine(
                            color = cursorColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = with(density) { 2.dp.toPx() },
                            cap = StrokeCap.Round
                        )
                        drawContext.canvas.nativeCanvas.apply {
                            val txt = "FINAL"
                            val paint = Paint().apply {
                                color = cursorColor.toArgb()
                                textSize = with(density) { 12.sp.toPx() }
                                isAntiAlias = true
                            }
                            drawText(
                                txt,
                                width - 12f - paint.measureText("FINAL"),
                                y - 8f,
                                paint
                            )
                        }
                    }

                    // Conector punteado central + ticks cada 1 unidad
                    if (startY != null && currentY != null) {
                        val top = min(startY, currentY)
                        val bottom = max(startY, currentY)
                        val dash = PathEffect.dashPathEffect(floatArrayOf(14f, 12f), 0f)

                        // Línea punteada vertical
                        drawLine(
                            color = textColor.copy(alpha = 0.65f),
                            start = Offset(centerX, top),
                            end = Offset(centerX, bottom),
                            strokeWidth = with(density) { 2.dp.toPx() },
                            cap = StrokeCap.Butt,
                            pathEffect = dash
                        )

                        // Ticks por unidad
                        val pxPer = pxPerUnit()
                        val distancePx = bottom - startY
                        val units = floor(distancePx / pxPer).toInt()
                        val tickHalf = with(density) { 10.dp.toPx() }
                        for (i in 1..units) {
                            val yTick = if (currentY >= startY) startY + i * pxPer else startY - i * pxPer
                            drawLine(
                                color = textColor.copy(alpha = 0.8f),
                                start = Offset(centerX - tickHalf, yTick),
                                end = Offset(centerX + tickHalf, yTick),
                                strokeWidth = with(density) { 2.dp.toPx() },
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // Hint de borde (arriba/abajo)
                    currentY?.let { y ->
                        val nearEdge = y < height * 0.1f || y > height * 0.9f
                        if (nearEdge) {
                            drawContext.canvas.nativeCanvas.apply {
                                val txt = "Levanta el dedo y vuelve a apoyar para seguir"
                                val paint = Paint().apply {
                                    color = Color.LightGray.toArgb()
                                    textSize = with(density) { 14.sp.toPx() }
                                    isAntiAlias = true
                                }
                                drawText(
                                    txt,
                                    width / 2f - paint.measureText(txt) / 2f,
                                    with(density) { 24.dp.toPx() },
                                    paint
                                )
                            }
                        }
                    }
                }

                // Lectura grande centrada
                Text(
                    text = formattedValue(),
                    color = textColor,
                    fontSize = 42.sp,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Mensajes contextuales
                when (phase) {
                    Phase.Idle -> {
                        CoachText(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp),
                            text = "1) Toca el INICIO del objeto\n2) Desliza hasta el FINAL\n3) Levanta y vuelve a apoyar para continuar"
                        )
                    }
                    Phase.Measuring -> {
                        CoachText(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp),
                            text = "Desliza hasta el FINAL del objeto"
                        )
                    }
                    Phase.Paused -> {
                        CoachText(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp),
                            text = "Toca para continuar (se suma)"
                        )
                    }
                }

                // Botonera inferior
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        totalPx = 0f
                        lastPos = null
                        fingerPos = null
                        anchorPos = null
                        lastHapticCm = -1
                        lastHaptic5 = -1
                        phase = Phase.Idle
                    }) {
                        Text(text = stringResource(R.string.reset))
                    }
                    OutlinedButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        unit = if (unit == UnitSys.CM) UnitSys.IN else UnitSys.CM
                    }) {
                        Text(
                            text = if (unit == UnitSys.CM)
                                stringResource(R.string.ruler_switch_to_inches)
                            else
                                stringResource(R.string.ruler_switch_to_cm)
                        )
                    }
                    OutlinedButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        clipboard.setText(AnnotatedString(formattedValue()))
                    }) {
                        Text(text = stringResource(R.string.copy))
                    }
                }
            }
        }
    }

    // Menú de ayuda
    if (showInfo) {
        AlertDialog(
            onDismissRequest = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showInfo = false
            },
            title = { Text(stringResource(R.string.ruler_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cómo usarla:")
                    Text("1) Alinea el teléfono junto al objeto.")
                    Text("2) Toca el INICIO del objeto (línea verde).")
                    Text("3) Desliza el teléfono hasta el FINAL (línea roja).")
                    Text("4) Si necesitas continuar, levanta y vuelve a apoyar.")
                    Text("Consejo: el movimiento lateral no afecta la medición (solo cuenta el desplazamiento vertical).")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showInfo = false
                }) { Text(stringResource(R.string.close)) }
            }
        )
    }
}

@Composable
private fun CoachText(modifier: Modifier, text: String) {
    Text(
        text = text,
        color = Color.LightGray,
        fontSize = 16.sp,
        modifier = modifier
    )
}

/** Regla vertical lateral (cm con 10 sub-divs, in con 16 sub-divs) */
@Composable
private fun SideRuler(
    modifier: Modifier,
    unit: UnitSys
) {
    val context = LocalContext.current
    val mainColor = MaterialTheme.colorScheme.primary
    val ydpi = context.resources.displayMetrics.ydpi

    Canvas(modifier = modifier) {
        val isCm = unit == UnitSys.CM
        val stepsPerUnit = if (isCm) 10 else 16
        val pxPerUnit = if (isCm) (ydpi / 2.54f) else ydpi
        val totalPx = size.height
        val unitsVisible = (totalPx / pxPerUnit).toInt() + 2

        for (u in 0..unitsVisible * stepsPerUnit) {
            val y = u * (pxPerUnit / stepsPerUnit)
            if (y > totalPx) break

            val isUnit = u % stepsPerUnit == 0
            val isHalf = isCm && u % 5 == 0 && !isUnit
            val isQuarter = !isCm && u % 4 == 0 && !isUnit

            val lineLength = when {
                isUnit -> size.width * 0.85f
                isHalf || isQuarter -> size.width * 0.55f
                else -> size.width * 0.30f
            }

            drawLine(
                color = mainColor,
                start = Offset(0f, y),
                end = Offset(lineLength, y),
                strokeWidth = if (isUnit) 6f else 3f,
                cap = StrokeCap.Round
            )

            if (isUnit) {
                drawContext.canvas.nativeCanvas.apply {
                    val num = u / stepsPerUnit
                    val label = if (isCm) "$num cm" else "$num in"
                    drawText(
                        label,
                        lineLength + 8f,
                        y + 12f,
                        Paint().apply {
                            color = mainColor.toArgb()
                            textSize = 34f
                            isAntiAlias = true
                        }
                    )
                }
            }
        }
    }
}
