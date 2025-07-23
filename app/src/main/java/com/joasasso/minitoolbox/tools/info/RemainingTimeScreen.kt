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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemainingTimeScreen(onBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val formatter = remember { NumberFormat.getInstance(Locale("es", "AR")) }
    val scrollState = rememberScrollState()

    var rawDigits by remember { mutableStateOf("") }
    var dateError by remember { mutableStateOf<String?>(null) }
    var daysLeft by remember { mutableStateOf<Long?>(0) }
    var showInfo by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    fun updateDaysLeft(target: LocalDate) {
        val today = LocalDate.now()
        daysLeft = ChronoUnit.DAYS.between(today, target).coerceAtLeast(0)
    }

    Scaffold(
        topBar = {TopBarReusable(stringResource(R.string.tool_day_countdown), onBack, {showInfo = true})}
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
                    daysLeft = null

                    if (rawDigits.length == 8) {
                        val d = rawDigits.substring(0, 2).toInt()
                        val m = rawDigits.substring(2, 4).toInt()
                        val y = rawDigits.substring(4, 8).toInt()

                        if (m in 1..12) {
                            val maxDay = LocalDate.of(y, m, 1).lengthOfMonth()
                            if (d in 1..maxDay) {
                                val target = LocalDate.of(y, m, d)
                                val today = LocalDate.now()
                                if (target.isBefore(today)) {
                                    dateError = "La fecha debe ser futura"
                                } else {
                                    updateDaysLeft(target)
                                }
                            } else {
                                dateError = "Día fuera de rango (1-$maxDay)"
                            }
                        } else {
                            dateError = "Mes fuera de rango (1–12)"
                        }
                    }
                },
                label = { Text("DD/MM/YYYY") },
                singleLine = true,
                isError = dateError != null,
                supportingText = {
                    dateError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = DateVisualTransformation()
            )

            if (daysLeft != null) {
                Text("⏳ Faltan ${formatter.format(daysLeft)} días", style = MaterialTheme.typography.headlineSmall)
            }

            Spacer(Modifier.height(8.dp))
            Text("Fechas típicas:")
            listOf(
                "🎄 Navidad" to nextDate(today, 12, 25),
                "🎆 Año Nuevo" to nextDate(today, 1, 1),
                "💘 San Valentín" to nextDate(today, 2, 14),
                "🐣 Pascuas" to estimateEaster(today.year).let {
                    if (it.isBefore(LocalDate.now())) estimateEaster(today.year + 1) else it
                },
                "🎃 Halloween" to nextDate(today, 10, 31),
                "👻 Día de los Muertos" to nextDate(today, 11, 2),
                "👑 Reyes" to nextDate(today, 1, 6),
                "🛠️ Día del Trabajador" to nextDate(today, 5, 1),
                "🌎 Día de la Tierra" to nextDate(today, 4, 22),
                "❄️ Solsticio de invierno" to nextDate(today, 6, 21),
                "🌞 Solsticio de verano" to nextDate(today, 12, 21),
                "🌱 Equinoccio de primavera" to nextDate(today, 9, 21),
                "🍂 Equinoccio de otoño" to nextDate(today, 3, 21)


            ).forEach { (label, date) ->
                Button(onClick = {
                    updateDaysLeft(date)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                    modifier = Modifier.fillMaxWidth())
                {
                    Text("$label (${date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))})")
                }
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Acerca de ¿Cuánto falta para...?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Para qué sirve: Calcula cuántos días faltan hasta la fecha que elijas.")
                    Text("• Guía rápida:")
                    Text("   – Ingresa la fecha en formato DD/MM/YYYY.")
                    Text("   – O selecciona una de las fechas típicas.")
                    Text("   – La fecha debe ser futura.")
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
