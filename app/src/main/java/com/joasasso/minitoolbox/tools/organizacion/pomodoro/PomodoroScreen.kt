// app/src/main/java/com/example/minitoolbox/tools/pomodoro/PomodoroScreen.kt
package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.PomodoroSettingsRepository
import com.joasasso.minitoolbox.data.PomodoroStateRepository
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var showInfo by remember { mutableStateOf(false) }

    val settingsRepo = remember { PomodoroSettingsRepository(context) }
    val stateRepo = remember { PomodoroStateRepository(context) }

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
        ensurePomodoroChannels(context)
    }

    // --- CONFIGURACIÓN PERSISTIDA ---
    val workMin by settingsRepo.workMinFlow.collectAsState(initial = 25)
    val shortMin by settingsRepo.shortBreakFlow.collectAsState(initial = 5)
    val longMin by settingsRepo.longBreakFlow.collectAsState(initial = 15)

    var workInput by remember { mutableStateOf(workMin.toString()) }
    var shortInput by remember { mutableStateOf(shortMin.toString()) }
    var longInput by remember { mutableStateOf(longMin.toString()) }

    //Se guardan los cambios de las preferencias
    LaunchedEffect(workMin) { workInput = workMin.toString() }
    LaunchedEffect(shortMin) { shortInput = shortMin.toString() }
    LaunchedEffect(longMin) { longInput = longMin.toString() }

    // --- ESTADO DEL POMODORO ---
    val phaseName by stateRepo.phaseNameFlow.collectAsState(initial = "")
    val phaseEnd by stateRepo.phaseEndFlow.collectAsState(initial = 0L)
    val phaseTotal by stateRepo.phaseTotalFlow.collectAsState(initial = 0L)

    var isRunning by remember { mutableStateOf(phaseEnd > System.currentTimeMillis()) }
    var remaining by remember {
        mutableLongStateOf(
            if (phaseEnd > System.currentTimeMillis())
                (phaseEnd - System.currentTimeMillis()) / 1000L else 0L
        )
    }

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
            TopBarReusable(
                stringResource(R.string.tool_pomodoro_timer),
                onBack,
                { showInfo = true }
            )
        },
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 60.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
// START
                Button(onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                    // 1) Persistir ajustes (suspend -> en corrutina)
                    scope.launch {
                        settingsRepo.updateWorkMin(workInput.toIntOrNull() ?: workMin)
                        settingsRepo.updateShortBreak(shortInput.toIntOrNull() ?: shortMin)
                        settingsRepo.updateLongBreak(longInput.toIntOrNull() ?: longMin)
                    }

                    // 2) Valores efectivos
                    val w = workInput.toIntOrNull() ?: workMin
                    val s = shortInput.toIntOrNull() ?: shortMin
                    val l = longInput.toIntOrNull() ?: longMin

                    // 3) Comprobar acceso a EXact Alarms (Android 12+)
                    val am = context.getSystemService(android.app.AlarmManager::class.java)
                    val canExact = am?.canScheduleExactAlarms() == true
                    if (!canExact) {
                        // Abrir pantalla de "Alarmas y recordatorios"
                        try {
                            context.startActivity(
                                Intent(
                                    android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {
                            // sin crash si no existe la activity (raro)
                        }
                        // No iniciar todavía: el usuario debe conceder y reintentar
                        return@Button
                    }

                    // 4) Iniciar ciclo (UI local + programación del alarm)
                    remaining = w * 60L
                    isRunning = true
                    PomodoroAlarmReceiver.startPomodoro(context, w, s, l)
                }) {
                    Text(stringResource(R.string.pomodoro_start))
                }


                // STOP
                Button(onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isRunning) {
                        isRunning = false
                        PomodoroAlarmReceiver.stopPomodoro(context)
                    }
                }) {
                    Text(stringResource(R.string.pomodoro_stop))
                }

                // SILENCE
                Button(onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    PomodoroAlarmReceiver.silenceAlarm(context)
                }) {
                    Text(stringResource(R.string.pomodoro_silence))
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = workInput,
                onValueChange = { new ->
                    workInput = new
                    scope.launch {
                        settingsRepo.updateWorkMin(new.toIntOrNull() ?: workMin)
                    }
                },
                label = { Text(stringResource(R.string.pomodoro_work_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = shortInput,
                onValueChange = { new ->
                    shortInput = new
                    scope.launch {
                        settingsRepo.updateShortBreak(new.toIntOrNull() ?: shortMin)
                    }
                },
                label = { Text(stringResource(R.string.pomodoro_short_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = longInput,
                onValueChange = { new ->
                    longInput = new
                    scope.launch {
                        settingsRepo.updateLongBreak(new.toIntOrNull() ?: longMin)
                    }
                },
                label = { Text(stringResource(R.string.pomodoro_long_label)) },
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

                val progress = if (phaseTotal > 0L) {
                    (remaining.toFloat() / phaseTotal.toFloat()).coerceIn(0f, 1f)
                } else 0f

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .progressSemantics(progress),
                    color = ProgressIndicatorDefaults.linearColor,
                    trackColor = ProgressIndicatorDefaults.linearTrackColor,
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )
            }
        }

        if (showInfo) {
            AlertDialog(
                onDismissRequest = { showInfo = false },
                title = { Text(stringResource(R.string.pomodoro_help_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.pomodoro_help_line1))
                        Text(stringResource(R.string.pomodoro_help_line2))
                        Text(stringResource(R.string.pomodoro_help_line3))
                        Text(stringResource(R.string.pomodoro_help_line4))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showInfo = false
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }) {
                        Text(stringResource(R.string.close))
                    }
                }
            )
        }
    }
}

