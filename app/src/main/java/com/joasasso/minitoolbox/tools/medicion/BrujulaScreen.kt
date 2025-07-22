package com.joasasso.minitoolbox.tools.medicion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BrujulaScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showInfo by remember { mutableStateOf(false) }

    var azimuth by remember { mutableFloatStateOf(0f) }
    var lastAzimuth by remember { mutableFloatStateOf(0f) }

    // Sensor setup
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)


        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> event.values.copyInto(gravity)
                    Sensor.TYPE_MAGNETIC_FIELD -> event.values.copyInto(geomagnetic)
                }

                val R = FloatArray(9)
                val I = FloatArray(9)
                if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(R, orientation)
                    val newAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    val delta = abs(newAzimuth - lastAzimuth)

                    if (delta > 2.5f) {
                        azimuth = (newAzimuth + 360f) % 360f
                        lastAzimuth = azimuth
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
    var previousRotation by remember { mutableFloatStateOf(0f) }

    val animatedRotation by animateFloatAsState(
        targetValue = remember(azimuth) {
            val diff = (azimuth - previousRotation + 540f) % 360f - 180f
            val newRotation = previousRotation + diff
            previousRotation = newRotation
            newRotation
        },
        animationSpec = tween(
            durationMillis = 175,
            easing = FastOutSlowInEasing
        ),
        label = "smoothRotation"
    )

    fun getDirectionLabel(angle: Float): String {
        val normalized = (angle % 360 + 360) % 360
        return when (normalized) {
            in 0f..22.5f, in 337.5f..360f -> "${normalized.toInt()}° Norte"
            in 22.5f..67.5f -> "${normalized.toInt()}° Noreste"
            in 67.5f..112.5f -> "${normalized.toInt()}° Este"
            in 112.5f..157.5f -> "${normalized.toInt()}° Sureste"
            in 157.5f..202.5f -> "${normalized.toInt()}° Sur"
            in 202.5f..247.5f -> "${normalized.toInt()}° Suroeste"
            in 247.5f..292.5f -> "${normalized.toInt()}° Oeste"
            in 292.5f..337.5f -> "${normalized.toInt()}° Noroeste"
            else -> "${normalized.toInt()}°"
        }
    }




    Scaffold(
        topBar = { TopBarReusable("Brújula", onBack) { showInfo = true } },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()
                .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.Center,)
            {
                Text(
                    text = getDirectionLabel(animatedRotation),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
                // Cardinal points (fixed positions)
                val directions = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
                val angleStep = 360f / directions.size
                val radius = 120f

                directions.forEachIndexed { i, dir ->
                    val angleRad = Math.toRadians((i * angleStep + 90).toDouble())
                    val x = (radius * cos(angleRad)).dp
                    val y = (radius * sin(angleRad)).dp

                    Box(
                        modifier = Modifier
                            .offset(x, -y) // Negar Y para que "N" quede arriba
                            .align(Alignment.Center)
                    ) {
                        Text(text = dir, fontSize = 16.sp)
                    }
                }

                // Flecha que gira
                Image(
                    painter = painterResource(R.drawable.arrow_north),
                    contentDescription = "Flecha brújula",
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer {
                            rotationZ = -animatedRotation
                        }
                )
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Acerca de Brújula") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Para qué sirve: Muestra una brújula digital que indica la dirección norte.")
                    Text("• Usa los sensores del teléfono para detectar orientación y rotación.")
                    Text("• La flecha central apunta siempre al norte magnético.")
                    Text("• Las direcciones cardinales se mantienen fijas.")
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
