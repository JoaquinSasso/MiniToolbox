package com.joasasso.minitoolbox.tools.calculadoras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversorRomanosScreen(onBack: () -> Unit) {
    var showInfo by remember { mutableStateOf(false) }
    var arabigoInput by remember { mutableStateOf("") }
    var romanoInput by remember { mutableStateOf("") }

    var errorArabigo by remember { mutableStateOf<String?>(null) }
    var errorRomano by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var lastUserEdit by remember { mutableStateOf("none") } // "arabigo", "romano", "none"

    fun toRoman(number: Int): String {
        val romanNumerals = listOf(
            1000 to "M", 900 to "CM", 500 to "D", 400 to "CD",
            100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
            10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I"
        )
        var num = number
        val sb = StringBuilder()
        for ((value, symbol) in romanNumerals) {
            while (num >= value) {
                sb.append(symbol)
                num -= value
            }
        }
        return sb.toString()
    }

    fun fromRoman(roman: String): Int? {
        val numerals = mapOf(
            "M" to 1000, "CM" to 900, "D" to 500, "CD" to 400,
            "C" to 100, "XC" to 90, "L" to 50, "XL" to 40,
            "X" to 10, "IX" to 9, "V" to 5, "IV" to 4, "I" to 1
        )
        var i = 0
        var sum = 0
        val s = roman.uppercase()
        while (i < s.length) {
            if (i + 1 < s.length && numerals.containsKey(s.substring(i, i + 2))) {
                sum += numerals[s.substring(i, i + 2)]!!
                i += 2
            } else if (numerals.containsKey(s.substring(i, i + 1))) {
                sum += numerals[s.substring(i, i + 1)]!!
                i += 1
            } else {
                return null
            }
        }
        // Validar que la reconversión coincide (por ej: "IIV" es inválido)
        return if (toRoman(sum) == s) sum else null
    }

    fun onArabigoChanged(text: String) {
        arabigoInput = text.filter { it.isDigit() }
        errorArabigo = null
        lastUserEdit = "arabigo"
        if (arabigoInput.isBlank()) {
            romanoInput = ""
            errorArabigo = null
            errorRomano = null
            return
        }
        val num = arabigoInput.toIntOrNull()
        if (num == null || num < 1 || num > 3999) {
            errorArabigo = "Solo entre 1 y 3999"
            romanoInput = ""
        } else {
            val roman = toRoman(num)
            romanoInput = roman
            errorArabigo = null
            errorRomano = null
        }
    }

    fun onRomanoChanged(text: String) {
        val upper = text.uppercase().filter { it in "MDCLXVI" }
        romanoInput = upper
        errorRomano = null
        lastUserEdit = "romano"
        if (romanoInput.isBlank()) {
            arabigoInput = ""
            errorRomano = null
            errorArabigo = null
            return
        }
        val result = fromRoman(romanoInput)
        if (result == null || result < 1 || result > 3999) {
            errorRomano = "Romano inválido o fuera de rango (1-3999)"
            arabigoInput = ""
        } else {
            arabigoInput = result.toString()
            errorRomano = null
            errorArabigo = null
        }
    }

    fun reset() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        arabigoInput = ""
        romanoInput = ""
        errorArabigo = null
        errorRomano = null
        lastUserEdit = "none"
        focusManager.clearFocus()
    }

    Scaffold(
        topBar = {TopBarReusable(stringResource(R.string.tool_roman_converter), onBack, {showInfo = true})},
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
            Text(
                "Convertí entre números arábigos y romanos",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
            // Arábigo a romano
            OutlinedTextField(
                value = arabigoInput,
                onValueChange = { onArabigoChanged(it) },
                label = { Text("N° arábigo (1-3999)") },
                singleLine = true,
                isError = errorArabigo != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                supportingText = {
                    errorArabigo?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier.width(180.dp)
            )
            // Romano a arábigo
            OutlinedTextField(
                value = romanoInput,
                onValueChange = { onRomanoChanged(it) },
                label = { Text("N° romano") },
                singleLine = true,
                isError = errorRomano != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                supportingText = {
                    errorRomano?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier.width(180.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val toCopy = if (lastUserEdit == "arabigo" && romanoInput.isNotBlank())
                            romanoInput
                        else if (lastUserEdit == "romano" && arabigoInput.isNotBlank())
                            arabigoInput
                        else ""
                        if (toCopy.isNotBlank()) {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(toCopy))
                            scope.launch { snackbarHostState.showSnackbar("Copiado: $toCopy") }
                        }
                    },
                    enabled = (arabigoInput.isNotBlank() && errorArabigo == null && romanoInput.isNotBlank())
                            || (romanoInput.isNotBlank() && errorRomano == null && arabigoInput.isNotBlank())
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar resultado")
                    Spacer(Modifier.width(4.dp))
                    Text("Copiar")
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = { reset() }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Resetear")
                    Spacer(Modifier.width(4.dp))
                    Text("Resetear")
                }
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Números romanos y arábigos") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Convertí números entre el sistema arábigo (123) y romano (CXXIII).")

                    Text("• Podés escribir en ambos campos y la conversión es automática.")
                    Spacer(Modifier.height(4.dp))
                    Text("Valores básicos:")
                    Text("I=1  V=5  X=10  L=50  C=100  D=500  M=1000")
                    Spacer(Modifier.height(4.dp))
                    Text("Ejemplo: 2024 = MMXXIV")
                    Text("• Rango válido: 1 a 3999 (I a MMMCMXCIX).")
                    Text("• ¿Por qué ese límite? El sistema clásico de números romanos solo usa las letras I, V, X, L, C, D y M. El número más grande que se puede escribir así es 3999 (MMMCMXCIX). Para números mayores, los romanos usaban símbolos o rayas especiales que no son comunes ni estándar hoy en día.")
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
