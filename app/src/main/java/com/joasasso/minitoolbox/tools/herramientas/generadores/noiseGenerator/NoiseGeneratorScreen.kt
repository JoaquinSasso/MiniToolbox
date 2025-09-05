package com.joasasso.minitoolbox.tools.herramientas.generadores.noiseGenerator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhiteNoiseScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var type by remember { mutableStateOf(NoiseType.White) }
    var volume by remember { mutableFloatStateOf(0.5f) }
    var minutes by remember { mutableFloatStateOf(30f) } // temporizador en minutos
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isPlaying = NoiseEngine.isRunning()
    }

    Scaffold(
        topBar = {
            TopBarReusable(stringResource(R.string.tool_noise_generator), onBack)
        }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Selector de tipo
            SegmentedButtons(
                selected = type,
                onSelected = { type = it },
            )

            // Volumen
            Text("Volumen: ${(volume * 100).toInt()}%")
            Slider(value = volume, onValueChange = {
                volume = it
                NoiseEngine.setVolume(volume)
            })

            // Temporizador
            Text("Temporizador: ${minutes.toInt()} min")
            Slider(value = minutes, onValueChange = { minutes = it.coerceIn(0f, 180f) }, valueRange = 0f..180f)

            // Botones
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        if (!isPlaying) {
                            NoiseService.Companion.startForeground(
                                ctx,
                                type = type,
                                volume = volume,
                                timerMs = (if (minutes <= 0f) null else (minutes * 60_000).toLong()),
                                fadeMs = 3000
                            )
                            isPlaying = true
                        } else {
                            // Cambiar tipo en vivo
                            NoiseEngine.changeType(type)
                        }
                    }
                ) { Text(if (isPlaying) "Cambiar tipo" else "Reproducir") }

                OutlinedButton(
                    onClick = {
                        NoiseService.Companion.stop(ctx)
                        isPlaying = false
                    },
                    enabled = isPlaying
                ) { Text("Detener") }
            }

            Text(
                "Consejo: usa el temporizador + fade para dormir. La app seguirá sonando con la pantalla apagada."
            )
        }
    }
}

@Composable
private fun SegmentedButtons(
    selected: NoiseType,
    onSelected: (NoiseType) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(NoiseType.White, NoiseType.Pink, NoiseType.Brown).forEach { t ->
            FilterChip(
                selected = selected == t,
                onClick = { onSelected(t) },
                label = { Text(t.name) }
            )
        }
    }
}
