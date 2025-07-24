package com.joasasso.minitoolbox.tools.calculadoras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

enum class UnitType(val stringResId: Int) {
    Length(R.string.unit_type_length),
    Weight(R.string.unit_type_weight),
    Temperature(R.string.unit_type_temperature),
    Time(R.string.unit_type_time)
}


data class SimpleUnit(val stringResId: Int, val symbol: String)


val lengthUnits = listOf(
    SimpleUnit(R.string.unit_meter, "m"),
    SimpleUnit(R.string.unit_centimeter, "cm"),
    SimpleUnit(R.string.unit_millimeter, "mm"),
    SimpleUnit(R.string.unit_kilometer, "km"),
    SimpleUnit(R.string.unit_inch, "in"),
    SimpleUnit(R.string.unit_foot, "ft"),
    SimpleUnit(R.string.unit_yard, "yd"),
    SimpleUnit(R.string.unit_mile, "mi")
)

val weightUnits = listOf(
    SimpleUnit(R.string.unit_kilogram, "kg"),
    SimpleUnit(R.string.unit_gram, "g"),
    SimpleUnit(R.string.unit_milligram, "mg"),
    SimpleUnit(R.string.unit_ton, "t"),
    SimpleUnit(R.string.unit_pound, "lb"),
    SimpleUnit(R.string.unit_ounce, "oz")
)

val temperatureUnits = listOf(
    SimpleUnit(R.string.unit_celsius, "°C"),
    SimpleUnit(R.string.unit_fahrenheit, "°F"),
    SimpleUnit(R.string.unit_kelvin, "K")
)

val timeUnits = listOf(
    SimpleUnit(R.string.unit_second, "s"),
    SimpleUnit(R.string.unit_minute, "min"),
    SimpleUnit(R.string.unit_hour, "h"),
    SimpleUnit(R.string.unit_day, "d")
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

    LaunchedEffect(type) {
        fromUnit = units[0]
        toUnit = units[1]
        input = ""
        result = null
    }

    Scaffold(
        topBar = { TopBarReusable(stringResource(R.string.tool_unit_converter), onBack, { showInfo = true }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Botones de tipo de unidad
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
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
                            Text(stringResource(ut.stringResId), fontSize = 14.sp, maxLines = 1)
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
                            Text(stringResource(ut.stringResId), fontSize = 14.sp, maxLines = 1)
                        }
                    }
                }
            }

            // Entrada y selección de unidades
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it.filter { c -> c.isDigit() || c == '.' || c == ',' }
                        calculate()
                    },
                    label = { Text(stringResource(R.string.unidad_valor)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.width(110.dp)
                )
                Spacer(Modifier.width(12.dp))

                // Unidad origen
                var expandedFrom by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = {
                            expandedFrom = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        modifier = Modifier.width(110.dp)
                    ) {
                        Text(fromUnit.symbol)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = expandedFrom,
                        onDismissRequest = { expandedFrom = false }
                    ) {
                        units.forEach {
                            DropdownMenuItem(
                                text = { Text("${stringResource(it.stringResId)} (${it.symbol})") },
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

                Spacer(Modifier.width(8.dp))
                Text("→", fontSize = 24.sp)
                Spacer(Modifier.width(8.dp))

                // Unidad destino
                var expandedTo by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = {
                            expandedTo = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        modifier = Modifier.width(110.dp)
                    ) {
                        Text(toUnit.symbol)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = expandedTo,
                        onDismissRequest = { expandedTo = false }
                    ) {
                        units.forEach {
                            DropdownMenuItem(
                                text = { Text("${stringResource(it.stringResId)} (${it.symbol})") },
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

            // Resultado
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (!result.isNullOrBlank()) {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(result!!))
                        }
                    },
                    enabled = !result.isNullOrBlank()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.copy))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.copy))
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
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.reset))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.reset))
                }
            }

            result?.let {
                Text(
                    stringResource(R.string.unidad_resultado_final, it, toUnit.symbol),
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
            title = { Text(stringResource(R.string.unidad_info_titulo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.unidad_info_linea1))
                    Text(stringResource(R.string.unidad_info_linea2))
                    Text(stringResource(R.string.unidad_info_linea3))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.unidad_info_linea4))
                    Text(stringResource(R.string.unidad_info_linea5))
                    Text(stringResource(R.string.unidad_info_linea6))
                    Text(stringResource(R.string.unidad_info_linea7))
                    Text(stringResource(R.string.unidad_info_linea8))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showInfo = false
                }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

