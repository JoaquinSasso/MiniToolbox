package com.joasasso.minitoolbox.tools.info

import android.speech.tts.TextToSpeech
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.categoriasDisponibles
import com.joasasso.minitoolbox.data.idiomasDisponibles
import com.joasasso.minitoolbox.data.todasLasFrases
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicPhrasesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val tts = remember {
        TextToSpeech(context, null).apply {
            this.language = Locale("es") // Or language = Locale("es")
        }
    }
    val haptic = LocalHapticFeedback.current

    var selectedLanguage by remember { mutableStateOf(idiomasDisponibles.first()) }
    var selectedCategory by remember { mutableStateOf(categoriasDisponibles.first()) }

    val filteredPhrases = todasLasFrases.filter {
        it.categoria == selectedCategory && selectedLanguage.codigo in it.traducciones
    }

    // Dropdown states
    var expandedLanguage by remember { mutableStateOf(false) }
    var expandedCategory by remember { mutableStateOf(false) }

    // Ayuda
    var showInfo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopBarReusable(stringResource(R.string.tool_basic_phrases), onBack = onBack, { showInfo = true })
        },
        content = { padding ->
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
                    // Selector de idioma
                    ExposedDropdownMenuBox(
                        expanded = expandedLanguage,
                        onExpandedChange = { expandedLanguage = !expandedLanguage },
                        modifier = Modifier.weight(1f)
                    ) {
                        TextField(
                            value = selectedLanguage.nombre,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Idioma") },
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
                                    text = { Text(idioma.nombre) },
                                    onClick = {
                                        selectedLanguage = idioma
                                        expandedLanguage = false
                                    }
                                )
                            }
                        }
                    }

                    // Selector de categoría
                    ExposedDropdownMenuBox(
                        expanded = expandedCategory,
                        onExpandedChange = { expandedCategory = !expandedCategory },
                        modifier = Modifier.weight(1f)
                    ) {
                        TextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Categoría") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            modifier = Modifier.menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = expandedCategory,
                            onDismissRequest = { expandedCategory = false }
                        ) {
                            categoriasDisponibles.forEach { categoria ->
                                DropdownMenuItem(
                                    text = { Text(categoria) },
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
                            modifier = Modifier
                                .fillMaxWidth(),
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
                                Column {
                                    Text(
                                        text = frase.fraseBase,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = frase.traducciones[selectedLanguage.codigo] ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(onClick = {
                                    val frasePronunciar = frase.traducciones[selectedLanguage.codigo]
                                    frasePronunciar?.let {
                                        tts.language = selectedLanguage.locale
                                        tts.speak(it, TextToSpeech.QUEUE_FLUSH, null, null)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Reproducir")
                                }
                            }
                        }
                    }
                }
            }
        }
    )

    // Ventana de ayuda
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("¿Cómo funciona?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Usa esta herramienta para aprender frases útiles en situaciones turísticas.")
                    Text("• Selecciona un idioma y una categoría para ver frases comunes.")
                    Text("• Toca el ícono de altavoz para escuchar la pronunciación con el lector de voz de tu dispositivo.")
                    Text("• No se requiere conexión a internet.")
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


