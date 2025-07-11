package com.joasasso.minitoolbox.tools.juegos

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.tools.data.CountryOuterClass
import com.joasasso.minitoolbox.tools.data.FlagGameDataStore
import com.joasasso.minitoolbox.tools.data.MinimalCountry
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdivinaBanderaScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var countries by remember { mutableStateOf<List<MinimalCountry>>(emptyList()) }
    var currentQuestion by remember { mutableStateOf<MinimalCountry?>(null) }

    var score by remember { mutableIntStateOf(0) }
    var record by remember { mutableIntStateOf(0) }
    var showInfo by remember { mutableStateOf(false) }

    var lastResult by remember { mutableStateOf<String?>(null) }

    var selectedOption by remember { mutableStateOf<String?>(null) }
    var correctOption by remember { mutableStateOf<String?>(null) }
    var buttonsEnabled by remember { mutableStateOf(true) }
    var shuffledOptions by remember { mutableStateOf<List<MinimalCountry>>(emptyList()) }

    val defaultBg = MaterialTheme.colorScheme.background
    var bgFlashColor by remember { mutableStateOf(defaultBg) }
    val animatedBgColor by animateColorAsState(targetValue = bgFlashColor, label = "bgColor")

    LaunchedEffect(Unit) {
        countries = loadMinimalCountryDataset(context)
        nextRound(countries) { q, opts, shuffled ->
            currentQuestion = q
            shuffledOptions = shuffled
        }
    }

    LaunchedEffect(Unit) {
        FlagGameDataStore.getBestScore(context).collect { record = it }
    }

    Scaffold(
        topBar = { TopBarReusable("Adivina la bandera", onBack) { showInfo = true } }
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
                Text("¿De qué país es esta bandera?", style = MaterialTheme.typography.titleLarge)
                Text(question.flag, style = MaterialTheme.typography.displayLarge)

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
                            option.name == correctOption -> Color(0xFF4CAF50)
                            else -> Color(0xFFF44336)
                        }

                        Button(
                            onClick = {
                                if (!buttonsEnabled) return@Button
                                buttonsEnabled = false
                                selectedOption = option.name
                                correctOption = currentQuestion?.name

                                val correct = option.name == currentQuestion?.name

                                if (correct) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    score++
                                    if (score > record) {
                                        record = score
                                        scope.launch { FlagGameDataStore.setBestScore(context, score) }
                                        lastResult = "¡Nuevo récord!"
                                    } else {
                                        lastResult = null
                                    }
                                } else {
                                    scope.launch {
                                        repeat(3) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        delay(150) // pequeño delay entre pulsos
                                        }
                                    }

                                    bgFlashColor = Color(0xFFC53737)
                                    lastResult = "¡Incorrecto! Tu puntuación se reinició."
                                    score = 0
                                }

                                scope.launch {
                                    delay(1000)
                                    bgFlashColor = defaultBg
                                    selectedOption = null
                                    correctOption = null
                                    buttonsEnabled = true
                                    nextRound(countries) { q, opts, shuffled ->
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
            title = { Text("Acerca de Adivina la Bandera") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Para qué sirve: Juego para poner a prueba tus conocimientos sobre banderas del mundo.")
                    Text("• Cómo usar: Aparece una bandera y debes elegir el país correspondiente. Si acertás, sumás puntos. Si errás, perdés el puntaje actual.")
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

suspend fun loadMinimalCountryDataset(context: Context): List<MinimalCountry> = withContext(Dispatchers.IO) {
    val bytes = context.assets.open("countries_dataset.pb").readBytes()
    val protoList = CountryOuterClass.CountryList.parseFrom(bytes)
    protoList.countriesList.map {
        MinimalCountry(name = it.name, flag = it.flag)
    }
}

private fun nextRound(
    all: List<MinimalCountry>,
    onSet: (MinimalCountry, List<MinimalCountry>, List<MinimalCountry>) -> Unit
) {
    val correct = all.random()
    val options = buildList {
        add(correct)
        while (size < 4) {
            val candidate = all.random()
            if (candidate.name != correct.name && candidate !in this) add(candidate)
        }
    }
    val shuffled = options.shuffled()
    onSet(correct, options, shuffled)
}
