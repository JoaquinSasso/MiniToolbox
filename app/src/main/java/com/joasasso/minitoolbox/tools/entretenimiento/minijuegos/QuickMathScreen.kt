// QuickMathScreen.kt
package com.joasasso.minitoolbox.tools.entretenimiento.minijuegos

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

val Context.dataStore by preferencesDataStore(name = "calculos_rapidos")

@Composable
fun CalculosRapidosScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var showInfo by remember { mutableStateOf(false) }

    var currentQuestion by remember { mutableStateOf(generateQuestion()) }
    var score by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(1f) }

    val highScoreFlow = remember { obtenerHighScore(context) }
    val highScore by highScoreFlow.collectAsState(initial = 0)

    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var buttonsEnabled by remember { mutableStateOf(true) }

    val initialTimerDuration = 10_000L
    var currentTimerDuration by remember { mutableLongStateOf(initialTimerDuration) }
    val timerInterval = 100L
    var timerJob by remember { mutableStateOf<Job?>(null) }
    var shuffledOptions by remember(currentQuestion) { mutableStateOf(currentQuestion.options.shuffled()) }


    val defaultBg = MaterialTheme.colorScheme.background
    var bgFlashColor by remember { mutableStateOf(defaultBg) }
    val animatedBgColor by animateColorAsState(targetValue = bgFlashColor, label = "bgColor")

    var lost by remember { mutableStateOf(false) }

    fun vibrate(ms: Long = 300, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.vibrate(VibrationEffect.createOneShot(ms, amplitude))
    }

    fun restartTimer() {
        timerJob?.cancel()
        progress = 1f
        timerJob = scope.launch {
            val startTime = System.currentTimeMillis()
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTime
                val remaining = currentTimerDuration - elapsed
                progress = 1f - (elapsed.toFloat() / currentTimerDuration)
                if (remaining <= 0L) {
                    vibrate()
                    lost = true
                    break
                }
                delay(timerInterval)
            }
        }
    }


    fun handleAnswer(selected: Int) {
        if (!buttonsEnabled) return
        selectedOption = selected
        buttonsEnabled = false
        timerJob?.cancel()
        if (selected == currentQuestion.correctAnswer) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            score++
            currentTimerDuration = (currentTimerDuration - 200).coerceAtLeast(2000)
            scope.launch {
                delay(1000)
                bgFlashColor = defaultBg
                currentQuestion = generateQuestion()
                selectedOption = null
                buttonsEnabled = true
                restartTimer()
            }
        } else lost = true
    }

    fun resetGame() {
        score = 0
        currentQuestion = generateQuestion()
        currentTimerDuration = initialTimerDuration
        lost = false
        selectedOption = null
        buttonsEnabled = true
        bgFlashColor = defaultBg
        restartTimer()
    }

    LaunchedEffect(Unit) {
        restartTimer()
    }

    LaunchedEffect(lost) {
        if (lost) {
            bgFlashColor = Color(0xFFC53737)
            buttonsEnabled = false
            vibrate(400, 255)
            scope.launch {
                guardarHighScoreSiEsMayor(context, score)
            }
            delay(1000)
            resetGame()
        }
    }

    Scaffold(
        topBar = { TopBarReusable(stringResource(R.string.tool_quick_math), onBack, { showInfo = true }) },
        containerColor = animatedBgColor,
        bottomBar = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    stringResource(
                        R.string.quickmath_tiempo_restante,
                        abs(progress * (currentTimerDuration / 1000f))
                    ),
                    style = MaterialTheme.typography.titleMedium
                )

                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = ProgressIndicatorDefaults.linearTrackColor,
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )
                Spacer(Modifier.height(40.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .background(animatedBgColor),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.quickmath_record, highScore),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                stringResource(R.string.quickmath_puntaje, score),
                style = MaterialTheme.typography.headlineSmall
            )

            Text(currentQuestion.questionText, style = MaterialTheme.typography.headlineMedium)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                shuffledOptions.forEach { option ->
                    val bgColor = when {
                        selectedOption == null -> MaterialTheme.colorScheme.primaryContainer
                        option == currentQuestion.correctAnswer -> Color(0xFF4CAF50)
                        else -> Color(0xFFF44336)
                    }

                    Button(
                        onClick = { handleAnswer(option) },
                        enabled = buttonsEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = bgColor,
                            disabledContainerColor = bgColor,
                            contentColor = Color.Black,
                            disabledContentColor = Color.Black
                        )
                    ) {
                        Text(option.toString())
                    }
                }
            }
        }
    }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.quickmath_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.quickmath_help_line1))
                    Text(stringResource(R.string.quickmath_help_line2))
                    Text(stringResource(R.string.quickmath_help_line3))
                    Text(stringResource(R.string.quickmath_help_line4))
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

// Lógica de preguntas y puntuación
data class MathQuestion(val questionText: String, val correctAnswer: Int, val options: List<Int>)

fun generateQuestion(): MathQuestion {
    val a = (1..10).random()
    val b = (1..10).random()
    val op = listOf("+", "-", "×").random()
    val correct = when (op) {
        "+" -> a + b
        "-" -> a - b
        "×" -> a * b
        else -> 0
    }
    val question = "$a $op $b = ?"
    val options = mutableSetOf(correct)
    while (options.size < 4) options.add(correct + (-10..10).random())
    return MathQuestion(question, correct, options.toList())
}

private val HIGH_SCORE_KEY = intPreferencesKey("high_score")

fun obtenerHighScore(context: Context): Flow<Int> {
    return context.dataStore.data.map { it[HIGH_SCORE_KEY] ?: 0 }
}

suspend fun guardarHighScoreSiEsMayor(context: Context, nuevoPuntaje: Int) {
    context.dataStore.edit { prefs ->
        val actual = prefs[HIGH_SCORE_KEY] ?: 0
        if (nuevoPuntaje > actual) {
            prefs[HIGH_SCORE_KEY] = nuevoPuntaje
        }
    }
}
