package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AguaStatisticsScreen(
    onBack: () -> Unit,
    viewModel: AguaStatisticsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    var showInfo by rememberSaveable { mutableStateOf(false) }
    var fechaEditandoStr by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopBarReusable(stringResource(R.string.water_statistics_screen), onBack, { showInfo = true }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Indicador(
                    titulo = stringResource(R.string.water_stats_streak_current),
                    valor = pluralStringResource(R.plurals.water_stats_days, uiState.rachaActual, uiState.rachaActual),
                    modifier = Modifier.weight(1f)
                )
                Indicador(
                    titulo = stringResource(R.string.water_stats_streak_best),
                    valor = pluralStringResource(R.plurals.water_stats_days, uiState.mejorRacha, uiState.mejorRacha),
                    modifier = Modifier.weight(1f)
                )
                Indicador(
                    titulo = stringResource(R.string.water_stats_average),
                    valor = uiState.promedioSemana
                        ?.let { stringResource(R.string.water_liters_value, AguaFormat.litros(it)) }
                        ?: stringResource(R.string.water_stats_no_value),
                    modifier = Modifier.weight(1f)
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.water_statistics_ultimos7), style = MaterialTheme.typography.titleMedium)
                    GraficoSemana(
                        semana = uiState.semana,
                        hoy = uiState.hoy,
                        objetivoML = uiState.objetivoML,
                        mlPorVaso = uiState.mlPorVaso
                    )
                    Text(
                        stringResource(R.string.water_stats_goal_line, AguaFormat.litros(uiState.objetivoML)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column {
                Text(stringResource(R.string.water_statistics_historial), style = MaterialTheme.typography.titleMedium)
                if (uiState.historico.isEmpty()) {
                    Text(
                        stringResource(R.string.water_statistics_historial_vacio),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    Text(
                        stringResource(R.string.water_statistics_historial_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (uiState.historico.isNotEmpty()) {
                Column {
                    uiState.historico.sortedByDescending { it.first }.forEach { (fecha, ml) ->
                        key("historial-$fecha") {
                            FilaHistorial(
                                fecha = fecha,
                                ml = ml,
                                cumplido = AguaMath.objetivoAlcanzado(ml, uiState.objetivoML, uiState.mlPorVaso),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    fechaEditandoStr = fecha.toString()
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
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
                TextButton(onClick = { showInfo = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }

    val fechaEditando = fechaEditandoStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    if (fechaEditando != null) {
        DialogoEditarDia(
            fecha = fechaEditando,
            mlInicial = uiState.historico.firstOrNull { it.first == fechaEditando }?.second ?: 0,
            objetivoML = uiState.objetivoML,
            mlPorVaso = uiState.mlPorVaso,
            onDismiss = { fechaEditandoStr = null },
            onGuardar = { nuevo ->
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                viewModel.editarConsumoFecha(fechaEditando, nuevo)
                fechaEditandoStr = null
            }
        )
    }
}

@Composable
private fun Indicador(titulo: String, valor: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(valor, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private fun capitalizar(texto: String, locale: Locale): String =
    texto.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

@Composable
private fun FilaHistorial(fecha: LocalDate, ml: Int, cumplido: Boolean, onClick: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(capitalizar(fecha.dayOfWeek.getDisplayName(TextStyle.FULL, locale), locale))
        },
        supportingContent = {
            Text(fecha.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)))
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.water_liters_value, AguaFormat.litros(ml)),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (cumplido) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = stringResource(R.string.water_goal_reached),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(20.dp)
                    )
                } else {
                    // Mismo ancho que el ícono para que los litros queden alineados
                    Spacer(Modifier.padding(start = 8.dp).size(20.dp))
                }
            }
        }
    )
}

@Composable
private fun DialogoEditarDia(
    fecha: LocalDate,
    mlInicial: Int,
    objetivoML: Int,
    mlPorVaso: Int,
    onDismiss: () -> Unit,
    onGuardar: (Int) -> Unit
) {
    var texto by rememberSaveable(fecha.toString()) { mutableStateOf(mlInicial.toString()) }
    val actual = texto.toIntOrNull() ?: 0
    val vasosObjetivo = AguaMath.calcularVasosObjetivo(objetivoML, mlPorVaso)
    val locale = LocalConfiguration.current.locales[0]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "${stringResource(R.string.water_edit_dialog_title)} · " +
                    fecha.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "${stringResource(R.string.water_liters_value, AguaFormat.litros(actual))} " +
                        stringResource(R.string.water_of_goal, AguaFormat.litros(objetivoML)),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    pluralStringResource(
                        R.plurals.water_glasses_progress,
                        vasosObjetivo,
                        AguaMath.calcularVasosConsumidos(actual, mlPorVaso),
                        vasosObjetivo
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { if (objetivoML > 0) (actual / objetivoML.toFloat()).coerceIn(0f, 1f) else 1f },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = { texto = (actual - mlPorVaso).coerceAtLeast(0).toString() },
                        enabled = actual > 0
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.water_remove_desc, mlPorVaso))
                    }
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it.filter(Char::isDigit).take(5) },
                        label = { Text(stringResource(R.string.water_edit_amount_label)) },
                        suffix = { Text("ml") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalIconButton(onClick = { texto = (actual + mlPorVaso).toString() }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.water_add, mlPorVaso))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onGuardar(actual) }) { Text(stringResource(R.string.water_edit_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
