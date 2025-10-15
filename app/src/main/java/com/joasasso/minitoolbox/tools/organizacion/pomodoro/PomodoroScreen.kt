package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.PomodoroStateRepository
import com.joasasso.minitoolbox.data.PomodoroTimersPrefs
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

// Broadcasts opcionales para reflejar en UI estado de alarma
const val ACTION_POMODORO_ALARM_START = "POMODORO_ALARM_START"
const val ACTION_POMODORO_ALARM_STOP  = "POMODORO_ALARM_STOP"

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PomodoroScreen(
    timerId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showInfo by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current

    val timerConfig by remember(timerId) {
        mutableStateOf(
            PomodoroTimersPrefs.loadAll(context).firstOrNull { it.id == timerId }
                ?: PomodoroTimerConfig(
                    name = "Default",
                    colorInt = Color(0xFF4DBC52).toArgbInt(),
                    workMin = 25, shortBreakMin = 5, longBreakMin = 15, cyclesBeforeLong = 4
                )
        )
    }
    // Flag para evitar dobles disparos si hay recomposición
    var advanceInFlight by remember { mutableStateOf(false) }

    // Permiso notificaciones Android 13+
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        // Asegurar canales de notificación creados
        ensurePomodoroChannels(context)
    }

    // Repos actuales
    val stateRepo    = remember { PomodoroStateRepository(context) }
    // Estado de la fase
    val phaseName by stateRepo.phaseNameFlow.collectAsState(initial = "")
    val phaseEnd  by stateRepo.phaseEndFlow.collectAsState(initial = 0L)
    val phaseTot  by stateRepo.phaseTotalFlow.collectAsState(initial = 0L)

    // Temporizador UI
    var isRunning by remember { mutableStateOf(phaseEnd > System.currentTimeMillis()) }

    // “Alarma sonando” (persistente + broadcasts)
    var alarmRinging by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val filter = IntentFilter().apply {
            addAction(ACTION_POMODORO_ALARM_START)
            addAction(ACTION_POMODORO_ALARM_STOP)
        }
        val rcv = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                alarmRinging = i.action == ACTION_POMODORO_ALARM_START
            }
        }
        context.registerReceiver(rcv, filter, Context.RECEIVER_NOT_EXPORTED)

        // lectura inicial al entrar
        alarmRinging = AlarmState.isActive(context)

        onDispose {
            context.unregisterReceiver(rcv)
        }
    }

    // Reconstrucción al cambiar el fin de fase
    LaunchedEffect(phaseEnd) {
        val now = System.currentTimeMillis()
        isRunning = phaseEnd > now
    }

    // Avanzar localmente cuando cruzamos el 0, sin esperar al Receiver
    LaunchedEffect(isRunning, phaseEnd, phaseName) {
        if (!isRunning || phaseEnd <= 0L) return@LaunchedEffect
        while (isRunning && System.currentTimeMillis() < phaseEnd) {
            delay(250)
        }
        if (isRunning && !advanceInFlight) {
            advanceInFlight = true
            try {
                PomodoroAlarmReceiver.forceAdvanceFromUi(
                    context = context,
                    currentPhaseName = phaseName.ifBlank { context.getString(R.string.pomodoro_work) },
                    config = timerConfig
                )
            } finally {
                advanceInFlight = false
            }
        }
    }

    // Refresca el estado al volver al foreground (si la alarma sonaba en segundo plano)
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                alarmRinging = AlarmState.isActive(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Scaffold(
        topBar = {
            TopBarReusable(timerConfig.name, onBack, onShowInfo = {showInfo = true})
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isRunning) {
                        isRunning = false
                        PomodoroAlarmReceiver.stopPomodoro(context)
                    } else {
                        isRunning = true
                        PomodoroAlarmReceiver.startPomodoro(
                            context = context,
                            config = timerConfig
                        )
                    }
                },
                modifier = Modifier.size(96.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1) FASE ACTUAL → ARRIBA
            Text(
                text = phaseName.ifBlank { stringResource(R.string.pomodoro_work) },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // 2) CÍRCULO + CONTENIDO
            val progress = rememberClockSyncedProgress(
                isRunning = isRunning,
                phaseEndMs = phaseEnd,
                phaseTotalSec = phaseTot
            )
            val stroke = with(LocalDensity.current) {
                Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularWavyProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(1f),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.70f),
                    stroke = stroke,
                    trackStroke = stroke,
                    gapSize = 10.dp,
                    amplitude = { 1f },
                    wavelength = 80.dp,
                    waveSpeed = WavyProgressIndicatorDefaults.CircularWavelength
                )

                AnimatedContent(
                    targetState = if (alarmRinging) "ring" else if (advanceInFlight) "finishing" else "time",
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "alarmOrTime"
                ) { state ->
                    when (state) {
                        "ring" -> {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = stringResource(R.string.pomodoro_silence),
                                modifier = Modifier
                                    .size(72.dp)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        PomodoroAlarmReceiver.silenceAlarm(context)
                                        alarmRinging = false
                                    }
                            )
                        }
                        "finishing" -> {
                            Text(
                                text = stringResource(R.string.pomodoro_finishing),
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                        else -> {
                            val nowForText = System.currentTimeMillis()
                            val remainingSec = if (phaseEnd > nowForText) (phaseEnd - nowForText) / 1000L else 0L
                            Text(
                                text = formatHMS(remainingSec),
                                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.pomodetail_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.pomodetail_help_p1))
                    Text(stringResource(R.string.pomodetail_help_p2))
                    Text(stringResource(R.string.pomodetail_help_p3))
                }
            },
            confirmButton = {
                TextButton(onClick =  {
                    showInfo = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) { Text(stringResource(R.string.close)) }
            }
        )
    }
}

// helper simple
private fun formatHMS(sec: Long): String {
    val s = sec.coerceAtLeast(0)
    val mm = (s / 60)
    val ss = (s % 60)
    return "%02d:%02d".format(mm, ss)
}

// --- Helpers: reloj y progreso continuo en base a phaseEnd/phaseTotal ---
@Composable
private fun rememberFrameNow(isRunning: Boolean, phaseEndMs: Long): Long {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isRunning, phaseEndMs) {
        now = System.currentTimeMillis()
        while (isRunning && now < phaseEndMs) {
            awaitFrame()
            now = System.currentTimeMillis()
        }
        now = System.currentTimeMillis()
    }
    return now
}

@Composable
private fun rememberClockSyncedProgress(
    isRunning: Boolean,
    phaseEndMs: Long,
    phaseTotalSec: Long
): Float {
    val now = rememberFrameNow(isRunning, phaseEndMs)
    val totalMs = max(1L, phaseTotalSec * 1000L)
    val clampedNow = min(now, phaseEndMs)
    val raw = if (phaseEndMs > 0L) 1f - ((phaseEndMs - clampedNow).toFloat() / totalMs) else 0f
    return raw.coerceIn(0f, 1f)
}
