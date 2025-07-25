import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import java.util.Calendar

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
    val nameRes: Int,
    val emoji: String,
    val rangeRes: Int,
    val funFactRes: Int,
    val elementRes: Int,
    val planetRes: Int,
    val personalityRes: Int
)

fun getZodiacSign(day: Int, month: Int): ZodiacSign? {

    val signs = listOf(
        (21 to 3) to ZodiacSign(R.string.zodiac_aries, "♈", R.string.range_aries, R.string.fact_aries, R.string.element_aries, R.string.planet_aries, R.string.personality_aries),
        (20 to 4) to ZodiacSign(R.string.zodiac_taurus, "♉", R.string.range_taurus, R.string.fact_taurus, R.string.element_taurus, R.string.planet_taurus, R.string.personality_taurus),
        (21 to 5) to ZodiacSign(R.string.zodiac_gemini, "♊", R.string.range_gemini, R.string.fact_gemini, R.string.element_gemini, R.string.planet_gemini, R.string.personality_gemini),
        (21 to 6) to ZodiacSign(R.string.zodiac_cancer, "♋", R.string.range_cancer, R.string.fact_cancer, R.string.element_cancer, R.string.planet_cancer, R.string.personality_cancer),
        (23 to 7) to ZodiacSign(R.string.zodiac_leo, "♌", R.string.range_leo, R.string.fact_leo, R.string.element_leo, R.string.planet_leo, R.string.personality_leo),
        (23 to 8) to ZodiacSign(R.string.zodiac_virgo, "♍", R.string.range_virgo, R.string.fact_virgo, R.string.element_virgo, R.string.planet_virgo, R.string.personality_virgo),
        (23 to 9) to ZodiacSign(R.string.zodiac_libra, "♎", R.string.range_libra, R.string.fact_libra, R.string.element_libra, R.string.planet_libra, R.string.personality_libra),
        (23 to 10) to ZodiacSign(R.string.zodiac_scorpio, "♏", R.string.range_scorpio, R.string.fact_scorpio, R.string.element_scorpio, R.string.planet_scorpio, R.string.personality_scorpio),
        (22 to 11) to ZodiacSign(R.string.zodiac_sagittarius, "♐", R.string.range_sagittarius, R.string.fact_sagittarius, R.string.element_sagittarius, R.string.planet_sagittarius, R.string.personality_sagittarius),
        (22 to 12) to ZodiacSign(R.string.zodiac_capricorn, "♑", R.string.range_capricorn, R.string.fact_capricorn, R.string.element_capricorn, R.string.planet_capricorn, R.string.personality_capricorn),
        (20 to 1) to ZodiacSign(R.string.zodiac_aquarius, "♒", R.string.range_aquarius, R.string.fact_aquarius, R.string.element_aquarius, R.string.planet_aquarius, R.string.personality_aquarius),
        (19 to 2) to ZodiacSign(R.string.zodiac_pisces, "♓", R.string.range_pisces, R.string.fact_pisces, R.string.element_pisces, R.string.planet_pisces, R.string.personality_pisces)
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
    var dateError by remember { mutableStateOf<String?>(null) }

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
        topBar = {TopBarReusable(stringResource(R.string.tool_zodiac_sign), onBack, {showInfo = true})}
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val errorMonth = stringResource(R.string.zodiac_error_month)
            val errorDay =  stringResource(R.string.zodiac_error_day)
            var maxDay = 0

            OutlinedTextField(
                value = rawDigits,
                onValueChange = { new ->
                    rawDigits = new.filter { it.isDigit() }.take(4)
                    dateError = null
                    currentSign = null

                    if (rawDigits.length == 4) {
                        val d = rawDigits.substring(0, 2).toIntOrNull()
                        val m = rawDigits.substring(2, 4).toIntOrNull()
                        if (d != null && m != null) {
                            if (m in 1..12) {
                                val tmp = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, 2024)
                                    set(Calendar.MONTH, m - 1)
                                }
                                maxDay = tmp.getActualMaximum(Calendar.DAY_OF_MONTH)
                                //Si el mes es correcto pero los días no, se muestra un error
                                if (d !in 1..maxDay) {
                                    dateError = errorDay.format(maxDay)
                                }
                                //Si los días son correctos, se calcula el signo
                                else currentSign = getZodiacSign(d, m)
                            }
                            //Si el mes no es correcto, se muestra un error
                            else{
                                dateError = errorMonth
                            }
                        }
                    }
                },
                label = { Text(stringResource(R.string.zodiac_hint)) },
                singleLine = true,
                isError = dateError != null,
                supportingText = {
                    dateError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                },
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
                    Text("🔮 ${stringResource(R.string.your_sign)}: ${sign?.emoji ?: "..."} ${sign?.let { stringResource(it.nameRes) } ?: "..."}", style = MaterialTheme.typography.headlineSmall)
                    Text("📆 ${stringResource(R.string.date_range)}: ${sign?.let { stringResource(it.rangeRes) } ?: "..."}")
                    Text("🌟 ${stringResource(R.string.fun_fact)}: ${sign?.let { stringResource(it.funFactRes) } ?: "..."}")
                    Text("🌬️ ${stringResource(R.string.element)}: ${sign?.let { stringResource(it.elementRes) } ?: "..."}")
                    Text("🪐 ${stringResource(R.string.ruling_planet)}: ${sign?.let { stringResource(it.planetRes) } ?: "..."}")
                    Text("🧠 ${stringResource(R.string.personality)}: ${sign?.let { stringResource(it.personalityRes) } ?: "..."}")
                }
            }

            if (!showData) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔮 ${stringResource(R.string.your_sign)}: ${sign?.emoji ?: "..."} ${sign?.let { stringResource(it.nameRes) } ?: "..."}", style = MaterialTheme.typography.headlineSmall)
                    Text("📆 ${stringResource(R.string.date_range)}: ${sign?.let { stringResource(it.rangeRes) } ?: "..."}")
                    Text("🌟 ${stringResource(R.string.fun_fact)}: ${sign?.let { stringResource(it.funFactRes) } ?: "..."}")
                    Text("🌬️ ${stringResource(R.string.element)}: ${sign?.let { stringResource(it.elementRes) } ?: "..."}")
                    Text("🪐 ${stringResource(R.string.ruling_planet)}: ${sign?.let { stringResource(it.planetRes) } ?: "..."}")
                    Text("🧠 ${stringResource(R.string.personality)}: ${sign?.let { stringResource(it.personalityRes) } ?: "..."}")
                }
            }
        }
    }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.zodiac_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.zodiac_help_line1))
                    Text(stringResource(R.string.zodiac_help_line2))
                    Text(stringResource(R.string.zodiac_help_line3))
                    Text(stringResource(R.string.zodiac_help_line4))
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