package com.joasasso.minitoolbox.tools.organizacion.pomodoro

import android.content.Context
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.PomodoroTimersPrefs
import com.joasasso.minitoolbox.ui.components.TopBarReusable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroTimersListScreen(
    onBack: () -> Unit,
    onOpenTimer: (PomodoroTimerConfig) -> Unit = {} // opcional: abrir detalle
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showInfo by remember { mutableStateOf(false) }

    // Paleta de colores (puedes reutilizar la del marcador)
    val colorOptions = listOf(
        Color(0xFFFFF9C4), Color(0xFFFFCCBC), Color(0xFF76D7C4),
        Color(0xFFB2EBF2), Color(0xFFC8E6C9), Color(0xFFD1C4E9),
        Color(0xFFFFECB3), Color(0xFFC71FE8), Color(0xFFDCEDC8),
        Color(0xFF723855), Color(0xFF5E08C2), Color(0xFF4DBC52),
        Color(0xFFFF746C)
    )

    // Estado: lista de timers
    var timers by remember { mutableStateOf(emptyList<PomodoroTimerConfig>()) }

    LaunchedEffect(Unit) { timers = PomodoroTimersPrefs.loadAll(context) }
    LaunchedEffect(timers) { PomodoroTimersPrefs.saveAll(context, timers) }

    // Diálogos
    var showEditFor by remember { mutableStateOf<PomodoroTimerConfig?>(null) }

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
                    timers = timers + defaultNewTimer(context, colorOptions)
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
            itemsIndexed(timers, key = { _, t -> t.id }) { index, timer ->
                val bg = timer.color()
                val textColor = getContrastingTextColor(bg)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = bg)
                ) {
                    CompositionLocalProvider(LocalContentColor provides textColor) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    timer.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onOpenTimer(timer)
                                }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.pomodoro_start))
                                }
                                IconButton(onClick = {
                                    showEditFor = timer
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                                }
                                IconButton(onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    timers = timers.toMutableList().also { it.removeAt(index) }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "${timer.workMin} ${stringResource(R.string.pomodoro_min_work)} • " +
                                        "${timer.shortBreakMin}/${timer.longBreakMin} ${stringResource(R.string.pomodoro_min_breaks)} • " +
                                        stringResource(R.string.pomodoro_cycles_before_long, timer.cyclesBeforeLong),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    // EDIT DIALOG
    showEditFor?.let { current ->
        var name by remember { mutableStateOf(current.name) }
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
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.name)) }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(
                            value = work,
                            onValueChange = { work = it },
                            label = stringResource(R.string.pomodoro_work),
                            modifier = Modifier.weight(1f)
                        )
                        NumberField(
                            value = shortB,
                            onValueChange = { shortB = it },
                            label = stringResource(R.string.pomodoro_short_break),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(
                            value = longB,
                            onValueChange = { longB = it },
                            label = stringResource(R.string.pomodoro_long_break),
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
                            name = name.ifBlank { current.name },
                            colorArgb = color.toArgbLong(),
                            workMin = w,
                            shortBreakMin = s,
                            longBreakMin = l,
                            cyclesBeforeLong = cy
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

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.pomodoro_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.pomodoro_help_line1))
                    Text(stringResource(R.string.pomodoro_help_line2))
                }
            },
            confirmButton = {
                TextButton(onClick = {
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
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all { ch -> ch.isDigit() } && it.length <= 3) onValueChange(it) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

fun getContrastingTextColor(bg: Color): Color {
    return if (bg.luminance() > 0.5f) Color.Black else Color.White
}

private fun defaultNewTimer(context: Context, colors: List<Color>): PomodoroTimerConfig {
    val idx = (0..9999).random()
    return PomodoroTimerConfig(
        name = "${context.getString(R.string.pomodoro_timer)} ${idx}",
        colorArgb = colors.random().toArgbLong(),
        workMin = 25,
        shortBreakMin = 5,
        longBreakMin = 15,
        cyclesBeforeLong = 4
    )
}