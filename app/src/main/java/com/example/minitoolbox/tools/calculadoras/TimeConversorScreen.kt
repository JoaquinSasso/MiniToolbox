package com.example.minitoolbox.tools.calculadoras

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversorHorasScreen(onBack: () -> Unit) {
    var showInfo by remember { mutableStateOf(false) }

    var hora12 by remember { mutableStateOf(TextFieldValue("")) }
    var amPm by remember { mutableStateOf("AM") }
    var hora24 by remember { mutableStateOf(TextFieldValue("")) }

    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var userEditing by remember { mutableStateOf("none") } // "12" o "24" o "none"
    var error12 by remember { mutableStateOf<String?>(null) }
    var error24 by remember { mutableStateOf<String?>(null) }

    fun autoFormatHora(input: TextFieldValue): TextFieldValue {
        val raw = input.text.filter { it.isDigit() }
        var formatted = raw
        var newCursor = input.selection.end

        // Insert ':' after two digits (but only if not already there)
        if (raw.length > 2) {
            formatted = raw.substring(0, 2) + ":" + raw.substring(2, raw.length.coerceAtMost(4))
            // Ajustar posición del cursor si se insertó un ':'
            if (input.text.length < formatted.length && input.selection.end > 2) {
                newCursor++
            }
        }
        // Truncar si hay más de 5 caracteres
        formatted = formatted.take(5)
        newCursor = newCursor.coerceIn(0, formatted.length)
        return TextFieldValue(
            text = formatted,
            selection = TextRange(newCursor)
        )
    }

    fun reset() {
        hora12 = TextFieldValue("")
        amPm = "AM"
        hora24 = TextFieldValue("")
        error12 = null
        error24 = null
        focusManager.clearFocus()
        userEditing = "none"
    }

    fun actualizarDesde12() {
        error12 = null
        val match = Regex("""^(\d{1,2}):(\d{2})$""").matchEntire(hora12.text.trim())
        if (match != null) {
            val h = match.groupValues[1].toIntOrNull()
            val m = match.groupValues[2].toIntOrNull()
            if (h in 1..12 && m in 0..59) {
                val h24 = if (amPm == "AM") {
                    if (h == 12) 0 else h
                } else {
                    if (h == 12) 12 else h?.plus(12)
                }
                hora24 = TextFieldValue("%02d:%02d".format(h24, m!!))
                error24 = null
            } else {
                error12 = "Hora inválida"
                hora24 = TextFieldValue("")
            }
        } else if (hora12.text.isEmpty()) {
            hora24 = TextFieldValue("")
            error12 = null
        } else {
            error12 = "Formato: HH:MM"
            hora24 = TextFieldValue("")
        }
    }

    fun actualizarDesde24() {
        error24 = null
        val match = Regex("""^(\d{1,2}):(\d{2})$""").matchEntire(hora24.text.trim())
        if (match != null) {
            val h = match.groupValues[1].toIntOrNull()
            val m = match.groupValues[2].toIntOrNull()
            if (h in 0..23 && m in 0..59) {
                val h12 = when {
                    h == 0 -> 12
                    h in 1..12 -> h
                    else -> h?.minus(12)
                }
                if (h != null) {
                    amPm = if (h < 12) "AM" else "PM"
                }
                hora12 = TextFieldValue("%02d:%02d".format(h12, m!!))
                error12 = null
            } else {
                error24 = "Hora inválida"
                hora12 = TextFieldValue("")
            }
        } else if (hora24.text.isEmpty()) {
            hora12 = TextFieldValue("")
            error24 = null
        } else {
            error24 = "Formato: HH:MM"
            hora12 = TextFieldValue("")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conversor de Horas 12h / 24h") },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showInfo = true
                    }) {
                        Icon(Icons.Filled.Info, contentDescription = "Información")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 12 horas
            Text(
                "Formato 12 horas",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedTextField(
                    value = hora12,
                    onValueChange = {
                        val formatted = autoFormatHora(it)
                        hora12 = formatted
                        userEditing = "12"
                        actualizarDesde12()
                    },
                    label = { Text("HH:MM") },
                    singleLine = true,
                    isError = error12 != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    supportingText = {
                        error12?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier.width(120.dp)
                )
                Spacer(Modifier.width(8.dp))
                AmPmSquareToggle(selected = amPm) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    amPm = it
                    if (userEditing == "12") actualizarDesde12()
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(hora12.text.padEnd(5, ' ')+ " " + amPm))
                        scope.launch {
                            snackbarHostState.showSnackbar("Hora 12h copiada")
                        }
                    },
                    enabled = hora12.text.isNotBlank() && error12 == null
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar resultado")
                    Spacer(Modifier.width(4.dp))
                    Text("Copiar")
                }
            }
            // 24 horas
            Text(
                "Formato 24 horas",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedTextField(
                    value = hora24,
                    onValueChange = {
                        val formatted = autoFormatHora(it)
                        hora24 = formatted
                        userEditing = "24"
                        actualizarDesde24()
                    },
                    label = { Text("HH:MM") },
                    singleLine = true,
                    isError = error24 != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    supportingText = {
                        error24?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier.width(120.dp)
                )
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(hora24.text))
                        scope.launch {
                            snackbarHostState.showSnackbar("Hora 24h copiada")
                        }
                    },
                    enabled = hora24.text.isNotBlank() && error24 == null
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar resultado")
                    Spacer(Modifier.width(4.dp))
                    Text("Copiar")
                }
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    reset()
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Resetear")
                Spacer(Modifier.width(8.dp))
                Text("Resetear todo")
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Acerca del conversor de horas") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Convierte horas entre formato 12h (AM/PM) y 24h.")
                    Text("• Escribí la hora en uno de los campos (formato HH:MM).")
                    Text("• El otro campo se actualiza automáticamente.")
                    Text("• El botón 'Copiar' te permite guardar el resultado en el portapapeles.")
                    Text("• Usá 'Resetear todo' para limpiar ambos campos.")
                    Spacer(Modifier.height(8.dp))
                    Text("Ejemplos:")
                    Text("  1:30 PM → 13:30")
                    Text("  00:15  → 12:15 AM")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showInfo = false
                }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

// Toggle cuadrado y compacto para AM/PM con texto explicativo
@Composable
fun AmPmSquareToggle(selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .height(48.dp)
            .wrapContentWidth()
    ) {
        Button(
            onClick = { onSelect("AM") },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected == "AM") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (selected == "AM") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(48.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AM",
                    fontSize = 18.sp,
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Button(
            onClick = { onSelect("PM") },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selected == "PM") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (selected == "PM") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.size(48.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PM",
                    fontSize = 18.sp,
                    maxLines = 1
                )
            }
        }
    }
}


