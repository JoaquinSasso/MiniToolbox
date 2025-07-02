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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgeCalculatorScreen(onBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current

    val formatter   = remember { NumberFormat.getInstance(Locale("es", "AR")) }
    val scrollState = rememberScrollState()
    var showInfo    by remember { mutableStateOf(false) }

    var rawDigits      by remember { mutableStateOf("") }
    var dateError      by remember { mutableStateOf<String?>(null) }
    var result         by remember { mutableStateOf("") }

    var daysUntilBirthday by remember { mutableStateOf(0L) }
    var daysLived         by remember { mutableStateOf(0L) }
    var hoursLived        by remember { mutableStateOf(0L) }
    var minutesLived      by remember { mutableStateOf(0L) }
    var secondsLived      by remember { mutableStateOf(0L) }
    var heartbeats        by remember { mutableStateOf(0L) }
    var bloodLiters       by remember { mutableStateOf(0L) }
    var blinks            by remember { mutableStateOf(0L) }
    var breaths           by remember { mutableStateOf(0L) }
    var sleepDays         by remember { mutableStateOf(0L) }
    var weeksLived        by remember { mutableStateOf(0L) }
    var fullMoonsSeen     by remember { mutableStateOf(0) }
    var percentSinceBday  by remember { mutableStateOf(0) }
    var dayOfWeek         by remember { mutableStateOf("") }

    fun calculateStats(birth: Calendar) {
        val today = Calendar.getInstance()

        result = run {
            var years  = today.get(Calendar.YEAR)  - birth.get(Calendar.YEAR)
            var months = today.get(Calendar.MONTH) - birth.get(Calendar.MONTH)
            var days   = today.get(Calendar.DAY_OF_MONTH) - birth.get(Calendar.DAY_OF_MONTH)
            if (days < 0) {
                months -= 1
                val prev = (today.get(Calendar.MONTH) + 11) % 12
                val tmp  = today.clone() as Calendar
                tmp.set(Calendar.MONTH, prev)
                days += tmp.getActualMaximum(Calendar.DAY_OF_MONTH)
            }
            if (months < 0) {
                years  -= 1
                months += 12
            }
            "Tienes $years años, $months meses y $days días"
        }

        val diff = today.timeInMillis - birth.timeInMillis
        daysLived    = diff / (1000L * 60 * 60 * 24)
        hoursLived   = diff / (1000L * 60 * 60)
        minutesLived = diff / (1000L * 60)
        secondsLived = diff / 1000L
        heartbeats   = minutesLived * 100
        bloodLiters  = heartbeats * 7 / 100
        blinks       = minutesLived * 15
        breaths      = minutesLived * 16
        sleepDays    = daysLived / 3
        weeksLived   = daysLived / 7
        fullMoonsSeen= (daysLived / 29.5).toInt()

        val next = Calendar.getInstance().apply {
            set(Calendar.MONTH, birth.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, birth.get(Calendar.DAY_OF_MONTH))
            set(Calendar.YEAR, today.get(Calendar.YEAR))
            if (!after(today)) add(Calendar.YEAR, 1)
        }
        daysUntilBirthday = (next.timeInMillis - today.timeInMillis) /
                (1000L * 60 * 60 * 24)

        val last = (next.clone() as Calendar).apply { add(Calendar.YEAR, -1) }
        val sinceLast = (today.timeInMillis - last.timeInMillis) /
                (1000L * 60 * 60 * 24)
        percentSinceBday = (sinceLast * 100 / 365).toInt()

        dayOfWeek = birth.getDisplayName(
            Calendar.DAY_OF_WEEK,
            Calendar.LONG,
            Locale("es")
        ) ?: ""
    }

    Scaffold(
        topBar = {TopBarReusable("Calculadora de Edad", onBack, {showInfo = true})},
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
                    result = ""
                    if (rawDigits.length == 8) {
                        val d = rawDigits.substring(0,2).toInt()
                        val m = rawDigits.substring(2,4).toInt()
                        val y = rawDigits.substring(4,8).toInt()

                        if (m in 1..12) {
                            val tmp = Calendar.getInstance().apply {
                                set(Calendar.YEAR, y)
                                set(Calendar.MONTH, m-1)
                            }
                            val maxDay = tmp.getActualMaximum(Calendar.DAY_OF_MONTH)
                            if (d in 1..maxDay) {
                                val birth = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, y)
                                    set(Calendar.MONTH, m-1)
                                    set(Calendar.DAY_OF_MONTH, d)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                val today = Calendar.getInstance()
                                if (birth.after(today)) {
                                    dateError = "La fecha no puede ser futura"
                                } else {
                                    calculateStats(birth)
                                }
                            } else {
                                dateError = "Día fuera de rango (1–$maxDay)"
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
                visualTransformation = DateVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            if (result.isNotEmpty()) {
                Text(result, style = MaterialTheme.typography.headlineSmall)
            }

            // Datos curiosos siempre visibles, empiezan en 0
            Spacer(Modifier.height(8.dp))
            Text("🎂 Faltan ${formatter.format(daysUntilBirthday)} días para tu próximo cumpleaños")
            Text("📅 Has vivido ${formatter.format(daysLived)} días")
            Text("😴 Has dormido unos ${formatter.format(sleepDays)} días")
            Text("🗓️ Eso equivale a ${formatter.format(weeksLived)} semanas")
            Text("🕐 Has vivido aproximadamente ${formatter.format(hoursLived)} horas")
            Text("⏰ Has vivido ${formatter.format(minutesLived)} minutos")
            Text("⏳ Has vivido ${formatter.format(secondsLived)} segundos")
            Text("❤️ Tu corazón ha latido unas ${formatter.format(heartbeats)} veces")
            Text("🩸 Tu corazón ha bombeado unos ${formatter.format(bloodLiters)} litros de sangre")
            Text("👁️ Has parpadeado unas ${formatter.format(blinks)} veces")
            Text("🌬️ Has respirado unas ${formatter.format(breaths)} veces")
            Text("🌕 Has presenciado unas ${formatter.format(fullMoonsSeen)} lunas llenas")
            Text("📊 Ha pasado el ${formatter.format(percentSinceBday)}% del año desde tu último cumpleaños")
            Text(
                if (dayOfWeek.isNotEmpty())
                    "📅 Naciste un día $dayOfWeek"
                else
                    "📅 Día de la semana no disponible"
            )
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Acerca de Calculadora de Edad") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Para qué sirve: Calcula tu edad y estadísticas relacionadas.")
                    Text("• Guía rápida:")
                    Text("   – Ingresa fecha en formato DD/MM/YYYY.")
                    Text("   – Mes entre 01–12; día válido según mes/año; no puede ser futura.")
                    Text("   – Los datos curiosos aparecen siempre y se actualizan con tu fecha.")
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