package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.PomodoroTimersPrefs
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import com.joasasso.minitoolbox.ui.utils.getContrastingTextColor
import com.joasasso.minitoolbox.utils.vibrate
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroTimersListScreen(
    onBack: () -> Unit,
    onOpenTimer: (PomodoroTimerConfig) -> Unit = {} // opcional: abrir detalle
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var showInfo by remember { mutableStateOf(false) }

    // Animación de sacudida para el timer activo
    val shakeOffset = remember { Animatable(0f) }
    val busyMsg = stringResource(R.string.pomodoro_timer_busy)

    // Paleta de colores (puedes reutilizar la del marcador)
    val colorOptions = listOf(
        Color(0xFFFFF9C4), Color(0xFFFFCCBC), Color(0xFF76D7C4),
        Color(0xFFB2EBF2), Color(0xFFC8E6C9), Color(0xFFD1C4E9),
        Color(0xFFFFECB3), Color(0xFFC71FE8), Color(0xFFDCEDC8),
        Color(0xFF723855), Color(0xFF5E08C2), Color(0xFF4DBC52)
    )

    // Estado: lista de timers
    var timers by remember { mutableStateOf(emptyList<PomodoroTimerConfig>()) }
    var activeTimerId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        timers = PomodoroTimersPrefs.loadAll(context)
        activeTimerId = PomodoroSchedulePrefs.load(context)?.config?.id
    }
    LaunchedEffect(timers) { PomodoroTimersPrefs.saveAll(context, timers) }

    // Diálogos
    var showEditFor by remember { mutableStateOf<PomodoroTimerConfig?>(null) }
    var showDeleteConfirmFor by remember { mutableStateOf<PomodoroTimerConfig?>(null) }

    Scaffold(
        topBar = {
            TopBarReusable(
                title = stringResource(R.string.tool_pomodoro_timer),
                onBack = onBack
            ) { showInfo = true }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    timers = timers + defaultNewTimer(colorOptions)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.pomodoro_add_timer)) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (timers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.pomodoro_no_timers),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }
            }
            items(timers, key = { it.id }) { timer ->
                val cardBg = timer.color()
                val textColor = getContrastingTextColor(cardBg)
                val isActive = timer.id == activeTimerId
                val activeIndicatorColor = Color(0xFFFF8A80) // Pastel Red

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(if (isActive) shakeOffset.value.roundToInt() else 0, 0) }
                        .clickable {
                            if (activeTimerId != null && activeTimerId != timer.id) {
                                // Feedback: vibración fuerte, sacudida y Toast
                                vibrate(context, 400, 255)
                                Toast.makeText(context, busyMsg, Toast.LENGTH_SHORT).show()
                                scope.launch {
                                    repeat(4) {
                                        shakeOffset.animateTo(15f, tween(50))
                                        shakeOffset.animateTo(-15f, tween(50))
                                    }
                                    shakeOffset.animateTo(0f, tween(50))
                                }
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOpenTimer(timer)
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = cardBg
                    ),
                    border = if (isActive) BorderStroke(4.dp, activeIndicatorColor) else null
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        if (isActive) {
                            Surface(
                                color = activeIndicatorColor,
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.pomodoro_active).uppercase(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = timer.workMin.toString(),
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Black,
                                        color = textColor
                                    )
                                    Text(
                                        text = " min",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                Text(
                                    stringResource(R.string.pomodoro_work),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = textColor.copy(alpha = 0.7f)
                                )
                            }
                            Text("/", style = MaterialTheme.typography.headlineMedium, color = textColor.copy(alpha = 0.5f))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = timer.shortBreakMin.toString(),
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Black,
                                        color = textColor
                                    )
                                    Text(
                                        text = " min",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                Text(
                                    stringResource(R.string.pomodoro_short_break),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = textColor.copy(alpha = 0.7f)
                                )
                            }
                            Text("/", style = MaterialTheme.typography.headlineMedium, color = textColor.copy(alpha = 0.5f))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = timer.longBreakMin.toString(),
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Black,
                                        color = textColor
                                    )
                                    Text(
                                        text = " min",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                                Text(
                                    stringResource(R.string.pomodoro_long_break),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = textColor.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.pomodoro_cycles_before_long, timer.cyclesBeforeLong),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = {
                                if (isActive) {
                                    // Bloqueo solo para el timer que está corriendo
                                    vibrate(context, 400, 255)
                                    Toast.makeText(context, busyMsg, Toast.LENGTH_SHORT).show()
                                    scope.launch {
                                        repeat(4) {
                                            shakeOffset.animateTo(15f, tween(50))
                                            shakeOffset.animateTo(-15f, tween(50))
                                        }
                                        shakeOffset.animateTo(0f, tween(50))
                                    }
                                } else {
                                    showEditFor = timer
                                }
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = null,
                                    tint = textColor)
                            }
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showDeleteConfirmFor = timer
                            }) { Icon(Icons.Default.Delete, contentDescription = null,
                                tint = textColor) }
                        }
                    }
                }
            }
        }
    }

    // EDIT DIALOG
    showEditFor?.let { current ->
        var work by remember { mutableStateOf(current.workMin.toString()) }
        var shortB by remember { mutableStateOf(current.shortBreakMin.toString()) }
        var longB by remember { mutableStateOf(current.longBreakMin.toString()) }
        var cycles by remember { mutableStateOf(current.cyclesBeforeLong.toString()) }
        var color by remember { mutableStateOf(current.color()) }

        AlertDialog(
            onDismissRequest = { showEditFor = null },
            title = { Text(stringResource(R.string.pomodoro_edit_timer)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(
                            value = work,
                            onValueChange = { work = it },
                            label = stringResource(R.string.pomodoro_work),
                            suffix = "min",
                            modifier = Modifier.weight(1f)
                        )
                        NumberField(
                            value = shortB,
                            onValueChange = { shortB = it },
                            label = stringResource(R.string.pomodoro_short_break),
                            suffix = "min",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(
                            value = longB,
                            onValueChange = { longB = it },
                            label = stringResource(R.string.pomodoro_long_break),
                            suffix = "min",
                            modifier = Modifier.weight(1f)
                        )
                        NumberField(
                            value = cycles,
                            onValueChange = { cycles = it },
                            label = stringResource(R.string.pomodoro_cycles),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(stringResource(R.string.color))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        items(colorOptions) { c ->
                            Box(
                                modifier = Modifier
                                    .size(if (c == color) 36.dp else 30.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .clickable { color = c }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val w = work.toIntOrNull()?.coerceAtLeast(1) ?: current.workMin
                    val s = shortB.toIntOrNull()?.coerceAtLeast(1) ?: current.shortBreakMin
                    val l = longB.toIntOrNull()?.coerceAtLeast(1) ?: current.longBreakMin
                    val cy = cycles.toIntOrNull()?.coerceAtLeast(1) ?: current.cyclesBeforeLong

                    timers = timers.map {
                        if (it.id == current.id) it.copy(
                            colorInt = color.toArgbInt(),                    // ← guardar Int
                            workMin = w, shortBreakMin = s, longBreakMin = l, cyclesBeforeLong = cy
                        ) else it
                    }
                    showEditFor = null
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showEditFor = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // DELETE CONFIRM DIALOG
    showDeleteConfirmFor?.let { timerToDelete ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmFor = null },
            title = { Text(stringResource(R.string.pomodoro_delete_title)) },
            text = { Text(stringResource(R.string.pomodoro_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    if (timerToDelete.id == activeTimerId) {
                        PomodoroAlarmReceiver.stopPomodoro(context)
                        activeTimerId = null
                    }
                    timers = timers.filter { it.id != timerToDelete.id }
                    showDeleteConfirmFor = null
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmFor = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest =  { showInfo = false },
            title = { Text(stringResource(R.string.pomolist_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.pomolist_help_p1))
                    Text(stringResource(R.string.pomolist_help_p2))
                    Text(stringResource(R.string.pomolist_help_p3))
                    Text(stringResource(R.string.pomolist_help_p4))
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

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all { ch -> ch.isDigit() } && it.length <= 3) onValueChange(it) },
        label = { Text(label) },
        suffix = suffix?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

private fun defaultNewTimer(colors: List<Color>): PomodoroTimerConfig {
    val c = colors.random()
    return PomodoroTimerConfig(
        colorInt = c.toArgbInt(),                         // ← Int
        workMin = 25,
        shortBreakMin = 5,
        longBreakMin = 15,
        cyclesBeforeLong = 4
    )
}