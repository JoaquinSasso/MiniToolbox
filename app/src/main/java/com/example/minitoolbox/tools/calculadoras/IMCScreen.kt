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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IMCScreen(onBack: () -> Unit) {
    var showInfo by remember { mutableStateOf(false) }
    var pesoInput by remember { mutableStateOf("") }
    var alturaInput by remember { mutableStateOf("") }
    var imcResult by remember { mutableStateOf<Float?>(null) }
    var imcCat by remember { mutableStateOf("") }
    var resultColor by remember { mutableStateOf(Color.Unspecified) }

    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    fun getIMCCategory(imc: Float): Pair<String, Color> {
        return when {
            imc < 18.5     -> "Bajo peso" to Color(0xFF039BE5)       // Celeste
            imc < 25.0     -> "Normal"     to Color(0xFF43A047)       // Verde
            imc < 30.0     -> "Sobrepeso"  to Color(0xFFFFB300)       // Amarillo
            imc < 35.0     -> "Obesidad I" to Color(0xFFFF7043)       // Naranja
            imc < 40.0     -> "Obesidad II" to Color(0xFFD32F2F)      // Rojo
            else           -> "Obesidad III" to Color(0xFFA22DD9)     // Violeta
        }
    }

    fun calcularIMC() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val peso = pesoInput.toFloatOrNull()
        val alturaCm = alturaInput.toFloatOrNull()
        if (peso != null && alturaCm != null && alturaCm > 0f) {
            val alturaM = alturaCm / 100f
            val imc = peso / (alturaM * alturaM)
            imcResult = imc
            val (cat, color) = getIMCCategory(imc)
            imcCat = cat
            resultColor = color
        } else {
            imcResult = null
            imcCat = ""
            resultColor = Color.Unspecified
        }
        focusManager.clearFocus()
    }

    fun resetear() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        pesoInput = ""
        alturaInput = ""
        imcResult = null
        imcCat = ""
        resultColor = Color.Unspecified
        focusManager.clearFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calculadora de IMC") },
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
            Text(
                "Calculá tu IMC",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = pesoInput,
                    onValueChange = { pesoInput = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Peso (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.width(120.dp)
                )
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = alturaInput,
                    onValueChange = { alturaInput = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Altura (cm)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { calcularIMC() }
                    ),
                    modifier = Modifier.width(120.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { calcularIMC() },
                    enabled = pesoInput.isNotBlank() && alturaInput.isNotBlank()
                ) {
                    Text("Calcular")
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (imcResult != null) {
                            clipboardManager.setText(
                                androidx.compose.ui.text.AnnotatedString(
                                    "IMC: %.2f ($imcCat)".format(imcResult)
                                )
                            )
                            scope.launch { snackbarHostState.showSnackbar("Resultado copiado") }
                        }
                    },
                    enabled = imcResult != null
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar resultado")
                    Spacer(Modifier.width(4.dp))
                    Text("Copiar")
                }
            }
            imcResult?.let {
                Text(
                    "IMC: %.2f".format(it),
                    fontSize = 22.sp,
                    color = resultColor,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    imcCat,
                    fontSize = 18.sp,
                    color = resultColor
                )
            }
            Spacer(Modifier.height(30.dp))
            Button(
                onClick = { resetear() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Resetear")
                Spacer(Modifier.width(8.dp))
                Text("Resetear")
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("¿Qué es el IMC?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("El IMC (Índice de Masa Corporal) es una fórmula que estima si tu peso es saludable según tu altura.")
                    Text("Fórmula: IMC = Peso (kg) / [Altura (m)]²")
                    Spacer(Modifier.height(8.dp))
                    Text("Clasificación según OMS:")
                    Text("• Menos de 18.5: Bajo peso")
                    Text("• 18.5 a 24.9: Normal")
                    Text("• 25 a 29.9: Sobrepeso")
                    Text("• 30 a 34.9: Obesidad I")
                    Text("• 35 a 39.9: Obesidad II")
                    Text("• 40 o más: Obesidad III")
                    Spacer(Modifier.height(8.dp))
                    Text("Esta calculadora es solo una referencia y no reemplaza el diagnóstico médico.")
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
