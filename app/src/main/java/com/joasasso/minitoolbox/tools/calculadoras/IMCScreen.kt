package com.joasasso.minitoolbox.tools.calculadoras

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.tools.data.BMIDataStore
import com.joasasso.minitoolbox.tools.generadores.SegmentedButton
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IMCScreen(onBack: () -> Unit) {
    var showInfo by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    //Entradas y salidas
    var pesoInput by remember { mutableStateOf("") }
    var alturaInput by remember { mutableStateOf("") }
    var alturaFeet by remember { mutableStateOf("") }
    var alturaInches by remember { mutableStateOf("") }
    var imcResult by remember { mutableStateOf<Float?>(null) }
    var imcCat by remember { mutableStateOf("") }
    var resultColor by remember { mutableStateOf(Color.Unspecified) }

    //Variables relacionadas a manejar el sistema de unidades con uso de dataStore
    var useImperial by remember { mutableStateOf(false) }
    val metric = stringResource(R.string.bmi_metric_system)
    val imperial = stringResource(R.string.bmi_imperial_system)
    var modo = if (useImperial) imperial else metric
    val opcionesModo = listOf(metric, imperial)

    LaunchedEffect(Unit) {
        BMIDataStore.getUseImperial(context).collect {
            useImperial = it
            modo = if (it) imperial else metric
        }
    }

    //Categorias de IMC
    val catBajo = stringResource(R.string.bmi_categoria_bajo)
    val catNormal = stringResource(R.string.bmi_categoria_normal)
    val catSobrepeso = stringResource(R.string.bmi_categoria_sobrepeso)
    val catObesidad1 = stringResource(R.string.bmi_categoria_obesidad1)
    val catObesidad2 = stringResource(R.string.bmi_categoria_obesidad2)
    val catObesidad3 = stringResource(R.string.bmi_categoria_obesidad3)

    var formattedText by remember { mutableStateOf("") }


    fun getIMCCategory(imc: Float): Pair<String, Color> {
        return when {
            imc < 18.5     -> catBajo to Color(0xFF039BE5)
            imc < 25.0     -> catNormal      to Color(0xFF43A047)
            imc < 30.0     -> catSobrepeso  to Color(0xFFFFB300)
            imc < 35.0     -> catObesidad1    to Color(0xFFFF7043)
            imc < 40.0     -> catObesidad2    to Color(0xFFD32F2F)
            else           -> catObesidad3    to Color(0xFFA22DD9)
        }
    }

    fun calcularIMC() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val peso = pesoInput.toFloatOrNull()
        val alturaM = if (useImperial) {
            val ft = alturaFeet.toFloatOrNull()
            val inch = alturaInches.toFloatOrNull()
            if (ft != null && inch != null) ((ft * 12f) + inch) * 0.0254f else null
        } else {
            val cm = alturaInput.toFloatOrNull()
            if (cm != null) cm / 100f else null
        }

        if (peso != null && alturaM != null && alturaM > 0f) {
            val pesoKg = if (useImperial) peso * 0.453592f else peso
            val imc = pesoKg / (alturaM * alturaM)
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
        alturaFeet = ""
        alturaInches = ""
        resultColor = Color.Unspecified
        focusManager.clearFocus()
    }

    Scaffold(
        topBar = { TopBarReusable(stringResource(R.string.tool_bmi_calculator), onBack, { showInfo = true }) }
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
                stringResource(R.string.bmi_titulo),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = pesoInput,
                    onValueChange = { pesoInput = it.filter { c -> c.isDigit() || c == '.' } },
                    label = {
                        Text(
                            if (useImperial) stringResource(R.string.bmi_peso_label_lb)
                            else stringResource(R.string.bmi_peso_label)
                        )
                    }
                    ,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.width(150.dp)
                )
                if (useImperial) {
                    Column {
                        OutlinedTextField(
                            value = alturaFeet,
                            onValueChange = { alturaFeet = it.filter { c -> c.isDigit() } },
                            label = { Text(stringResource(R.string.bmi_altura_label_ft)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(150.dp)
                        )
                        Spacer(Modifier.height(20.dp))
                        OutlinedTextField(
                            value = alturaInches,
                            onValueChange = { alturaInches = it.filter { c -> c.isDigit() } },
                            label = { Text(stringResource(R.string.bmi_altura_label_in)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            keyboardActions = KeyboardActions(onDone = { calcularIMC() }),
                            modifier = Modifier.width(150.dp)
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = alturaInput,
                        onValueChange = { alturaInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(stringResource(R.string.bmi_altura_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        keyboardActions = KeyboardActions(onDone = { calcularIMC() }),
                        modifier = Modifier.width(150.dp)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { resetear() },
                    enabled = imcResult != null
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.reset))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.reset))
                }
                Spacer(Modifier.width(16.dp))
                if (imcResult != null) {
                    formattedText = stringResource(R.string.bmi_result_copy_format, imcResult!!, imcCat)
                }
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (imcResult != null) {
                            clipboardManager.setText(AnnotatedString(formattedText))
                        }
                    },
                    enabled = imcResult != null
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.copy))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.copy))
                }
            }
            imcResult?.let {
                val resultMessage = stringResource(R.string.bmi_resultado, it)
                Text(resultMessage,
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
                onClick = { calcularIMC() },
                enabled = pesoInput.isNotBlank() && (
                        (!useImperial && alturaInput.isNotBlank()) ||
                                (useImperial && alturaFeet.isNotBlank() && alturaInches.isNotBlank())
                        ),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(stringResource(R.string.bmi_boton_calcular))
            }
            SegmentedButton(
                options = opcionesModo,
                selected = modo,
                onSelect = {
                    modo = it
                    useImperial = it == imperial
                    resetear()
                    scope.launch {
                        BMIDataStore.setUseImperial(context, useImperial)
                    }
                }
            )
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.bmi_info_titulo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.bmi_info_descripcion1))
                    Text(stringResource(R.string.bmi_info_formula))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.bmi_info_clasificacion))
                    Text(stringResource(R.string.bmi_info_clasificacion_1))
                    Text(stringResource(R.string.bmi_info_clasificacion_2))
                    Text(stringResource(R.string.bmi_info_clasificacion_3))
                    Text(stringResource(R.string.bmi_info_clasificacion_4))
                    Text(stringResource(R.string.bmi_info_clasificacion_5))
                    Text(stringResource(R.string.bmi_info_clasificacion_6))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.bmi_info_aclaracion))
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

