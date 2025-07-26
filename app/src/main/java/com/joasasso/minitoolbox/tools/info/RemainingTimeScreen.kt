package com.joasasso.minitoolbox.tools.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.NumberFormat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemainingTimeScreen(onBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val formatter = remember { NumberFormat.getInstance(Locale.getDefault()) }
    val scrollState = rememberScrollState()

    var rawDigits by remember { mutableStateOf("") }
    var dateError by remember { mutableStateOf<String?>(null) }
    var targetDateTime by remember { mutableStateOf<LocalDateTime?>(null) }
    var remainingTime by remember { mutableStateOf(Duration.ZERO) }
    var showInfo by remember { mutableStateOf(false) }

    val today = LocalDate.now()

    fun updateRemainingTime(target: LocalDate) {
        targetDateTime = target.atStartOfDay()
    }

    LaunchedEffect(targetDateTime) {
        while (isActive && targetDateTime != null) {
            val now = LocalDateTime.now()
            remainingTime = Duration.between(now, targetDateTime).coerceAtLeast(Duration.ZERO)
            delay(1000)
        }
    }

    val context = LocalContext.current

    Scaffold(
        topBar = { TopBarReusable(stringResource(R.string.tool_day_countdown), onBack, { showInfo = true }) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = rawDigits,
                onValueChange = { new ->
                    rawDigits = new.filter { it.isDigit() }.take(8)
                    dateError = null
                    targetDateTime = null

                    if (rawDigits.length == 8) {
                        val d = rawDigits.substring(0, 2).toInt()
                        val m = rawDigits.substring(2, 4).toInt()
                        val y = rawDigits.substring(4, 8).toInt()

                        if (m in 1..12) {
                            val maxDay = LocalDate.of(y, m, 1).lengthOfMonth()
                            if (d in 1..maxDay) {
                                val target = LocalDate.of(y, m, d)
                                val now = LocalDate.now()
                                if (target.isBefore(now)) {
                                    dateError = context.getString(R.string.countdown_error_future)
                                } else {
                                    updateRemainingTime(target)
                                }
                            } else {
                                dateError = context.getString(R.string.countdown_error_day, maxDay)
                            }
                        } else {
                            dateError = context.getString(R.string.countdown_error_month)
                        }
                    }
                },
                label = { Text(stringResource(R.string.countdown_hint)) },
                singleLine = true,
                isError = dateError != null,
                supportingText = {
                    dateError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = DateVisualTransformation()
            )

            val days = remainingTime.toDays()
            val hours = (remainingTime.toHours() % 24)
            val minutes = (remainingTime.toMinutes() % 60)
            val seconds = (remainingTime.seconds % 60)

            Text(
                stringResource(
                    R.string.countdown_result,
                    formatter.format(days),
                    formatter.format(hours),
                    formatter.format(minutes),
                    formatter.format(seconds)
                ),
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.countdown_title_dates))
            listOf(
                "🎄 ${stringResource(R.string.countdown_xmas)}" to nextDate(today, 12, 25),
                "🎆 ${stringResource(R.string.countdown_new_year)}" to nextDate(today, 1, 1),
                "💘 ${stringResource(R.string.countdown_valentines)}" to nextDate(today, 2, 14),
                "🐣 ${stringResource(R.string.countdown_easter)}" to estimateEaster(today.year).let {
                    if (it.isBefore(LocalDate.now())) estimateEaster(today.year + 1) else it
                },
                "🎃 ${stringResource(R.string.countdown_halloween)}" to nextDate(today, 10, 31),
                "👻 ${stringResource(R.string.countdown_dead)}" to nextDate(today, 11, 2),
                "👑 ${stringResource(R.string.countdown_kings)}" to nextDate(today, 1, 6),
                "🛠️ ${stringResource(R.string.countdown_labor)}" to nextDate(today, 5, 1),
                "🌎 ${stringResource(R.string.countdown_earth)}" to nextDate(today, 4, 22),
                "❄️ ${stringResource(R.string.countdown_winter)}" to nextDate(today, 6, 21),
                "🌞 ${stringResource(R.string.countdown_summer)}" to nextDate(today, 12, 21),
                "🌱 ${stringResource(R.string.countdown_spring)}" to nextDate(today, 9, 21),
                "🍂 ${stringResource(R.string.countdown_autumn)}" to nextDate(today, 3, 21)
            ).forEach { (label, date) ->
                Button(
                    onClick = {
                        updateRemainingTime(date)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("$label (${date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))})")
                }
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.countdown_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.countdown_help_line1))
                    Text(stringResource(R.string.countdown_help_line2))
                    Text(stringResource(R.string.countdown_help_line3))
                    Text(stringResource(R.string.countdown_help_line4))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showInfo = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}



fun nextDate(today: LocalDate, month: Int, day: Int): LocalDate {
    var candidate = LocalDate.of(today.year, month, day)
    if (!candidate.isAfter(today)) {
        candidate = candidate.plusYears(1)
    }
    return candidate
}

fun estimateEaster(year: Int): LocalDate {
    val a = year % 19
    val b = year / 100
    val c = year % 100
    val d = b / 4
    val e = b % 4
    val f = (b + 8) / 25
    val g = (b - f + 1) / 3
    val h = (19 * a + b - d - g + 15) % 30
    val i = c / 4
    val k = c % 4
    val l = (32 + 2 * e + 2 * i - h - k) % 7
    val m = (a + 11 * h + 22 * l) / 451
    val month = (h + l - 7 * m + 114) / 31
    val day = ((h + l - 7 * m + 114) % 31) + 1
    return LocalDate.of(year, month, day)
}
