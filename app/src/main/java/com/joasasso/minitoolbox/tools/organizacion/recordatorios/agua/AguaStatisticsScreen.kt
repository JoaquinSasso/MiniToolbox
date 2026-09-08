package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.aguaDataStore
import com.joasasso.minitoolbox.data.flujoObjetivo
import com.joasasso.minitoolbox.data.flujoPorVaso
import com.joasasso.minitoolbox.data.guardarAguaFecha
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AguaStatisticsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var showInfo by remember { mutableStateOf(false) }
    var historico by remember { mutableStateOf<List<Pair<LocalDate, Int>>>(emptyList()) }
    var promedioSemana by remember { mutableIntStateOf(0) }
    var objetivoML by remember { mutableIntStateOf(2000) }
    var mlPorVaso by remember { mutableIntStateOf(250) }
    var itemParaEditar by remember { mutableStateOf<Pair<LocalDate, Int>?>(null) }

    val verdeSuave = Color(0xFF81C784)
    val rojoSuave = Color(0xFFE57373)
    // Cargar datos reactivos de DataStore
    LaunchedEffect(Unit) {
        context.aguaDataStore.data.collect { prefs ->
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val lista = prefs.asMap()
                .filter { (k, _) -> k.name.startsWith("agua_ml_") }
                .mapNotNull { (k, v) ->
                    val fechaStr = k.name.removePrefix("agua_ml_")
                    try {
                        val fecha = LocalDate.parse(fechaStr, formatter)
                        (v as? Int)?.let { fecha to it }
                    } catch (_: DateTimeParseException) { null }
                }
                .sortedBy { it.first }
            historico = lista

            // Promedio últimos 7 días (pueden no ser consecutivos)
            val ultimos7 = lista.takeLast(7)
            promedioSemana = if (ultimos7.isNotEmpty()) ultimos7.map { it.second }.average().roundToInt() else 0
        }
    }

    LaunchedEffect(Unit) {
        context.flujoObjetivo().collect { objetivo ->
            objetivoML = objetivo
        }
    }

    LaunchedEffect(Unit) {
        context.flujoPorVaso().collect { porVaso ->
            mlPorVaso = porVaso
        }
    }

    Scaffold(
        topBar = {TopBarReusable(stringResource(R.string.water_statistics_screen), onBack, {showInfo = true})},
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.water_statistics_ultimos7), fontSize = 17.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            // --- Barra semanal ---
            BarChartAguaSemana(
                historico.takeLast(7),
                objetivoML
            )
            Spacer(Modifier.height(16.dp))
            // --- Datos ---
            if (historico.isEmpty()) {
                Text(stringResource(R.string.water_statistics_sin_registros), color = MaterialTheme.colorScheme.error)
            } else {
                Text(
                    stringResource(R.string.water_statistics_promedio, promedioSemana / 1000f),
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(
                        R.string.water_statistics_mejor_dia,
                        historico.maxByOrNull { it.second }?.let {
                            "${it.first.dayOfMonth}/${it.first.monthValue} - ${(it.second / 1000f).let { f -> "%.2f".format(f) }}L"
                        }.orEmpty()
                    ),
                    fontSize = 15.sp
                )
            }
            Spacer(Modifier.height(18.dp))
            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.water_statistics_historial), fontSize = 17.sp, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(6.dp))
            if (historico.isEmpty()) {
                Text(stringResource(R.string.water_statistics_historial_vacio))
            } else {
                historico.sortedByDescending { it.first }.forEach { (fecha, ml) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                itemParaEditar = fecha to ml
                            }
                            .padding(vertical = 8.dp, horizontal = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "${fecha.dayOfMonth}/${fecha.monthValue}/${fecha.year}",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                stringResource(R.string.water_edit_hint),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "${(ml / 1000f).let { "%.2f".format(it) }}L",
                                fontSize = 18.sp,
                                color = if (ml >= objetivoML) verdeSuave else rojoSuave
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.water_edit_hint),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                }

            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.water_statistics_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.water_statistics_help_line1))
                    Text(stringResource(R.string.water_statistics_help_line2))
                    Text(stringResource(R.string.water_statistics_help_line3))
                    Text(stringResource(R.string.water_statistics_help_line4))
                    Text(stringResource(R.string.water_statistics_help_line5))
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

    itemParaEditar?.let { (fecha, mlActual) ->
        var tempMl by remember(itemParaEditar) { mutableStateOf(mlActual.toString()) }
        val currentMl = tempMl.toIntOrNull() ?: 0
        val cantLitros = currentMl / 1000f
        val objetivoLitros = objetivoML / 1000f
        val glassesConsumed = (currentMl / mlPorVaso.coerceAtLeast(1).toFloat()).toInt()
        val glassesGoal = (objetivoML / mlPorVaso.coerceAtLeast(1).toFloat()).toInt()
        val glassesText = stringResource(R.string.water_glasses_display, glassesConsumed, glassesGoal)

        AlertDialog(
            onDismissRequest = { itemParaEditar = null },
            title = {
                Text("${stringResource(R.string.water_edit_dialog_title)} (${fecha.dayOfMonth}/${fecha.monthValue}/${fecha.year})")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${"%.2f".format(cantLitros)} L / ${"%.2f".format(objetivoLitros)} L $glassesText",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    AguaLevelBar(currentMl, objetivoML)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val updated = (currentMl - mlPorVaso).coerceAtLeast(0)
                                tempMl = updated.toString()
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            enabled = currentMl > 0
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.water_loss),
                                contentDescription = stringResource(R.string.water_remove),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("-$mlPorVaso ml", fontSize = 14.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = {
                                val updated = currentMl + mlPorVaso
                                tempMl = updated.toString()
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.water_full),
                                contentDescription = stringResource(R.string.water_add),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("+$mlPorVaso ml", fontSize = 14.sp)
                        }
                    }

                    OutlinedTextField(
                        value = tempMl,
                        onValueChange = { tempMl = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(R.string.water_edit_amount_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val nuevoValor = tempMl.toIntOrNull()?.coerceAtLeast(0) ?: 0
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        context.guardarAguaFecha(fecha, nuevoValor)
                        if (fecha == LocalDate.now()) {
                            actualizarWidgetAgua(context)
                        }
                    }
                    itemParaEditar = null
                }) {
                    Text(stringResource(R.string.water_edit_save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    itemParaEditar = null
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

// --- Gráfico semanal básico ---
@Composable
fun BarChartAguaSemana(
    datos: List<Pair<LocalDate, Int>>,
    objetivoML: Int
) {
    val verdeSuave = Color(0xFF81C784)
    val rojoSuave = Color(0xFFE57373)
    if (datos.isEmpty()) {
        Box(Modifier.height(72.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.water_statistics_screen_no_data))
        }
        return
    }
    val maxY = objetivoML.coerceAtLeast(datos.maxOf { it.second })
    val barWidth = 38.dp

    Row(
        Modifier
            .fillMaxWidth()
            .height(120.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        for ((fecha, ml) in datos) {
            val frac = (ml / maxY.toFloat()).coerceIn(0f, 1f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .height(110.dp)
                    .width(barWidth)
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Canvas(
                        Modifier
                            .height(95.dp)
                            .width(barWidth)
                    ) {
                        val barHeight = size.height * frac
                        drawRoundRect(
                            color = if (ml >= objetivoML) verdeSuave else rojoSuave,
                            topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - barHeight),
                            size = androidx.compose.ui.geometry.Size(size.width, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                        )
                    }
                }
                Text(
                    text = "${fecha.dayOfMonth}/${fecha.monthValue}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
