package com.example.minitoolbox.tools.calculadoras

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

enum class UnitType(val display: String) {
    Length("Longitud"),
    Weight("Peso"),
    Temperature("Temperatura"),
    Time("Tiempo")
}

data class SimpleUnit(val name: String, val symbol: String)

val lengthUnits = listOf(
    SimpleUnit("Metro", "m"),
    SimpleUnit("Centímetro", "cm"),
    SimpleUnit("Milímetro", "mm"),
    SimpleUnit("Kilómetro", "km"),
    SimpleUnit("Pulgada", "in"),
    SimpleUnit("Pie", "ft"),
    SimpleUnit("Yarda", "yd"),
    SimpleUnit("Milla", "mi")
)

val weightUnits = listOf(
    SimpleUnit("Kilogramo", "kg"),
    SimpleUnit("Gramo", "g"),
    SimpleUnit("Miligramo", "mg"),
    SimpleUnit("Tonelada", "t"),
    SimpleUnit("Libra", "lb"),
    SimpleUnit("Onza", "oz")
)

val temperatureUnits = listOf(
    SimpleUnit("Celsius", "°C"),
    SimpleUnit("Fahrenheit", "°F"),
    SimpleUnit("Kelvin", "K")
)

val timeUnits = listOf(
    SimpleUnit("Segundo", "s"),
    SimpleUnit("Minuto", "min"),
    SimpleUnit("Hora", "h"),
    SimpleUnit("Día", "d")
)

fun convertLength(value: Double, from: String, to: String): Double {
    val meters = when (from) {
        "m" -> value
        "cm" -> value / 100
        "mm" -> value / 1000
        "km" -> value * 1000
        "in" -> value * 0.0254
        "ft" -> value * 0.3048
        "yd" -> value * 0.9144
        "mi" -> value * 1609.34
        else -> value
    }
    return when (to) {
        "m" -> meters
        "cm" -> meters * 100
        "mm" -> meters * 1000
        "km" -> meters / 1000
        "in" -> meters / 0.0254
        "ft" -> meters / 0.3048
        "yd" -> meters / 0.9144
        "mi" -> meters / 1609.34
        else -> meters
    }
}

fun convertWeight(value: Double, from: String, to: String): Double {
    val kg = when (from) {
        "kg" -> value
        "g" -> value / 1000
        "mg" -> value / 1_000_000
        "t" -> value * 1000
        "lb" -> value * 0.453592
        "oz" -> value * 0.0283495
        else -> value
    }
    return when (to) {
        "kg" -> kg
        "g" -> kg * 1000
        "mg" -> kg * 1_000_000
        "t" -> kg / 1000
        "lb" -> kg / 0.453592
        "oz" -> kg / 0.0283495
        else -> kg
    }
}

fun convertTemperature(value: Double, from: String, to: String): Double {
    return when (from to to) {
        "°C" to "°C" -> value
        "°C" to "°F" -> value * 9 / 5 + 32
        "°C" to "K"  -> value + 273.15
        "°F" to "°C" -> (value - 32) * 5 / 9
        "°F" to "°F" -> value
        "°F" to "K"  -> (value - 32) * 5 / 9 + 273.15
        "K"  to "°C" -> value - 273.15
        "K"  to "°F" -> (value - 273.15) * 9 / 5 + 32
        "K"  to "K"  -> value
        else -> value
    }
}

fun convertTime(value: Double, from: String, to: String): Double {
    val seconds = when (from) {
        "s" -> value
        "min" -> value * 60
        "h" -> value * 3600
        "d" -> value * 86400
        else -> value
    }
    return when (to) {
        "s" -> seconds
        "min" -> seconds / 60
        "h" -> seconds / 3600
        "d" -> seconds / 86400
        else -> seconds
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversorUnidadesScreen(onBack: () -> Unit) {
    var showInfo by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf(UnitType.Length) }
    var input by remember { mutableStateOf("") }
    var fromUnit by remember { mutableStateOf(lengthUnits[0]) }
    var toUnit by remember { mutableStateOf(lengthUnits[1]) }
    var result by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val units = when (type) {
        UnitType.Length -> lengthUnits
        UnitType.Weight -> weightUnits
        UnitType.Temperature -> temperatureUnits
        UnitType.Time -> timeUnits
    }

    fun calculate() {
        val value = input.replace(",", ".").toDoubleOrNull()
        if (value == null) {
            result = null
            return
        }
        result = when (type) {
            UnitType.Length -> "%.5f".format(convertLength(value, fromUnit.symbol, toUnit.symbol))
            UnitType.Weight -> "%.5f".format(convertWeight(value, fromUnit.symbol, toUnit.symbol))
            UnitType.Temperature -> "%.2f".format(convertTemperature(value, fromUnit.symbol, toUnit.symbol))
            UnitType.Time -> "%.5f".format(convertTime(value, fromUnit.symbol, toUnit.symbol))
        }
    }

    // Actualiza las unidades cuando cambia el tipo
    LaunchedEffect(type) {
        fromUnit = units[0]
        toUnit = units[1]
        input = ""
        result = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conversor de Unidades") },
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
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Botones de tipo en dos filas
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(UnitType.Length, UnitType.Weight).forEach { ut ->
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                type = ut
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == ut) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (type == ut) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .height(40.dp)
                                .weight(1f)
                        ) {
                            Text(ut.display, fontSize = 14.sp, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(UnitType.Temperature, UnitType.Time).forEach { ut ->
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                type = ut
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == ut) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (type == ut) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .height(40.dp)
                                .weight(1f)
                        ) {
                            Text(ut.display, fontSize = 14.sp, maxLines = 1)
                        }
                    }
                }
            }
            // Input y selección de unidades
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it.filter { c -> c.isDigit() || c == '.' || c == ',' }
                        calculate()
                    },
                    label = { Text("Valor") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    modifier = Modifier.width(110.dp)
                )
                Spacer(Modifier.width(12.dp))
                // Unidad origen
                ExposedDropdownMenuBox(
                    expanded = false,
                    onExpandedChange = {},
                ) {
                    var expandedFrom by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = {
                                expandedFrom = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                            modifier = Modifier.width(110.dp)
                        ) {
                            Text(fromUnit.symbol)
                        }
                        DropdownMenu(
                            expanded = expandedFrom,
                            onDismissRequest = { expandedFrom = false }
                        ) {
                            units.forEach {
                                DropdownMenuItem(
                                    text = { Text("${it.name} (${it.symbol})") },
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        fromUnit = it
                                        expandedFrom = false
                                        calculate()
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text("a", fontSize = 24.sp)
                Spacer(Modifier.width(8.dp))
                // Unidad destino
                ExposedDropdownMenuBox(
                    expanded = false,
                    onExpandedChange = {haptic.performHapticFeedback(HapticFeedbackType.LongPress)},
                ) {
                    var expandedTo by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { expandedTo = true},
                            modifier = Modifier.width(110.dp)
                        ) {
                            Text(toUnit.symbol)
                        }
                        DropdownMenu(
                            expanded = expandedTo,
                            onDismissRequest = { expandedTo = false }
                        ) {
                            units.forEach {
                                DropdownMenuItem(
                                    text = { Text("${it.name} (${it.symbol})") },
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        toUnit = it
                                        expandedTo = false
                                        calculate()
                                    }
                                )
                            }
                        }
                    }
                }
            }
            // Resultado, copiar y reset
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (!result.isNullOrBlank()) {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(result!!))
                            scope.launch { snackbarHostState.showSnackbar("Resultado copiado") }
                        }
                    },
                    enabled = !result.isNullOrBlank()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar resultado")
                    Spacer(Modifier.width(4.dp))
                    Text("Copiar")
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        input = ""
                        result = null
                        focusManager.clearFocus()
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Resetear")
                    Spacer(Modifier.width(4.dp))
                    Text("Resetear")
                }
            }
            result?.let {
                Text(
                    "Resultado: $it ${toUnit.symbol}",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showInfo = false
            },
            title = { Text("Acerca del conversor de unidades") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Convertí valores entre unidades de longitud, peso, temperatura y tiempo.")
                    Text("• Ingresá el valor, seleccioná las unidades de origen y destino, y el resultado se calcula automáticamente.")
                    Text("• Usá el botón 'Copiar' para guardar el resultado o 'Resetear' para limpiar los campos.")
                    Spacer(Modifier.height(8.dp))
                    Text("Ejemplos de unidades:")
                    Text("– Longitud: metro, pulgada, milla, etc.")
                    Text("– Peso: kilogramo, libra, onza, etc.")
                    Text("– Temperatura: Celsius, Fahrenheit, Kelvin.")
                    Text("– Tiempo: segundo, minuto, hora, día.")
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
