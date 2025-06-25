package com.example.minitoolbox.tools.medicion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlin.math.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

enum class OrientationMode { FLAT, PORTRAIT, LANDSCAPE }

fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BubbleLevelScreen(onBack: () -> Unit) {
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
    val smallerGuideColor = Color(0xFF034907)
    val normalColor = MaterialTheme.colorScheme.onBackground
    val leveledColor = Color(0xFF4CAF50)

    // Estado de bloqueo (hold)
    var isLocked by remember { mutableStateOf(false) }
    var lockedBubbleOffset by remember { mutableStateOf(Offset.Zero) }
    var lockedInclinationDegX by remember { mutableStateOf(0f) }
    var lockedInclinationDegY by remember { mutableStateOf(0f) }

    // Estados
    var bubbleOffset by remember { mutableStateOf(Offset.Zero) }
    var rawBubbleOffset by remember { mutableStateOf(Offset.Zero) }
    var currentMode by remember { mutableStateOf(OrientationMode.PORTRAIT) }
    var inclinationX by remember { mutableStateOf(0f) }
    var inclinationY by remember { mutableStateOf(0f) }
    var inclinationDegX by remember { mutableStateOf(0f) }
    var inclinationDegY by remember { mutableStateOf(0f) }

    val axisThresh = 0.01f
    val flatRatio = 0.75f
    val orientAngle = 40f
    val stretcher = 1.75f

    // Beep
    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    var isForeground by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // El valor que realmente se usa (puede estar congelado)
    val displayBubbleOffset = if (isLocked) lockedBubbleOffset else bubbleOffset
    val displayInclinationDegX = if (isLocked) lockedInclinationDegX else inclinationDegX
    val displayInclinationDegY = if (isLocked) lockedInclinationDegY else inclinationDegY

    val isLevel by derivedStateOf {
        when (currentMode) {
            OrientationMode.FLAT      -> sqrt(displayBubbleOffset.x * displayBubbleOffset.x + displayBubbleOffset.y * displayBubbleOffset.y) <= axisThresh
            OrientationMode.PORTRAIT  -> abs(displayBubbleOffset.x) <= axisThresh
            OrientationMode.LANDSCAPE -> abs(displayBubbleOffset.y) <= axisThresh
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isForeground = event == Lifecycle.Event.ON_RESUME
        }
        val lifecycle = lifecycleOwner.lifecycle
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isLevel, isForeground, isLocked) {
        while (isLevel && isForeground && !isLocked) {
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            delay(250)
        }
    }

    // Suavizado animado de burbuja
    LaunchedEffect(rawBubbleOffset, isLocked) {
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
                    inclinationX = upX
                    inclinationY = upY
                    inclinationDegX = degX
                    inclinationDegY = degY
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
        topBar = {
            TopAppBar(
                title = { Text("Nivel Burbuja") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Botón de hold/bloqueo
                    IconButton(onClick = { isLocked = !isLocked }) {
                        Icon(
                            imageVector = if (isLocked) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (isLocked) "Desbloquear burbuja" else "Bloquear burbuja"
                        )
                    }
                    IconButton(onClick = { showInfo = true }) {
                        Icon(Icons.Filled.Info, contentDescription = "Información")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) { padding ->
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
                        val y = top + rectH / 2f + displayBubbleOffset.y * (rectH / 2f - bubbleR)
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
                currentMode == OrientationMode.LANDSCAPE && displayInclinationDegX > orientAngle  ->  -90f
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

            Spacer(Modifier.height(25.dp))
        }
    }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Acerca de Nivel Burbuja") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Para qué sirve: Permite ver la inclinación horizontal y vertical del dispositivo, como un nivel de burbuja real.")
                    Text("• Guía rápida:")
                    Text("   – Coloca el dispositivo sobre la superficie que quieres nivelar.")
                    Text("   – Ajusta la inclinación de la superficie. Cuando escuches un pitido y la burbuja se ponga verde, significa que está nivelada.")
                    Text("   – Puedes utilizar los modos Flat, Portrait y Landscape para medir inclinación en diferentes posiciones del dispositivo.")
                    Text("   – Flat (plano) detecta nivel en ambas direcciones.")
                    Text("   – Portrait y Landscape miden nivel en una sola dirección.")
                    Text("   – Usa el botón con el icono de pausa para congelar/retener la burbuja y los valores.")
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
