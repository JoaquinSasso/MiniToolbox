package com.example.minitoolbox.tools.calculadoras

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType


class ZodiacVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.take(4)
        val out = buildString {
            digits.forEachIndexed { i, c ->
                append(c)
                if (i == 1) append('/')
            }
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = when {
                offset <= 1 -> offset
                offset <= 4 -> offset + 1
                else -> 5
            }
            override fun transformedToOriginal(offset: Int): Int = when {
                offset <= 2 -> offset
                offset <= 5 -> offset - 1
                else -> 4
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

data class ZodiacSign(
    val name: String,
    val emoji: String,
    val range: String,
    val funFact: String,
    val element: String,
    val planet: String,
    val personality: String
)

fun getZodiacSign(day: Int, month: Int): ZodiacSign? {
    val signs = listOf(
        (21 to 3) to ZodiacSign("Aries", "♈", "21 Mar - 19 Abr", "🔥 Es el primer signo del zodiaco y representa el inicio de todo.",
            "🔥 Fuego", "🪖 Marte", "⚡ Impulsivo, valiente, competitivo"),
        (20 to 4) to ZodiacSign("Tauro", "♉", "20 Abr - 20 May", "🌿 Ama el confort y lo tangible.",
            "🌍 Tierra", "💖 Venus", "😌 Paciente, estable, leal"),
        (21 to 5) to ZodiacSign("Géminis", "♊", "21 May - 20 Jun", "🌀 Tiene facilidad para adaptarse a cualquier situación.",
            "💨 Aire", "📬 Mercurio", "💬 Curioso, comunicativo, versátil"),
        (21 to 6) to ZodiacSign("Cáncer", "♋", "21 Jun - 22 Jul", "🌙 Tiene una memoria emocional muy poderosa.",
            "💧 Agua", "🌕 Luna", "🤍 Protector, intuitivo, sentimental"),
        (23 to 7) to ZodiacSign("Leo", "♌", "23 Jul - 22 Ago", "🌟 Adora el escenario y brillar con luz propia.",
            "🔥 Fuego", "☀️ Sol", "🎭 Líder, generoso, creativo"),
        (23 to 8) to ZodiacSign("Virgo", "♍", "23 Ago - 22 Sep", "🔍 Detecta errores con facilidad.",
            "🌍 Tierra", "📬 Mercurio", "🧠 Analítico, ordenado, perfeccionista"),
        (23 to 9) to ZodiacSign("Libra", "♎", "23 Sep - 22 Oct", "⚖️ Ama el equilibrio y la estética.",
            "💨 Aire", "💖 Venus", "🎨 Diplomático, sociable, justo"),
        (23 to 10) to ZodiacSign("Escorpio", "♏", "23 Oct - 21 Nov", "🦂 Tiene una intensidad emocional profunda.",
            "💧 Agua", "🌑 Plutón", "🔮 Misterioso, apasionado, decidido"),
        (22 to 11) to ZodiacSign("Sagitario", "♐", "22 Nov - 21 Dic", "🏹 Siempre quiere explorar más allá.",
            "🔥 Fuego", "🪐 Júpiter", "🌍 Optimista, aventurero, libre"),
        (22 to 12) to ZodiacSign("Capricornio", "♑", "22 Dic - 19 Ene", "🏔️ Es el más disciplinado del zodiaco.",
            "🌍 Tierra", "🪐 Saturno", "📈 Responsable, ambicioso, práctico"),
        (20 to 1) to ZodiacSign("Acuario", "♒", "20 Ene - 18 Feb", "🧠 Tiene ideas adelantadas a su tiempo.",
            "💨 Aire", "⚡ Urano", "🤖 Innovador, excéntrico, independiente"),
        (19 to 2) to ZodiacSign("Piscis", "♓", "19 Feb - 20 Mar", "🌊 Está muy conectado con lo espiritual.",
            "💧 Agua", "🌊 Neptuno", "🌙 Empático, soñador, sensible")
    ).sortedBy { it.first.second * 100 + it.first.first }

    val input = month * 100 + day
    for (i in signs.indices) {
        val current = signs[i].first
        val currentDate = current.second * 100 + current.first
        val nextDate = if (i + 1 < signs.size) {
            val next = signs[i + 1].first
            next.second * 100 + next.first
        } else 321

        if (input in currentDate until nextDate || (i == signs.lastIndex && input < 321)) {
            return signs[i].second
        }
    }

    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZodiacSignScreen(onBack: () -> Unit) {
    var rawDigits by remember { mutableStateOf("") }
    var currentSign by remember { mutableStateOf<ZodiacSign?>(null) }
    val showData = currentSign != null
    var showInfo    by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    fun calculateSign() {
        if (rawDigits.length == 4) {
            val day = rawDigits.substring(0, 2).toIntOrNull() ?: return
            val month = rawDigits.substring(2, 4).toIntOrNull() ?: return
            if (day in 1..31 && month in 1..12) {
                currentSign = getZodiacSign(day, month)
            }
        } else {
            currentSign = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Signo Zodiacal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showInfo = true
                    }) {
                        Icon(Icons.Filled.Info, contentDescription = "Información")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = rawDigits,
                onValueChange = {
                    rawDigits = it.filter { c -> c.isDigit() }.take(4)
                    calculateSign()
                },
                label = { Text("Fecha de nacimiento (DD/MM)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = ZodiacVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            val sign = currentSign

            AnimatedVisibility(
                visible = showData,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔮 Tu signo es: ${sign?.emoji ?: "..."} ${sign?.name ?: "..."}", style = MaterialTheme.typography.headlineSmall)
                    Text("📆 Rango de fechas: ${sign?.range ?: "..."}")
                    Text("🌟 Curiosidad: ${sign?.funFact ?: "..."}")
                    Text("🌬️ Elemento: ${sign?.element ?: "..."}")
                    Text("🪐 Planeta regente: ${sign?.planet ?: "..."}")
                    Text("🧠 Personalidad típica: ${sign?.personality ?: "..."}")
                }
            }

            if (!showData) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔮 Tu signo es: ...", style = MaterialTheme.typography.headlineSmall)
                    Text("📆 Rango de fechas: ...")
                    Text("🌟 Curiosidad: ...")
                    Text("🌬️ Elemento: ...")
                    Text("🪐 Planeta regente: ...")
                    Text("🧠 Personalidad típica: ...")
                }
            }
        }
    }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Acerca de Signo Zodiacal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Para qué sirve: Identifica tu signo zodiacal según tu fecha de nacimiento.")
                    Text("• Guía rápida:")
                    Text("   – Ingresa tu fecha de cumpleaños en formato DD/MM.")
                    Text("   – Si es válido, veras tu signo zodiacal y datos curiosos.")
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
