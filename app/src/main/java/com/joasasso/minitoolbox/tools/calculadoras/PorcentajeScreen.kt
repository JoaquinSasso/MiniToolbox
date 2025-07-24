package com.joasasso.minitoolbox.tools.calculadoras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PorcentajeScreen(onBack: () -> Unit) {
    var showInfo by remember { mutableStateOf(false) }

    var baseInput1 by remember { mutableStateOf("") }
    var percentInput by remember { mutableStateOf("") }
    var valueInput by remember { mutableStateOf("") }
    var baseInput2 by remember { mutableStateOf("") }

    var result1 by remember { mutableStateOf<String?>(null) }
    var result2 by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    fun calcularPorcentajeDeNumero() {
        val percent = percentInput.toFloatOrNull()
        val base = baseInput1.toFloatOrNull()
        result1 = if (percent != null && base != null) {
            "%.2f".format((percent / 100) * base)
        } else {
            null
        }
        focusManager.clearFocus()
    }

    fun calcularQuePorcentajeDeYEsX() {
        val value = valueInput.toFloatOrNull()
        val base = baseInput2.toFloatOrNull()
        result2 = if (value != null && base != null && base != 0f) {
            "%.2f%%".format(100 * value / base)
        } else {
            null
        }
        focusManager.clearFocus()
    }

    fun resetearTodo() {
        percentInput = ""
        baseInput1 = ""
        valueInput = ""
        baseInput2 = ""
        result1 = null
        result2 = null
        focusManager.clearFocus()
    }

    Scaffold(
        topBar = {
            TopBarReusable(
                stringResource(R.string.tool_percentage),
                onBack,
                { showInfo = true })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.percentage_subtitle1),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = percentInput,
                    onValueChange = { percentInput = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(R.string.percentage_label_percent)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { calcularPorcentajeDeNumero() }
                    ),
                    modifier = Modifier.width(100.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.percentage_de), fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = baseInput1,
                    onValueChange = { baseInput1 = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(R.string.percentage_label_number)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { calcularPorcentajeDeNumero() }
                    ),
                    modifier = Modifier.width(130.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { calcularPorcentajeDeNumero() },
                    enabled = percentInput.isNotBlank() && baseInput1.isNotBlank()
                ) {
                    Text(stringResource(R.string.calculate))
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = {
                        clipboardManager.setText(
                            androidx.compose.ui.text.AnnotatedString(
                                result1 ?: ""
                            )
                        )
                    },
                    enabled = result1 != null
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.copy)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.copy))
                }
            }
            result1?.let {
                Text(
                    "${stringResource(R.string.result)}: $it",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            HorizontalDivider(
                Modifier.padding(vertical = 12.dp),
                DividerDefaults.Thickness,
                DividerDefaults.color
            )

            Text(
                stringResource(R.string.percentage_subtitle2),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = valueInput,
                    onValueChange = { valueInput = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(R.string.percentage_label_value)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { calcularQuePorcentajeDeYEsX() }
                    ),
                    modifier = Modifier.width(120.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.percentage_en), fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = baseInput2,
                    onValueChange = { baseInput2 = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(R.string.percentage_label_number_y)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { calcularQuePorcentajeDeYEsX() }
                    ),
                    modifier = Modifier.width(130.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { calcularQuePorcentajeDeYEsX() },
                    enabled = valueInput.isNotBlank() && baseInput2.isNotBlank()
                ) {
                    Text(stringResource(R.string.calculate))
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = {
                        clipboardManager.setText(
                            androidx.compose.ui.text.AnnotatedString(
                                result2 ?: ""
                            )
                        )
                    },
                    enabled = result2 != null
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.copy)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.copy))
                }
            }
            result2?.let {
                Text(
                    "${stringResource(R.string.result)}: $it",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { resetearTodo() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.reset))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.reset))
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.percentage_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.percentage_help_line1))
                    Text(stringResource(R.string.percentage_help_line2))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.percentage_help_line3))
                    Text(stringResource(R.string.percentage_help_line4))
                    Text(stringResource(R.string.percentage_help_line5))
                    Text(stringResource(R.string.percentage_help_line6))
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


