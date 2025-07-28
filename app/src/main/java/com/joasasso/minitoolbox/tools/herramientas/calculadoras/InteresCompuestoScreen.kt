package com.joasasso.minitoolbox.tools.herramientas.calculadoras

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.withSave
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteresCompuestoScreen(onBack: () -> Unit) {
    var inversionInicial by remember { mutableStateOf("") }
    var aporteMensual by remember { mutableStateOf("") }
    var cantidadAnios by remember { mutableStateOf("") }
    var tasa by remember { mutableStateOf("") }
    var margen by remember { mutableStateOf("") }
    var frecuencia by remember { mutableStateOf("Mensual") }
    var showInfo by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val frecuencias = listOf("Anual", "Semestral", "Trimestral", "Mensual", "Diaria")
    val haptic = LocalHapticFeedback.current

    val resultados = calcularEscenarios(
        inversionInicial.toDoubleOrNull() ?: 0.0,
        aporteMensual.toDoubleOrNull() ?: 0.0,
        cantidadAnios.toIntOrNull() ?: 1,
        tasa.toDoubleOrNull() ?: 0.0,
        margen.toDoubleOrNull() ?: 0.0,
        frecuencia
    )

    Scaffold(
        topBar = { TopBarReusable(stringResource(R.string.tool_compound_interest), onBack, { showInfo = true }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = inversionInicial,
                onValueChange = { inversionInicial = it },
                label = { Text(stringResource(R.string.interes_inicial)) },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = aporteMensual,
                onValueChange = { aporteMensual = it },
                label = { Text(stringResource(R.string.interes_aporte)) },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = cantidadAnios,
                onValueChange = { cantidadAnios = it },
                label = { Text(stringResource(R.string.interes_anios)) },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = tasa,
                onValueChange = { tasa = it },
                label = { Text(stringResource(R.string.interes_tasa)) },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = margen,
                onValueChange = { margen = it },
                label = { Text(stringResource(R.string.interes_margen)) },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                OutlinedTextField(
                    value = frecuencia,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.interes_frecuencia)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    frecuencias.forEach {
                        DropdownMenuItem(
                            text = { Text(it) },
                            onClick = {
                                frecuencia = it
                                expanded = false
                            }
                        )
                    }
                }
            }
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            Text(stringResource(R.string.interes_pesimista, formatoMiles(resultados.pesimista)), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.interes_promedio, formatoMiles(resultados.promedio)), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.interes_optimista, formatoMiles(resultados.optimista)), style = MaterialTheme.typography.bodyLarge)


            Spacer(modifier = Modifier.height(16.dp))

            Text(stringResource(R.string.interes_grafica), style = MaterialTheme.typography.titleMedium)
            if (resultados.puntos.size >= 2 && resultados.puntos.any { it != 0.0 }) {
                LineChartInteresCompuesto(resultados.puntos, cantidadAnios.toIntOrNull() ?: 1)
            }

        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = {
                showInfo = false
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            title = { Text(stringResource(R.string.tool_compound_interest)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.interes_help_linea1))
                    Text(stringResource(R.string.interes_help_linea2))
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

@Composable
fun LineChartInteresCompuesto(valores: List<Double>, cantidadAnios: Int) {
    val verdeSuave = Color(0xFF81C784)
    val textoColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textoColorArgb = textoColor.toArgb()


    if (valores.size < 2 || valores.all { it == 0.0 }) {
        Box(Modifier.height(72.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.interes_sin_datos))
        }
        return
    }

    val maxY = valores.maxOrNull() ?: 1.0
    val puntos = valores.mapIndexed { index, valor -> index to valor }
    val anchoBarra = 24.dp

    Box(Modifier.border(BorderStroke(1.dp, Color.Gray), shape = RectangleShape))
    {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp), // Altura total del gráfico
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(puntos) { (index, valor) ->
                val frac = (valor / maxY).coerceIn(0.0, 1.0)

                Column(
                    modifier = Modifier
                        .height(280.dp)
                        .width(anchoBarra),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Barra
                    Box(
                        modifier = Modifier
                            .height(140.dp)
                            .width(anchoBarra),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Canvas(
                            Modifier
                                .fillMaxSize()
                        ) {
                            val height = size.height * frac.toFloat()

                            // Dibujo de la barra
                            drawRoundRect(
                                color = verdeSuave,
                                topLeft = Offset(0f, size.height - height),
                                size = Size(size.width, height),
                                cornerRadius = CornerRadius(6f, 6f)
                            )

                            // Dibujo del texto rotado, con el dinero en ese intervalo
                            drawContext.canvas.nativeCanvas.apply {
                                withSave {
                                    val textPaint = android.graphics.Paint().apply {
                                        color = textoColorArgb
                                        textSize = 30f
                                        isAntiAlias = true
                                        textAlign = android.graphics.Paint.Align.LEFT
                                    }
                                    // Texto formateado
                                    val texto = formatoMiles(valor)

                                    // Posicionar y rotar texto sobre la barra
                                    rotate(-90f, size.width / 2, size.height - height - 8f)
                                    drawText(
                                        texto,
                                        size.width / 2 + 4f,
                                        size.height - height - 8f,
                                        textPaint
                                    )
                                }
                            }
                        }
                    }

                    // Año debajo
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${(index + 1)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


data class ResultadoInteres(
    val pesimista: Double,
    val promedio: Double,
    val optimista: Double,
    val puntos: List<Double>
)

fun calcularEscenarios(
    inversionInicial: Double,
    aporteMensual: Double,
    cantidadAnios: Int,
    tasaAnual: Double,
    margen: Double,
    frecuencia: String
): ResultadoInteres {
    val n = when (frecuencia) {
        "Anual" -> 1
        "Semestral" -> 2
        "Trimestral" -> 4
        "Mensual" -> 12
        "Diaria" -> 365
        else -> 12
    }

    val totalPeriodos = cantidadAnios * n

    fun calcularMontos(tasa: Double): List<Double> {
        val r = tasa / 100 / n
        val montos = mutableListOf<Double>()
        var saldo = inversionInicial
        for (periodo in 1..totalPeriodos) {
            saldo *= (1 + r)
            saldo += (aporteMensual * 12 / n)
            if (periodo % (totalPeriodos / cantidadAnios) == 0 || periodo == totalPeriodos) {
                montos.add(saldo)
            }
        }
        return montos
    }

    val tasas = listOf(
        tasaAnual - margen, // pesimista
        tasaAnual,          // promedio
        tasaAnual + margen  // optimista
    )

    val montos = tasas.map { calcularMontos(it) }

    return ResultadoInteres(
        pesimista = montos[0].lastOrNull() ?: 0.0,
        promedio = montos[1].lastOrNull() ?: 0.0,
        optimista = montos[2].lastOrNull() ?: 0.0,
        puntos = montos[1] // gráfico con escenario promedio
    )
}

fun formatoMiles(valor: Double): String {
    val formato = NumberFormat.getNumberInstance(Locale.GERMANY)
    formato.minimumFractionDigits = 2
    formato.maximumFractionDigits = 2
    return formato.format(valor)
}
