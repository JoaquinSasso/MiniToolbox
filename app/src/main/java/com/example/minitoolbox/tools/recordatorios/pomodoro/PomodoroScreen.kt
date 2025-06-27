// app/src/main/java/com/example/minitoolbox/tools/pomodoro/PomodoroScreen.kt
package com.example.minitoolbox.tools.pomodoro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope   = rememberCoroutineScope()
    var showInfo    by remember { mutableStateOf(false) }

    val settingsRepo = remember { PomodoroSettingsRepository(context) }
    val stateRepo    = remember { PomodoroStateRepository   (context) }

    // Permiso notificaciones Android13+
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // --- CONFIGURACIÓN PERSISTIDA ---
    val workMin  by settingsRepo.workMinFlow.collectAsState(initial = 25)
    val shortMin by settingsRepo.shortBreakFlow.collectAsState(initial = 5)
    val longMin  by settingsRepo.longBreakFlow.collectAsState(initial = 15)

    var workInput  by remember { mutableStateOf(workMin.toString())  }
    var shortInput by remember { mutableStateOf(shortMin.toString()) }
    var longInput  by remember { mutableStateOf(longMin.toString())  }

    // Sin cambios aquí: campos se actualizan automáticamente
    LaunchedEffect(workMin)  { workInput  = workMin.toString()  }
    LaunchedEffect(shortMin) { shortInput = shortMin.toString() }
    LaunchedEffect(longMin)  { longInput  = longMin.toString()  }

    // --- ESTADO DEL POMODORO ---
    val phaseName  by stateRepo.phaseNameFlow.collectAsState(initial = "")
    val phaseEnd   by stateRepo.phaseEndFlow .collectAsState(initial = 0L)
    val phaseTotal by stateRepo.phaseTotalFlow.collectAsState(initial = 0L)

    var isRunning by remember { mutableStateOf(phaseEnd > System.currentTimeMillis()) }
    var remaining by remember { mutableStateOf(
        if (phaseEnd > System.currentTimeMillis())
            (phaseEnd - System.currentTimeMillis()) / 1000L else 0L
    ) }

    // Reconstruir estado cuando cambie phaseEnd
    LaunchedEffect(phaseEnd) {
        val now = System.currentTimeMillis()
        if (phaseEnd > now) {
            isRunning = true
            remaining = (phaseEnd - now) / 1000L
        } else {
            isRunning = false
            remaining = 0L
        }
    }

    // Conteo regresivo en UI
    LaunchedEffect(isRunning) {
        while (isRunning && remaining > 0L) {
            delay(1000L)
            remaining--
        }
        if (isRunning && remaining == 0L) {
            isRunning = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pomodoro Timer") },
                navigationIcon = {
                    IconButton(onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showInfo = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }) {
                        Icon(Icons.Filled.Info, contentDescription = "Información")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 60.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    // Guardar configuración
                    scope.launch {
                        settingsRepo.updateWorkMin  (workInput.toIntOrNull() ?: workMin)
                        settingsRepo.updateShortBreak(shortInput.toIntOrNull() ?: shortMin)
                        settingsRepo.updateLongBreak (longInput.toIntOrNull() ?: longMin)
                    }
                    // Iniciar UI + servicio
                    remaining = (workInput.toIntOrNull() ?: workMin) * 60L
                    isRunning = true
                    Intent(context, PomodoroService::class.java).apply {
                        putExtra("WORK_MINUTES", workInput.toIntOrNull() ?: workMin)
                        putExtra("SHORT_BREAK",  shortInput.toIntOrNull() ?: shortMin)
                        putExtra("LONG_BREAK",   longInput.toIntOrNull() ?: longMin)
                    }.also {
                        ContextCompat.startForegroundService(context, it)
                    }
                }) {
                    Text("Iniciar")
                }

                Button(onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    // Detener servicio
                    Intent(context, PomodoroService::class.java).apply {
                        action = ACTION_STOP
                    }.also {
                        ContextCompat.startForegroundService(context, it)
                    }
                }) {
                    Text("Detener")
                }

                Button(onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    // Silenciar
                    Intent(context, PomodoroService::class.java).apply {
                        action = ACTION_SILENCE
                    }.also {
                        ContextCompat.startForegroundService(context, it)
                    }
                }) {
                    Text("Silenciar")
                }
            }
        }
    )
    {
        padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = workInput,
                onValueChange = { workInput = it },
                label = { Text("Trabajo (min)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = shortInput,
                onValueChange = { shortInput = it },
                label = { Text("Descanso corto (min)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = longInput,
                onValueChange = { longInput = it },
                label = { Text("Descanso largo (min)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            if (isRunning) {
                val minutes = remaining / 60
                val seconds = remaining % 60
                Text(
                    "$phaseName — %02d:%02d".format(minutes, seconds),
                    style = MaterialTheme.typography.headlineSmall
                )

                // Cálculo seguro de progreso
                val progress = if (phaseTotal > 0L) {
                    (remaining.toFloat() / phaseTotal.toFloat()).coerceIn(0f, 1f)
                } else 0f

                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .progressSemantics(progress)
                )
            }
        }
    }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Acerca de Pomodoro Timer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Para qué sirve: Temporizador que alterna ciclos de trabajo y descanso para mejorar tu productividad.")
                    Text("• Guía rápida:")
                    Text("   – Ingresa la duración (min) de trabajo, descanso corto y descanso largo.")
                    Text("   – Pulsa “Iniciar” para arrancar el ciclo.")
                    Text("   – Usa “Silenciar” o “Detener” desde la notificación cuando lo necesites.")
                    Text("   – El progreso y el tiempo restante se muestran aquí con barra y contador.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showInfo = false
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

