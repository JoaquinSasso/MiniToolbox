package com.joasasso.minitoolbox.ui.screens

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.Categoria
import com.joasasso.minitoolbox.data.Frase
import com.joasasso.minitoolbox.data.idiomasDisponibles
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicPhrasesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val tts = remember {
        TextToSpeech(context, null).apply {
            this.language = Locale.getDefault()
        }
    }
    val haptic = LocalHapticFeedback.current

    val frases: List<Frase> = remember {
        cargarFrasesDesdeJson(context)
    }

    val locale = LocalConfiguration.current.locales[0]
    val mostrarCodigo = locale.language

    var selectedLanguage by remember { mutableStateOf(idiomasDisponibles.first({ it.codigo != mostrarCodigo })) }
    var selectedCategory by remember { mutableStateOf(Categoria.GREETINGS) }

    val filteredPhrases = frases.filter {
        it.categoria == selectedCategory.id &&
                it.traducciones.containsKey(mostrarCodigo) &&
                it.traducciones.containsKey(selectedLanguage.codigo)
    }

    var expandedLanguage by remember { mutableStateOf(false) }
    var expandedCategory by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopBarReusable(stringResource(R.string.tool_basic_phrases), onBack = onBack, { showInfo = true })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Idioma
                ExposedDropdownMenuBox(
                    expanded = expandedLanguage,
                    onExpandedChange = { expandedLanguage = !expandedLanguage },
                    modifier = Modifier.weight(1f)
                ) {
                    TextField(
                        value = stringResource(id = selectedLanguage.nombreResId),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.frases_idioma)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLanguage) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedLanguage,
                        onDismissRequest = { expandedLanguage = false }
                    ) {
                        idiomasDisponibles.forEach { idioma ->
                            DropdownMenuItem(
                                text = { Text(stringResource(id = idioma.nombreResId)) },
                                onClick = {
                                    selectedLanguage = idioma
                                    expandedLanguage = false
                                }
                            )
                        }
                    }
                }

                // Categoría
                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = !expandedCategory },
                    modifier = Modifier.weight(1f)
                ) {
                    TextField(
                        value = stringResource(id = selectedCategory.nombreResId),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.frases_categoria)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        Categoria.entries.forEach { categoria ->
                            DropdownMenuItem(
                                text = { Text(stringResource(id = categoria.nombreResId)) },
                                onClick = {
                                    selectedCategory = categoria
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredPhrases) { frase ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        elevation = CardDefaults.cardElevation(4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = frase.traducciones[mostrarCodigo] ?: "",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = frase.traducciones[selectedLanguage.codigo] ?: "",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            IconButton(onClick = {
                                frase.traducciones[selectedLanguage.codigo]?.let {
                                    tts.language = selectedLanguage.locale
                                    tts.speak(it, TextToSpeech.QUEUE_FLUSH, null, null)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = stringResource(R.string.frases_play))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.frases_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.frases_help_line1))
                    Text(stringResource(R.string.frases_help_line2))
                    Text(stringResource(R.string.frases_help_line3))
                    Text(stringResource(R.string.frases_help_line4))
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

fun cargarFrasesDesdeJson(context: Context): List<Frase> {
    return try {
        val input = context.assets.open("basic_phrases.json")
        val json = input.bufferedReader().use { it.readText() }
        Log.d("BasicPhrasesScreen", "Contenido JSON: ${json.take(300)}")

        val tipo = object : TypeToken<List<Frase>>() {}.type
        val frases = Gson().fromJson<List<Frase>>(json, tipo)
        Log.d("BasicPhrasesScreen", "Cantidad de frases cargadas: ${frases.size}")
        frases
    } catch (e: Exception) {
        Log.e("BasicPhrasesScreen", "Error al cargar frases", e)
        emptyList()
    }
}

