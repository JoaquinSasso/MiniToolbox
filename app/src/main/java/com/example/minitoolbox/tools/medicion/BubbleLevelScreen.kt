// BubbleLevelScreen.kt
package com.example.minitoolbox.tools.medicion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.*
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info


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
    val accelerometer = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    // Colores
    val guideColor = MaterialTheme.colorScheme.primary
    val normalColor = MaterialTheme.colorScheme.onBackground
    val leveledColor = Color(0xFF4CAF50)

    // Estados
    var gravityX by remember { mutableStateOf(0f) }
    var gravityY by remember { mutableStateOf(0f) }
    var gravityZ by remember { mutableStateOf(0f) }
    var rollDeg by remember { mutableStateOf(0f) }
    var pitchDeg by remember { mutableStateOf(0f) }
    var prevRollDeg by remember { mutableStateOf(0f) }
    var prevPitchDeg by remember { mutableStateOf(0f) }
    var currentMode by remember { mutableStateOf(OrientationMode.PORTRAIT) }

    // Burbuja "visible" (suavizada) y "real" (target)
    var bubbleOffset by remember { mutableStateOf(Offset.Zero) }
    var rawBubbleOffset by remember { mutableStateOf(Offset.Zero) }

    val alpha = 0.8f
    val axisThresh = 0.015f
    val flatRatio = 0.7f      // Ajusta si querés que sea más o menos sensible el modo plano
    val orientAngle = 50f     // Ángulo de umbral entre portrait y landscape
    val stretcher = 1.75f      //Aumenta la sensibilidad en el modo portrait para que la burbuja llegue mas cerca del borde

    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }


    fun gMag() = sqrt(gravityX * gravityX + gravityY * gravityY + gravityZ * gravityZ)

    // Normaliza ángulos a [-180,180]
    fun normalizeAngle(deg: Float): Float {
        var a = deg % 360f
        if (a > 180f) a -= 360f
        if (a < -180f) a += 360f
        return a
    }

    // Interpola para evitar saltos bruscos en el paso ±180°
    fun smoothAngle(prev: Float, current: Float): Float {
        val delta = normalizeAngle(current - prev)
        return prev + delta
    }

    val isLevel by derivedStateOf {
        when (currentMode) {
            OrientationMode.FLAT -> sqrt(bubbleOffset.x * bubbleOffset.x + bubbleOffset.y * bubbleOffset.y) <= axisThresh
            OrientationMode.PORTRAIT -> abs(bubbleOffset.x) <= axisThresh
            OrientationMode.LANDSCAPE -> abs(bubbleOffset.y) <= axisThresh
        }
    }

    LaunchedEffect(isLevel) {
        while (isLevel) {
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            // Espera un poco antes de repetir (ajusta el intervalo a gusto)
            delay(250)
        }
    }



    // Suavizado de burbuja (interpolación animada)
    LaunchedEffect(rawBubbleOffset) {
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

    DisposableEffect(sensorManager, accelerometer) {
        var firstEvent = true
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

                // Inicializa gravity rápido al arrancar
                if (firstEvent) {
                    gravityX = event.values[0]
                    gravityY = event.values[1]
                    gravityZ = event.values[2]
                    firstEvent = false
                } else {
                    gravityX = alpha * gravityX + (1 - alpha) * event.values[0]
                    gravityY = alpha * gravityY + (1 - alpha) * event.values[1]
                    gravityZ = alpha * gravityZ + (1 - alpha) * event.values[2]
                }

                // Roll/pitch en radianes
                val rollRad = atan2(gravityY, gravityZ)
                val pitchRad = atan2(
                    -gravityX,
                    sqrt(gravityY * gravityY + gravityZ * gravityZ)
                )

                // A grados
                val rawRoll = Math.toDegrees(rollRad.toDouble()).toFloat()
                val rawPitch = Math.toDegrees(pitchRad.toDouble()).toFloat()

                // Normalizar y suavizar para evitar saltos
                val normRoll = normalizeAngle(rawRoll)
                val normPitch = normalizeAngle(rawPitch)
                rollDeg = smoothAngle(prevRollDeg, normRoll)
                pitchDeg = smoothAngle(prevPitchDeg, normPitch)
                prevRollDeg = rollDeg
                prevPitchDeg = pitchDeg

                // Calculo del modo: SOLO UNO activo a la vez, con prioridad a flat
                val gm = sqrt(gravityX * gravityX + gravityY * gravityY + gravityZ * gravityZ)
                val flat = gm > 0f && abs(gravityZ) / gm > flatRatio
                val landscape = !flat && (abs(pitchDeg) > orientAngle)
                val mode = when {
                    flat -> OrientationMode.FLAT
                    landscape -> OrientationMode.LANDSCAPE
                    else -> OrientationMode.PORTRAIT
                }
                currentMode = mode

                // Calculo del offset según modo (rawBubbleOffset, para suavizar luego)
                when (mode) {
                    OrientationMode.FLAT -> {
                        // x = pitch (adelante/atrás), y = roll (izquierda/derecha)
                        val x = (pitchRad / (PI/4)).toFloat().coerceIn(-1f, 1f)
                        val y = (rollRad / (PI/4)).toFloat().coerceIn(-1f, 1f)
                        rawBubbleOffset = Offset(x, y)
                    }
                    OrientationMode.PORTRAIT -> {
                        // Solo se mueve horizontal: pitch
                        val x = (pitchRad / (PI/2)).toFloat().coerceIn(-1f, 1f)
                        rawBubbleOffset = Offset(x, 0f)
                    }
                    OrientationMode.LANDSCAPE -> {
                        // Solo se mueve vertical: roll
                        val y = (rollRad / (PI/2)).toFloat().coerceIn(-1f, 1f)
                        rawBubbleOffset = Offset(0f, y)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener)
            sensorManager.unregisterListener(listener)
            toneGen.release()}
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
                        drawCircle(
                            color = guideColor,
                            radius = radius,
                            center = Offset(w / 2f, h / 2f),
                            style = Stroke(width = 4.dp.toPx())
                        )
                        val x = w / 2f - bubbleOffset.x * (radius - bubbleR)
                        val y = h / 2f - bubbleOffset.y * (radius - bubbleR)
                        drawCircle(
                            color = if (isLevel) leveledColor else normalColor,
                            radius = bubbleR, center = Offset(x, y)
                        )
                    }
                    OrientationMode.PORTRAIT -> {
                        val rectW = w * 0.8f
                        val rectH = 40.dp.toPx()
                        val left = (w - rectW) / 2f
                        val top = (h - rectH) / 2f
                        val rr = CornerRadius(x = rectH / 2f, y = rectH / 2f)
                        drawRoundRect(
                            color = guideColor,
                            topLeft = Offset(left, top),
                            size = Size(rectW, rectH),
                            cornerRadius = rr,
                            style = Stroke(width = 4.dp.toPx())
                        )
                        val x = left + rectW / 2f - (bubbleOffset.x * (rectW / 2f - bubbleR)) * stretcher
                        val y = top + rectH / 2f
                        drawCircle(
                            color = if (isLevel) leveledColor else normalColor,
                            radius = bubbleR, center = Offset(x, y)
                        )
                    }
                    OrientationMode.LANDSCAPE -> {
                        val rectH = h * 0.8f
                        val rectW = 40.dp.toPx()
                        val left = (w - rectW) / 2f
                        val top = (h - rectH) / 2f
                        val rr = CornerRadius(x = rectW / 2f, y = rectW / 2f)
                        drawRoundRect(
                            color = guideColor,
                            topLeft = Offset(left, top),
                            size = Size(rectW, rectH),
                            cornerRadius = rr,
                            style = Stroke(width = 4.dp.toPx())
                        )
                        val x = left + rectW / 2f
                        val y = top + rectH / 2f - bubbleOffset.y * (rectH / 2f - bubbleR)
                        drawCircle(
                            color = if (isLevel) leveledColor else normalColor,
                            radius = bubbleR, center = Offset(x, y)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Inclinación horizontal: ${"%.1f".format(pitchDeg)}°")
                Text("Inclinación vertical:   ${"%.1f".format(rollDeg)}°")
            }
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

