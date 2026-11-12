package com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import android.text.format.DateFormat
import java.text.DecimalFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

/**
 * Configuración de Agua: todo lo que se toca una vez y no tiene por qué ocupar la pantalla
 * del día. Cada cambio se guarda en el momento; no hay botón "Guardar".
 *
 * [avisoPermiso] muestra el estado del permiso de notificaciones; lo arma la pantalla porque
 * necesita el launcher de permisos y el ciclo de vida para enterarse si el usuario lo cambió.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AguaAjustesSheet(
    uiState: AguaUiState,
    onDismiss: () -> Unit,
    onObjetivo: (Int) -> Unit,
    onPorVaso: (Int) -> Unit,
    onCantidadesRapidas: (List<Int>) -> Unit,
    onNotificaciones: (Boolean) -> Unit,
    onFrecuencia: (Int) -> Unit,
    onHorario: (inicioMin: Int, finMin: Int) -> Unit,
    onPausarAlCumplir: (Boolean) -> Unit,
    avisoPermiso: @Composable ColumnScope.() -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(stringResource(R.string.water_settings), style = MaterialTheme.typography.headlineSmall)
            SeccionMeta(uiState.objetivoML, onObjetivo)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SeccionVaso(uiState.mlPorVaso, uiState.objetivoML, onPorVaso)
            SeccionCantidadesRapidas(uiState.cantidadesRapidas, onCantidadesRapidas)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SeccionRecordatorios(
                activas = uiState.notificacionesActivas,
                frecuenciaMinutos = uiState.frecuenciaMinutos,
                onNotificaciones = onNotificaciones,
                onFrecuencia = onFrecuencia,
                horaInicioMin = uiState.horaInicioMin,
                horaFinMin = uiState.horaFinMin,
                onHorario = onHorario,
                pausarAlCumplir = uiState.pausarAlCumplir,
                onPausarAlCumplir = onPausarAlCumplir,
                avisoPermiso = avisoPermiso
            )
        }
    }
}

@Composable
private fun SeccionMeta(objetivoML: Int, onObjetivo: (Int) -> Unit) {
    val haptic = LocalHapticFeedback.current
    var meta by remember(objetivoML) {
        mutableFloatStateOf(objetivoML.coerceIn(AguaMath.MIN_OBJETIVO_ML, AguaMath.MAX_OBJETIVO_ML).toFloat())
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.water_settings_goal), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.water_liters_value, AguaFormat.litros(meta.roundToInt())),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = meta,
            onValueChange = { valor ->
                val paso = AguaMath.PASO_OBJETIVO_ML.toFloat()
                val redondeado = (valor / paso).roundToInt() * paso
                if (redondeado != meta) {
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    meta = redondeado
                }
            },
            onValueChangeFinished = { onObjetivo(meta.roundToInt()) },
            valueRange = AguaMath.MIN_OBJETIVO_ML.toFloat()..AguaMath.MAX_OBJETIVO_ML.toFloat(),
            steps = (AguaMath.MAX_OBJETIVO_ML - AguaMath.MIN_OBJETIVO_ML) / AguaMath.PASO_OBJETIVO_ML - 1
        )
        Text(
            stringResource(R.string.water_settings_goal_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SeccionVaso(mlPorVaso: Int, objetivoML: Int, onPorVaso: (Int) -> Unit) {
    var texto by rememberSaveable { mutableStateOf(mlPorVaso.toString()) }
    val valor = texto.toIntOrNull()
    val valido = valor != null && valor in AguaMath.MIN_ML_POR_VASO..AguaMath.MAX_ML_POR_VASO

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.water_settings_glass), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.water_settings_glass_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = texto,
            onValueChange = { entrada ->
                val digitos = entrada.filter(Char::isDigit).take(4)
                texto = digitos
                digitos.toIntOrNull()
                    ?.takeIf { it in AguaMath.MIN_ML_POR_VASO..AguaMath.MAX_ML_POR_VASO }
                    ?.let(onPorVaso)
            },
            label = { Text(stringResource(R.string.water_settings_glass_label)) },
            suffix = { Text("ml") },
            isError = !valido,
            supportingText = {
                if (valido) {
                    val vasos = AguaMath.calcularVasosObjetivo(objetivoML, valor)
                    Text(pluralStringResource(R.plurals.water_settings_glass_per_day, vasos, vasos))
                } else {
                    Text(
                        stringResource(
                            R.string.water_settings_glass_error,
                            AguaMath.MIN_ML_POR_VASO,
                            AguaMath.MAX_ML_POR_VASO
                        )
                    )
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeccionCantidadesRapidas(cantidades: List<Int>, onCantidadesRapidas: (List<Int>) -> Unit) {
    var mostrarDialogo by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.water_settings_quick), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.water_settings_quick_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            cantidades.forEach { ml ->
                key("rapida-$ml") {
                    InputChip(
                        selected = false,
                        onClick = { onCantidadesRapidas(cantidades - ml) },
                        label = { Text(stringResource(R.string.water_settings_glass_value, ml)) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.water_settings_quick_delete_desc, ml),
                                modifier = Modifier.size(InputChipDefaults.IconSize)
                            )
                        }
                    )
                }
            }
            if (cantidades.size < AguaMath.MAX_CANTIDADES_RAPIDAS) {
                AssistChip(
                    onClick = { mostrarDialogo = true },
                    label = { Text(stringResource(R.string.water_settings_quick_add)) },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) }
                )
            }
        }
    }

    if (mostrarDialogo) {
        var texto by rememberSaveable { mutableStateOf("") }
        val valor = texto.toIntOrNull()
        val valido = valor != null && valor in AguaMath.MIN_ML_POR_VASO..AguaMath.MAX_ML_POR_VASO
        AlertDialog(
            onDismissRequest = { mostrarDialogo = false },
            title = { Text(stringResource(R.string.water_settings_quick_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it.filter(Char::isDigit).take(4) },
                    label = { Text(stringResource(R.string.water_settings_glass_label)) },
                    suffix = { Text("ml") },
                    isError = texto.isNotEmpty() && !valido,
                    supportingText = {
                        Text(
                            stringResource(
                                R.string.water_settings_glass_error,
                                AguaMath.MIN_ML_POR_VASO,
                                AguaMath.MAX_ML_POR_VASO
                            )
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(
                    enabled = valido,
                    onClick = {
                        valor?.let { onCantidadesRapidas((cantidades + it).distinct().sorted()) }
                        mostrarDialogo = false
                    }
                ) { Text(stringResource(R.string.accept)) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SeccionRecordatorios(
    activas: Boolean,
    frecuenciaMinutos: Int,
    onNotificaciones: (Boolean) -> Unit,
    onFrecuencia: (Int) -> Unit,
    horaInicioMin: Int,
    horaFinMin: Int,
    onHorario: (Int, Int) -> Unit,
    pausarAlCumplir: Boolean,
    onPausarAlCumplir: (Boolean) -> Unit,
    avisoPermiso: @Composable ColumnScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    // null: ningún reloj abierto; true: editando el inicio; false: editando el fin
    var editandoInicio by rememberSaveable { mutableStateOf<Boolean?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.water_notif_title), style = MaterialTheme.typography.titleMedium)
        FilaSwitch(
            texto = stringResource(R.string.water_notif_enabled),
            activo = activas,
            onCambio = { nuevo ->
                haptic.performHapticFeedback(
                    if (nuevo) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
                )
                onNotificaciones(nuevo)
            }
        )
        avisoPermiso()
        if (activas) {
            Text(
                stringResource(R.string.water_notif_interval),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val seleccionada = AguaMath.frecuenciaMasCercana(frecuenciaMinutos)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AguaMath.FRECUENCIAS_MINUTOS.forEach { minutos ->
                    key("frecuencia-$minutos") {
                        FilterChip(
                            selected = minutos == seleccionada,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onFrecuencia(minutos)
                            },
                            label = { Text(etiquetaFrecuencia(minutos)) }
                        )
                    }
                }
            }

            Text(
                stringResource(R.string.water_notif_window),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                AssistChip(
                    onClick = { editandoInicio = true },
                    label = { Text(formatearHora(horaInicioMin)) },
                    leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) }
                )
                Text(
                    stringResource(R.string.water_notif_window_and),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                AssistChip(
                    onClick = { editandoInicio = false },
                    label = { Text(formatearHora(horaFinMin)) },
                    leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) }
                )
            }

            FilaSwitch(
                texto = stringResource(R.string.water_notif_pause_goal),
                detalle = stringResource(R.string.water_notif_pause_goal_hint),
                activo = pausarAlCumplir,
                onCambio = onPausarAlCumplir
            )
        }
    }

    editandoInicio?.let { inicio ->
        val actual = if (inicio) horaInicioMin else horaFinMin
        val context = LocalContext.current
        val estado = rememberTimePickerState(
            initialHour = actual / 60,
            initialMinute = actual % 60,
            is24Hour = DateFormat.is24HourFormat(context)
        )
        AlertDialog(
            onDismissRequest = { editandoInicio = null },
            title = {
                Text(
                    stringResource(
                        if (inicio) R.string.water_notif_window_start else R.string.water_notif_window_end
                    )
                )
            },
            text = { TimePicker(state = estado) },
            confirmButton = {
                TextButton(onClick = {
                    val elegido = estado.hour * 60 + estado.minute
                    if (inicio) onHorario(elegido, horaFinMin) else onHorario(horaInicioMin, elegido)
                    editandoInicio = null
                }) { Text(stringResource(R.string.accept)) }
            },
            dismissButton = {
                TextButton(onClick = { editandoInicio = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun FilaSwitch(
    texto: String,
    activo: Boolean,
    onCambio: (Boolean) -> Unit,
    detalle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = activo, role = Role.Switch, onValueChange = onCambio),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(texto, style = MaterialTheme.typography.bodyLarge)
            if (detalle != null) {
                Text(
                    detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = activo, onCheckedChange = null)
    }
}

/** Hora en el formato corto del idioma del sistema (14:30 o 2:30 p. m.). */
private fun formatearHora(minutoDelDia: Int): String =
    LocalTime.of(minutoDelDia / 60, minutoDelDia % 60)
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

@Composable
private fun etiquetaFrecuencia(minutos: Int): String =
    if (minutos < 60) {
        stringResource(R.string.water_interval_minutes, minutos)
    } else {
        stringResource(R.string.water_interval_hours, DecimalFormat("0.#").format(minutos / 60.0))
    }
