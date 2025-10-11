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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.PomodoroSettingsRepository
import com.joasasso.minitoolbox.data.PomodoroStateRepository
import com.joasasso.minitoolbox.ui.components.TopBarReusable
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
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var showInfo by remember { mutableStateOf(false) }


    // Asegurar canales de notificación creados
    LaunchedEffect(Unit) { ensurePomodoroChannels(context) }

    // Permiso notificaciones Android 13+
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // Repos actuales
    val settingsRepo = remember { PomodoroSettingsRepository(context) }
    val stateRepo    = remember { PomodoroStateRepository(context) }

    // Flows de configuración (usamos para botón Start)
    val workMin   by settingsRepo.workMinFlow.collectAsState(initial = 25)
    val shortMin  by settingsRepo.shortBreakFlow.collectAsState(initial = 5)
    val longMin   by settingsRepo.longBreakFlow.collectAsState(initial = 15)

    // Estado de la fase
    val phaseName by stateRepo.phaseNameFlow.collectAsState(initial = "")
    val phaseEnd  by stateRepo.phaseEndFlow.collectAsState(initial = 0L)
    val phaseTot  by stateRepo.phaseTotalFlow.collectAsState(initial = 0L)

    // Temporizador UI
    var isRunning by remember { mutableStateOf(phaseEnd > System.currentTimeMillis()) }
    var remaining by remember {
        mutableLongStateOf(
            if (phaseEnd > System.currentTimeMillis()) (phaseEnd - System.currentTimeMillis()) / 1000L else 0L
        )
    }

    // “Alarma sonando” (opcional: lo actualizamos con Broadcasts que emite el AlarmReceiver)
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

        onDispose {
            context.unregisterReceiver(rcv)
        }
    }


    // Reconstrucción al cambiar el fin de fase
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

    // Tick UI (solo visual)
    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(1000L)
            val now = System.currentTimeMillis()
            if (phaseEnd > now) {
                remaining = (phaseEnd - now) / 1000L
            } else {
                // acá NO apagamos la UI; el receiver ya puso la próxima fase
                break
            }
        }
    }

    Scaffold(
        topBar = {
            TopBarReusable(stringResource(R.string.tool_pomodoro_timer), onBack, onShowInfo = {showInfo = true})
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isRunning) {
                        isRunning = false
                        PomodoroAlarmReceiver.stopPomodoro(context)
                    } else {
                        remaining = workMin * 60L
                        isRunning = true
                        PomodoroAlarmReceiver.startPomodoro(context, workMin, shortMin, longMin)
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
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // 1) FASE ACTUAL → ARRIBA
            Text(
                text = phaseName.ifBlank { stringResource(R.string.pomodoro_work) },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 2) CÍRCULO + CONTENIDO CENTRADO ADENTRO
            val progress = rememberSmoothPomodoroProgress(
                isRunning = isRunning,
                phaseEndMs = phaseEnd,
                phaseTotalSec = phaseTot
            )
            val stroke = with(LocalDensity.current) {
                Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round) // ← bordes redondeados
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularWavyProgressIndicator(
                    progress = { progress },                                // 0f..1f
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(1f),
                    color = MaterialTheme.colorScheme.primary,       // “progreso” (ondulado)
                    trackColor = MaterialTheme.colorScheme.primary
                        .copy(alpha = 0.70f),                        // “restante” (liso)
                    stroke = stroke,
                    trackStroke = stroke,
                    gapSize = 10.dp,
                    // por defecto la onda aparece entre 10% y 95%.
                    // si la querés SIEMPRE visible:
                    amplitude = { 1f },
                    // opcional: si prefieres el comportamiento por defecto, quita la línea de arriba
                    wavelength =    80.dp,
                    waveSpeed = WavyProgressIndicatorDefaults.CircularWavelength
                )




                // TIEMPO EN GRANDE o ÍCONO DE SILENCIO
                AnimatedContent(
                    targetState = alarmRinging,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "alarmOrTime"
                ) { ringing ->
                    if (ringing) {
                        // Solo icono, tamaño similar al texto grande
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = stringResource(R.string.pomodoro_silence),
                            modifier = Modifier.size(72.dp) // ajustá según tu displayLarge
                                .clickable(onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    PomodoroAlarmReceiver.silenceAlarm(context)
                                    alarmRinging = false // feedback inmediato en UI
                                })

                        )
                    } else {
                        Text(
                            text = formatHMS(remaining),
                            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // (dejamos un Spacer pequeño si querés aire)
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.pomodoro_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Placeholder") //TODO agregar nuevo menu de ayuda al pomodoro timer
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

// helper simple
private fun formatHMS(sec: Long): String {
    val s = sec.coerceAtLeast(0)
    val mm = (s / 60)
    val ss = (s % 60)
    return "%02d:%02d".format(mm, ss)
}


@Composable
fun rememberSmoothPomodoroProgress(
    isRunning: Boolean,
    phaseEndMs: Long,
    phaseTotalSec: Long
): Float {
    val nowMs = System.currentTimeMillis()
    val totalMs = (phaseTotalSec * 1000L).coerceAtLeast(1L)
    val clampedNow = min(nowMs, phaseEndMs)
    val raw = if (phaseEndMs > 0L)
        1f - ((phaseEndMs - clampedNow).toFloat() / totalMs.toFloat())
    else 0f

    val anim = remember { Animatable(raw.coerceIn(0f, 1f)) }

    // Si cambia el tiempo total o el fin de fase, reiniciamos la animación al punto actual
    LaunchedEffect(phaseEndMs, phaseTotalSec) {
        anim.snapTo(raw.coerceIn(0f, 1f))
        if (isRunning) {
            val remainingMs = max(0L, phaseEndMs - System.currentTimeMillis())
            if (remainingMs > 0L) {
                anim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = remainingMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), easing = LinearEasing)
                )
            }
        }
    }

    // Si pausás/detenés, frenamos la animación y fijamos al valor actual
    LaunchedEffect(isRunning) {
        if (!isRunning) {
            anim.stop()
            val now = System.currentTimeMillis()
            val r = if (phaseEndMs > 0L)
                1f - ((phaseEndMs - min(now, phaseEndMs)).toFloat() / totalMs.toFloat())
            else 0f
            anim.snapTo(r.coerceIn(0f, 1f))
        }
    }

    return anim.value
}
