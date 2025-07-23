package com.joasasso.minitoolbox.tools.medicion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LightSensorScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showInfo by remember { mutableStateOf(false) }
    var lux by remember { mutableStateOf<Float?>(null) }
    var maxLux by remember { mutableFloatStateOf(0f) }
    var sensorDisponible by remember { mutableStateOf(true) }

    // Sensor
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                lux = event.values.firstOrNull()
                lux?.let { if (it > maxLux) maxLux = it }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (lightSensor != null) {
            sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            sensorDisponible = false
        }
        onDispose {
            if (lightSensor != null)
                sensorManager.unregisterListener(listener, lightSensor)
        }
    }

    Scaffold(
        topBar = {TopBarReusable(stringResource(R.string.tool_light_meter), onBack, {showInfo = true})}
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!sensorDisponible) {
                    Text(
                        "Este dispositivo no tiene sensor de luz ambiental.",
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        "Nivel de luz actual",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        lux?.let { "%.0f lux".format(it) } ?: "...",
                        fontSize = 46.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Máximo detectado: ${if (maxLux > 0f) "%.0f lux".format(maxLux) else "..."}",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            maxLux = lux ?: 0f
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reiniciar máximo")
                        Spacer(Modifier.width(8.dp))
                        Text("Reiniciar máximo")
                    }
                }
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = {
                showInfo = false
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            title = { Text("¿Cómo funciona el medidor de luz?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Usa el sensor de luz ambiental del dispositivo para mostrar el nivel de iluminación en lux (lúmenes por metro cuadrado).")
                    Text("• 1 lux = 1 lumen/m². No mide lúmenes directamente, pero el valor de lux es el estándar en medición ambiental.")
                    Text("• Valores típicos:")
                    Text("    • 10–50 lux: luz baja, ambiente tenue.")
                    Text("    • 300–500 lux: lectura/escritorio.")
                    Text("    • 1.000+ lux: cerca de ventana o exterior.")
                    Text("    • 10.000+ lux: luz solar directa.")
                    Text("• Si el sensor marca 0 o no varía, es posible que el dispositivo no tenga sensor de luz o esté tapado.")
                    Text("• Es normal que el valor de lux cambie en saltos grandes, especialmente al pasar de poca luz a mucha luz. Esto depende de cómo está construido el sensor y no significa que la app funcione mal.")
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
