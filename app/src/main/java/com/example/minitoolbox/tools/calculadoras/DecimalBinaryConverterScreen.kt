// DecimalBinaryConverterScreen.kt
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.math.BigInteger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecimalBinaryConverterScreen(onBack: () -> Unit) {
    var decimalInput by remember { mutableStateOf("") }
    var binaryInput by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var showInfo    by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Decimal ↔ Binario") },
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = decimalInput,
                onValueChange = { new ->
                    val filtered = new.filter(Char::isDigit)
                    decimalInput = filtered
                    binaryInput = if (filtered.isNotEmpty()) {
                        runCatching { BigInteger(filtered).toString(2) }
                            .getOrNull()
                            ?: binaryInput
                    } else ""
                },
                label = { Text("Decimal") },
                singleLine = false,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        clipboard.setText(AnnotatedString(decimalInput))
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copiar"
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor   = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor      = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor    = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = binaryInput,
                onValueChange = { new ->
                    val filtered = new.filter { it == '0' || it == '1' }
                    binaryInput = filtered
                    decimalInput = if (filtered.isNotEmpty()) {
                        runCatching { BigInteger(filtered, 2).toString() }
                            .getOrNull()
                            ?: decimalInput
                    } else ""
                },
                label = { Text("Binario") },
                singleLine = false,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        clipboard.setText(AnnotatedString(binaryInput))
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copiar"
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor   = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor      = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor    = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Acerca de Decimal ↔ Binario") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Para qué sirve: Convierte entre números decimales y su representación binaria.")
                    Text("• Guía rápida:")
                    Text("   – Ingresa un número decimal para ver su representación en binario.")
                    Text("   – También puedes ingresar un número binario para ver su representación decimal.")
                    Text("   – Pulsa 📋 para copiar el resultado.")
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
