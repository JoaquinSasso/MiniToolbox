// app/src/main/java/com/example/minitoolbox/tools/calculadoras/TextBinaryConverterScreen.kt
package com.example.minitoolbox.tools.calculadoras

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.unit.dp

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
