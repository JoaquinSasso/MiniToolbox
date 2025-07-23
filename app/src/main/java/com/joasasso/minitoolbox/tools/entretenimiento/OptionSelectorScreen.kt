package com.joasasso.minitoolbox.tools.entretenimiento

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun OptionSelectorScreen(onBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var showInfo by remember { mutableStateOf(false) }

    var options by remember { mutableStateOf(mutableStateListOf("Opción 1", "Opción 2", "Opción 3")) }
    var currentStep by remember { mutableStateOf(0) }
    var currentText by remember { mutableStateOf("") }
    var isSpinning by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val itemHeight = 80.dp

    Scaffold(
        topBar = { TopBarReusable(stringResource(R.string.tool_option_selector), onBack) { showInfo = true } }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .height(itemHeight)
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = Pair(currentText, currentStep),
                    transitionSpec = {
                        (slideInVertically(
                            animationSpec = tween(200),
                            initialOffsetY = { -it }
                        )
                            .togetherWith(
                                slideOutVertically(
                                    animationSpec = tween(200),
                                    targetOffsetY = { it }
                                )
                            ))
                    }
                ) { (text, _) ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }

            Button(
                onClick = {
                    if (!isSpinning && options.isNotEmpty()) {
                        isSpinning = true
                        scope.launch {
                            val steps = 15
                            val totalDuration = 300L * steps

                            for (i in 1..steps) {
                                val next = options.random()
                                currentText = next
                                currentStep++ // Incrementa para que AnimatedContent detecte cambio
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                                val stepDelay = (totalDuration / steps) * i / steps
                                delay(stepDelay)
                            }

                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isSpinning = false
                        }
                    }
                },
                enabled = !isSpinning
            ) {
                Text("Girar")
            }

            Column(
                Modifier
                    .verticalScroll(scrollState)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEachIndexed { index, text ->
                    OutlinedTextField(
                        value = text,
                        onValueChange = { new -> options[index] = new },
                        label = { Text("Opción ${index + 1}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { options.add("Opción ${options.size + 1}") }) {
                        Text("Agregar opción")
                    }
                    if (options.size > 1) {
                        Button(onClick = { options.removeAt(options.lastIndex) }) {
                            Text("Eliminar última")
                        }
                    }
                }
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Acerca del Selector de Opciones") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Para qué sirve: Selector estilo tragamonedas que pasa por varias opciones antes de detenerse.")
                    Text("• Cómo usar: Editá o agregá opciones, luego presioná 'Girar'. El selector pasará por varias y se detendrá en una al azar.")
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
