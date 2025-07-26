package com.joasasso.minitoolbox.tools.entretenimiento.minijuegos

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.joasasso.minitoolbox.data.CapitalOfCountry
import com.joasasso.minitoolbox.data.CountryOuterClass
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

@Composable
fun AdivinaCapitalScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var countries by remember { mutableStateOf<List<CapitalOfCountry>>(emptyList()) }
    var currentQuestion by remember { mutableStateOf<CapitalOfCountry?>(null) }

    var score by remember { mutableIntStateOf(0) }
    var record by remember { mutableIntStateOf(0) }
    var showInfo by remember { mutableStateOf(false) }

    var lastResult by remember { mutableStateOf<String?>(null) }

    var selectedOption by remember { mutableStateOf<String?>(null) }
    var correctOption by remember { mutableStateOf<String?>(null) }
    var buttonsEnabled by remember { mutableStateOf(true) }
    var shuffledOptions by remember { mutableStateOf<List<CapitalOfCountry>>(emptyList()) }

    var timeLeft by remember { mutableIntStateOf(100) }
    var timerRunning by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(1f) }

    val defaultBg = MaterialTheme.colorScheme.background
    var bgFlashColor by remember { mutableStateOf(defaultBg) }
    val animatedBgColor by animateColorAsState(targetValue = bgFlashColor, label = "bgColor")

    LaunchedEffect(Unit) {
        countries = loadCapitalsFromDataset(context)
        nextRoundCapital(countries) { q, opts, shuffled ->
            currentQuestion = q
            shuffledOptions = shuffled
        }
        timerRunning = true
    }

    LaunchedEffect(Unit) {
        CapitalGameDataStore.getBestScore(context).collect { record = it }
    }

    LaunchedEffect(timerRunning) {
        if (timerRunning) {
            while (timeLeft > 0) {
                delay(100)
                timeLeft--
                progress = (timeLeft / 100f).coerceIn(0f, 1f)
            }
            if (timeLeft == 0 && buttonsEnabled) {
                buttonsEnabled = false
                correctOption = currentQuestion?.name
                bgFlashColor = Color(0xFFC53737)
                lastResult = "¡Se acabó el tiempo! Tu puntuación se reinició."
                score = 0
                timerRunning = false

                scope.launch {
                    delay(1000)
                    bgFlashColor = defaultBg
                    selectedOption = null
                    correctOption = null
                    buttonsEnabled = true
                    timeLeft = 100
                    timerRunning = true
                    nextRoundCapital(countries) { q, opts, shuffled ->
                        currentQuestion = q
                        shuffledOptions = shuffled
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = { TopBarReusable(stringResource(R.string.tool_guess_capital), onBack) { showInfo = true } },
        bottomBar = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text("Tiempo restante: %.0f segundos".format(abs(progress * 10f)),
                    style = MaterialTheme.typography.titleMedium
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = ProgressIndicatorDefaults.linearTrackColor,
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap
                )
                Spacer(Modifier.height(40.dp))
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(animatedBgColor)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            currentQuestion?.let { question ->
                Text("¿Que país tiene esta cápital?", style = MaterialTheme.typography.titleLarge)
                Text(question.capital, style = MaterialTheme.typography.headlineLarge)


                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(shuffledOptions) { option ->
                        val bgColor = when {
                            selectedOption == null -> MaterialTheme.colorScheme.primaryContainer
                            option.capital == correctOption -> Color(0xFF4CAF50)
                            else -> Color(0xFFF44336)
                        }

                        Button(
                            onClick = {
                                if (!buttonsEnabled) return@Button
                                buttonsEnabled = false
                                selectedOption = option.name
                                correctOption = currentQuestion?.capital

                                val correct = option.capital == currentQuestion?.capital

                                if (correct) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    score++
                                    if (score > record) {
                                        record = score
                                        scope.launch { CapitalGameDataStore.setBestScore(context, score) }
                                        lastResult = "¡Nuevo récord!"
                                    } else {
                                        lastResult = null
                                    }
                                } else {
                                    vibrate(context, duration = 400, amplitude = 255)
                                    bgFlashColor = Color(0xFFC53737)
                                    lastResult = "¡Incorrecto! Tu puntuación se reinició."
                                    score = 0
                                }

                                scope.launch {
                                    timerRunning = false
                                    delay(1000)
                                    bgFlashColor = defaultBg
                                    selectedOption = null
                                    correctOption = null
                                    buttonsEnabled = true
                                    timeLeft = 100
                                    timerRunning = true
                                    nextRoundCapital(countries) { q, opts, shuffled ->
                                        currentQuestion = q
                                        shuffledOptions = shuffled
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = bgColor,
                                disabledContainerColor = bgColor,
                                contentColor = Color.Black,
                                disabledContentColor = Color.Black
                            ),
                            enabled = buttonsEnabled
                        ) {
                            Text(option.name)
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Puntaje: $score", style = MaterialTheme.typography.bodyLarge)
                Text("Récord: $record", style = MaterialTheme.typography.bodyLarge)
            }

            lastResult?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Acerca de Adivina la Capital") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Para qué sirve: Juego para practicar y divertirte reconociendo capitales del mundo.")
                    Text("• Cómo usar: Aparece el nombre de una capital y debes elegir el país correspondiente.")
                    Text("• Si acertás, sumás puntos. Si errás o se termina el tiempo, tu puntaje se reinicia.")
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

private suspend fun loadCapitalsFromDataset(context: Context): List<CapitalOfCountry> = withContext(Dispatchers.IO) {
    val bytes = context.assets.open("countries_dataset.pb").readBytes()
    val protoList = CountryOuterClass.CountryList.parseFrom(bytes)
    protoList.countriesList
        .filter { it.capitalList.isNotEmpty() && it.capitalList.first().isNotBlank() }
        .map {
            // capital = flag, país = name
            CapitalOfCountry(name = it.name, capital = it.capitalList.first())
        }
}

private fun nextRoundCapital(
    all: List<CapitalOfCountry>,
    onSet: (CapitalOfCountry, List<CapitalOfCountry>, List<CapitalOfCountry>) -> Unit
) {
    val correct = all.random()
    val options = buildList {
        add(correct)
        while (size < 4) {
            val candidate = all.random()
            if (candidate.capital != correct.capital && candidate !in this) add(candidate)
        }
    }
    val shuffled = options.shuffled()
    onSet(correct, options, shuffled)
}

object CapitalGameDataStore {
    private val Context.dataStore by preferencesDataStore("capital_game_prefs")
    private val BEST_SCORE = intPreferencesKey("capital_best_score")

    suspend fun getBestScore(context: Context): Flow<Int> =
        context.dataStore.data.map { it[BEST_SCORE] ?: 0 }

    suspend fun setBestScore(context: Context, score: Int) {
        context.dataStore.edit { it[BEST_SCORE] = score }
    }
}

