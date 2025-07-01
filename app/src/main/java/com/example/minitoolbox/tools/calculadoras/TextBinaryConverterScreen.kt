// app/src/main/java/com/example/minitoolbox/tools/calculadoras/TextBinaryConverterScreen.kt
package com.example.minitoolbox.tools.calculadoras

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextBinaryConverterScreen(onBack: () -> Unit) {
    var textInput by remember { mutableStateOf("") }
    var binaryRaw by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val haptic    = LocalHapticFeedback.current
    var showInfo  by remember { mutableStateOf(false) }

    // VisualTransformation que inserta un espacio cada 8 bits sin alterar el texto subyacente
    val binaryTransformation = remember {
        object : VisualTransformation {
            override fun filter(text: AnnotatedString): TransformedText {
                val raw = text.text
                val sb = StringBuilder()
                raw.forEachIndexed { i, c ->
                    sb.append(c)
                    if ((i + 1) % 8 == 0 && i + 1 < raw.length) sb.append(' ')
                }
                val out = sb.toString()
                val outLen = out.length
                val rawLen = raw.length

                val offsetTranslator = object : OffsetMapping {
                    override fun originalToTransformed(offset: Int): Int {
                        val spacesBefore = offset / 8
                        val candidate = offset + spacesBefore
                        return candidate.coerceIn(0, outLen)
                    }
                    override fun transformedToOriginal(offset: Int): Int {
                        val spacesBefore = offset / 9
                        val candidate = offset - spacesBefore
                        return candidate.coerceIn(0, rawLen)
                    }
                }
                return TransformedText(AnnotatedString(out), offsetTranslator)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Texto ↔ Binario") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showInfo = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }) {
                        Icon(Icons.Filled.Info, contentDescription = "Información")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Texto → Binario
            OutlinedTextField(
                value = textInput,
                onValueChange = { new ->
                    textInput = new
                    // Actualiza raw binario sin espacios
                    binaryRaw = new
                        .map { it.code.toString(2).padStart(8, '0') }
                        .joinToString(separator = "")
                },
                label = { Text("Texto") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                maxLines = 6,
                trailingIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        clipboard.setText(AnnotatedString(textInput))
                    }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar texto")
                    }
                }
            )

            // Binario → Texto, con espacios visuales cada 8 bits
            OutlinedTextField(
                value = binaryRaw,
                onValueChange = { new ->
                    // Filtra solo 0 y 1
                    binaryRaw = new.filter { it == '0' || it == '1' }
                    // Actualiza texto a partir de cada byte completo
                    textInput = binaryRaw.chunked(8)
                        .mapNotNull { it.toIntOrNull(2)?.toChar() }
                        .joinToString("")
                },
                label = { Text("Binario") },
                visualTransformation = binaryTransformation,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                maxLines = 6,
                trailingIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        // Copia el texto con espacios para legibilidad
                        val spaced = binaryTransformation
                            .filter(AnnotatedString(binaryRaw)).text.text
                        clipboard.setText(AnnotatedString(spaced))
                    }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar binario")
                    }
                }
            )
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Acerca de Texto ↔ Binario") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Para qué sirve: Convierte texto a código binario ASCII y viceversa.")
                    Text("• Guía rápida:")
                    Text("   – Escribe texto para ver su representación binaria.")
                    Text("   – Escribe ceros y unos; se agrupan visualmente en bytes (8 bits) sin afectar la edición.")
                    Text("   – Usa 📋 para copiar el resultado.")
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

class DateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.take(8)
        val out = buildString {
            digits.forEachIndexed { i, c ->
                append(c)
                if (i == 1 || i == 3) append('/')
            }
        }
        val offsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = when {
                offset <= 1 -> offset
                offset <= 3 -> offset + 1
                offset <= 8 -> offset + 2
                else        -> out.length
            }
            override fun transformedToOriginal(offset: Int): Int = when {
                offset <= 2  -> offset
                offset <= 5  -> offset - 1
                offset <= 10 -> offset - 2
                else         -> digits.length
            }
        }
        return TransformedText(AnnotatedString(out), offsetTranslator)
    }
}

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
        topBar = {
            TopAppBar(
                title = { Text("Calculadora de Edad") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showInfo = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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