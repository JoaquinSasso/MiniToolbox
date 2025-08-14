package com.joasasso.minitoolbox.tools.herramientas.instrumentos

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.min
import kotlin.math.sqrt

enum class OrientationMode { FLAT, PORTRAIT, LANDSCAPE }

fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BubbleLevelScreen(onBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var showInfo by remember { mutableStateOf(false) }
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val rotationSensor = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }

    // Colores
    val guideColor = MaterialTheme.colorScheme.primary
    val smallerGuideColor = Color(0xFF00FF1B)
    val normalColor = MaterialTheme.colorScheme.onBackground
    val leveledColor = Color(0xFF4CAF50)

    // Estado de bloqueo (hold)
    var isLocked by remember { mutableStateOf(false) }
    var lockedBubbleOffset by remember { mutableStateOf(Offset.Zero) }
    var lockedInclinationDegX by remember { mutableFloatStateOf(0f) }
    var lockedInclinationDegY by remember { mutableFloatStateOf(0f) }

    // Estados
    var bubbleOffset by remember { mutableStateOf(Offset.Zero) }
    var rawBubbleOffset by remember { mutableStateOf(Offset.Zero) }
    var currentMode by remember { mutableStateOf(OrientationMode.PORTRAIT) }
    var inclinationDegX by remember { mutableFloatStateOf(0f) }
    var inclinationDegY by remember { mutableFloatStateOf(0f) }

    val axisThresh = 0.01f
    val flatRatio = 0.75f
    val orientAngle = 40f
    val stretcher = 1.75f

    // Beep
    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    var isForeground by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasSensorSample by remember { mutableStateOf(false) }
    var isBeepArmed by remember { mutableStateOf(false) }

    // El valor que realmente se usa (puede estar congelado)
    val displayBubbleOffset = if (isLocked) lockedBubbleOffset else bubbleOffset
    val displayInclinationDegX = if (isLocked) lockedInclinationDegX else inclinationDegX
    val displayInclinationDegY = if (isLocked) lockedInclinationDegY else inclinationDegY

    // Valores que determinan si la burbuja está nivelada en cada modo
    val isLevel by remember(currentMode, displayBubbleOffset, axisThresh) {
        derivedStateOf {
            when (currentMode) {
                OrientationMode.FLAT -> sqrt(displayBubbleOffset.x * displayBubbleOffset.x + displayBubbleOffset.y * displayBubbleOffset.y) <= axisThresh
                OrientationMode.PORTRAIT -> abs(displayBubbleOffset.x) <= axisThresh
                OrientationMode.LANDSCAPE -> abs(displayBubbleOffset.y) <= axisThresh
            }
        }
    }

    // Se comprueba si la app no está en primer plano (background) para evitar reproducir el pitido
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isForeground = event == Lifecycle.Event.ON_RESUME
        }
        val lifecycle = lifecycleOwner.lifecycle
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // Se reproduce el pitido si la burbuja está nivelada
    LaunchedEffect(isLevel, hasSensorSample, isLocked, isForeground) {
        if (!hasSensorSample || !isForeground || isLocked) {
            isBeepArmed = false
            return@LaunchedEffect
        }

        if (isLevel) {
            // Esperar un poquito y re-chequear que siga nivelado (debounce)
            isBeepArmed = true
            kotlinx.coroutines.delay(300)
            // si en ese tiempo se desarmó, no beep
            if (!(isBeepArmed && hasSensorSample && isForeground && !isLocked)) return@LaunchedEffect

            // Ahora sí: bucle de beep mientras permanezca nivelado
            while (isForeground && !isLocked && hasSensorSample) {
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                kotlinx.coroutines.delay(250)
            }
        } else {
            isBeepArmed = false
        }
    }

    // Suavizado animado de burbuja
    // Suavizado solo si NO está bloqueado
    LaunchedEffect(rawBubbleOffset) {
        if (isLocked) return@LaunchedEffect
        val steps = 6
        val delayMs = 8L
        repeat(steps) {
            bubbleOffset = Offset(
                lerp(bubbleOffset.x, rawBubbleOffset.x, 0.35f),
                lerp(bubbleOffset.y, rawBubbleOffset.y, 0.35f)
            )
            delay(delayMs)
        }
        bubbleOffset = rawBubbleOffset
    }

// Cuando bloqueás, guarda el valor SUAVIZADO al instante
    LaunchedEffect(isLocked) {
        if (isLocked) {
            lockedBubbleOffset = bubbleOffset
            lockedInclinationDegX = inclinationDegX
            lockedInclinationDegY = inclinationDegY
        }
    }


    // SENSADO
    DisposableEffect(sensorManager, rotationSensor) {
        val rotationMatrix = FloatArray(9)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Convierte el vector de rotación (cuaternión) a matriz de rotación
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                // El vector "arriba" del dispositivo en coordenadas del mundo (proyección en el plano XY)
                val upX = -rotationMatrix[6]
                val upY = -rotationMatrix[7]
                val upZ = -rotationMatrix[8]

                val gm = sqrt(upX * upX + upY * upY + upZ * upZ)
                val flat = gm > 0f && abs(upZ) / gm > flatRatio
                val degX = Math.toDegrees(asin(upX.toDouble())).toFloat()
                val degY = Math.toDegrees(asin(upY.toDouble())).toFloat()

                val landscape = !flat && (abs(degX) > orientAngle)
                val mode = when {
                    flat -> OrientationMode.FLAT
                    landscape -> OrientationMode.LANDSCAPE
                    else -> OrientationMode.PORTRAIT
                }
                currentMode = mode

                // Offset (normalizado a -1..1)
                val newBubbleOffset = when (mode) {
                    OrientationMode.FLAT -> Offset(upX.coerceIn(-1f, 1f), upY.coerceIn(-1f, 1f))
                    OrientationMode.PORTRAIT -> Offset(upX.coerceIn(-1f, 1f), 0f)
                    OrientationMode.LANDSCAPE -> Offset(0f, upY.coerceIn(-1f, 1f))
                }

                if (!isLocked) {
                    rawBubbleOffset = newBubbleOffset
                    inclinationDegX = degX
                    inclinationDegY = degY
                    hasSensorSample = true
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            sensorManager.unregisterListener(listener)
            toneGen.release()
        }
    }

    // Al bloquear, guardamos la posición "congelada"
    LaunchedEffect(isLocked) {
        if (isLocked) {
            lockedBubbleOffset = bubbleOffset
            lockedInclinationDegX = inclinationDegX
            lockedInclinationDegY = inclinationDegY
        }
    }

    Scaffold(
        topBar = { TopBarReusable(stringResource(R.string.tool_bubble_level), onBack, { showInfo = true }) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { isLocked = !isLocked }
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = stringResource(if (isLocked) R.string.bubble_unlock else R.string.bubble_lock),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(if (isLocked) R.string.bubble_unlock else R.string.bubble_lock),
                        fontSize = 16.sp
                    )
                }
            }
        }
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val bubbleR = 20.dp.toPx()

                    when (currentMode) {
                        // Modo plano (flat), modo que ocurre cuando el telefono esta con la pantalla hacia arriba o abajo
                        OrientationMode.FLAT -> {
                            val radius = min(w, h) / 3f
                            val center = Offset(w / 2f, h / 2f)

                            // Anillos concéntricos
                            drawCircle(
                                color = guideColor,
                                radius = radius,
                                center = center,
                                style = Stroke(width = 4.dp.toPx())
                            )
                            drawCircle(
                                color = smallerGuideColor,
                                radius = radius / 5f,
                                center = center,
                                style = Stroke(width = 2.dp.toPx())
                            )
                            drawCircle(
                                color = smallerGuideColor,
                                radius = radius / 2f,
                                center = center,
                                style = Stroke(width = 2.dp.toPx())
                            )
                            drawCircle(
                                color = smallerGuideColor,
                                radius = radius / 1.3f,
                                center = center,
                                style = Stroke(width = 2.dp.toPx())
                            )

                            // Línea horizontal (centro)
                            drawLine(
                                color = smallerGuideColor,
                                start = Offset(center.x - radius, center.y),
                                end = Offset(center.x + radius, center.y),
                                strokeWidth = 2.dp.toPx()
                            )
                            // Línea vertical (centro)
                            drawLine(
                                color = smallerGuideColor,
                                start = Offset(center.x, center.y - radius),
                                end = Offset(center.x, center.y + radius),
                                strokeWidth = 2.dp.toPx()
                            )

                            // Burbuja
                            val x = center.x + displayBubbleOffset.x * (radius - bubbleR)
                            val y = center.y + displayBubbleOffset.y * (radius - bubbleR)
                            drawCircle(
                                color = if (isLevel) leveledColor else normalColor,
                                radius = bubbleR, center = Offset(x, y),
                                alpha = 0.7f
                            )
                        }

                        // Modo vertical (portrait), ocurre cuando el telefono tiene el puerto de carga hacia abajo o arriba
                        OrientationMode.PORTRAIT -> {
                            val rectW = w * 0.8f
                            val rectH = 40.dp.toPx()
                            val left = (w - rectW) / 2f
                            val top = (h - rectH) / 2f
                            val rr = CornerRadius(x = rectH / 2f, y = rectH / 2f)
                            // Guia mayor
                            drawRoundRect(
                                color = guideColor,
                                topLeft = Offset(left, top),
                                size = Size(rectW, rectH),
                                cornerRadius = rr,
                                style = Stroke(width = 4.dp.toPx())
                            )
                            // Guia menor (centrada)
                            val minorW = rectW / 6f
                            val minorLeft = left + (rectW - minorW) / 2f
                            drawRoundRect(
                                color = smallerGuideColor,
                                topLeft = Offset(minorLeft, top),
                                size = Size(minorW, rectH),
                                cornerRadius = rr,
                                style = Stroke(width = 3.dp.toPx())
                            )
                            // Burbuja
                            val x =
                                left + rectW / 2f + (displayBubbleOffset.x * (rectW / 2f - bubbleR)) * stretcher
                            val y = top + rectH / 2f
                            drawCircle(
                                color = if (isLevel) leveledColor else normalColor,
                                radius = bubbleR, center = Offset(x, y),
                                alpha = 0.7f
                            )
                        }

                        // Modo horizontal (landscape), ocurre cuando el telefono tiene el puerto de carga hacia la derecha o izquierda
                        OrientationMode.LANDSCAPE -> {
                            val rectH = h * 0.8f
                            val rectW = 40.dp.toPx()
                            val left = (w - rectW) / 2f
                            val top = (h - rectH) / 2f
                            val rr = CornerRadius(x = rectW / 2f, y = rectW / 2f)
                            // Guia mayor
                            drawRoundRect(
                                color = guideColor,
                                topLeft = Offset(left, top),
                                size = Size(rectW, rectH),
                                cornerRadius = rr,
                                style = Stroke(width = 4.dp.toPx())
                            )
                            // Guia menor (centrada)
                            val minorH = rectH / 10f
                            val minorTop = top + (rectH - minorH) / 2f
                            drawRoundRect(
                                color = smallerGuideColor,
                                topLeft = Offset(left, minorTop),
                                size = Size(rectW, minorH),
                                cornerRadius = rr,
                                style = Stroke(width = 3.dp.toPx())
                            )
                            // Burbuja
                            val x = left + rectW / 2f
                            val y =
                                top + rectH / 2f + displayBubbleOffset.y * (rectH / 2f - bubbleR)
                            drawCircle(
                                color = if (isLevel) leveledColor else normalColor,
                                radius = bubbleR, center = Offset(x, y),
                                alpha = 0.7f
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                val landscapeRotation = when {
                    currentMode == OrientationMode.LANDSCAPE && displayInclinationDegX > orientAngle -> -90f
                    currentMode == OrientationMode.LANDSCAPE && displayInclinationDegX < -orientAngle -> 90f
                    else -> 0f
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.graphicsLayer(rotationZ = landscapeRotation)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("X: ${"%.2f".format(displayInclinationDegX)}°", fontSize = 22.sp)
                        Text("Y: ${"%.2f".format(displayInclinationDegY)}°", fontSize = 22.sp)
                    }
                }

                Spacer(Modifier.height(100.dp))
            }
        }

        //Menu de ayuda con información sobre la tool
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.bubble_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.bubble_help_line1))
                    Text(stringResource(R.string.bubble_help_line2))
                    Text(stringResource(R.string.bubble_help_line3))
                    Text(stringResource(R.string.bubble_help_line4))
                    Text(stringResource(R.string.bubble_help_line5))
                    Text(stringResource(R.string.bubble_help_line6))
                    Text(stringResource(R.string.bubble_help_line7))
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

