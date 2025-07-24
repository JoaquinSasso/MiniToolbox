// DecimalBinaryConverterScreen.kt
package com.joasasso.minitoolbox.tools.calculadoras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
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
            TopBarReusable(
                stringResource(R.string.tool_decimal_binary),
                onBack,
                { showInfo = true }
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
            // Campo Decimal
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
                label = { Text(stringResource(R.string.decimal_binario_decimal)) },
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
                            contentDescription = stringResource(R.string.copy)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Campo Binario
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
                label = { Text(stringResource(R.string.decimal_binario_binario)) },
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
                            contentDescription = stringResource(R.string.copy)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }

// Diálogo de ayuda
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.decimal_binario_titulo_ayuda)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.decimal_binario_linea1))
                    Text(stringResource(R.string.decimal_binario_linea2))
                    Text(stringResource(R.string.decimal_binario_linea3))
                    Text(stringResource(R.string.decimal_binario_linea4))
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
