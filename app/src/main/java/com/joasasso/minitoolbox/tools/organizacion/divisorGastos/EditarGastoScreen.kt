package com.joasasso.minitoolbox.tools.organizacion.divisorGastos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.Gasto
import com.joasasso.minitoolbox.data.Reunion
import com.joasasso.minitoolbox.data.ReunionesRepository
import com.joasasso.minitoolbox.ui.components.Stepper
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun EditarGastoScreen(
    reunionId: String,
    gastoId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val locale = Locale.getDefault()
    val formatter = NumberFormat.getCurrencyInstance(locale).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }

    var showInfo by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val msgNombreOblig = stringResource(R.string.expense_name_required)
    val msgSinAporte = stringResource(R.string.expense_amount_required)
    val msgSinConsumidores = stringResource(R.string.expense_consumers_required)

    var reunion by remember { mutableStateOf<Reunion?>(null) }
    var descripcion by remember { mutableStateOf("") }

    // Durante edición, mantenemos Strings para inputs
    var aportes by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var consumidores by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    fun parseFlexibleDouble(text: String): Double? {
        if (text.isBlank()) return null
        val cleaned = text.trim()
            .replace(',', '.')
            .replace(Regex("[^0-9.]"), "")
        if (cleaned.count { it == '.' } > 1) return null
        return cleaned.toDoubleOrNull()
    }

    LaunchedEffect(Unit) {
        val reuniones = ReunionesRepository.flujoReuniones(context).firstOrNull().orEmpty()
        val r = reuniones.find { it.id == reunionId }
        val gasto = r?.gastos?.find { it.id == gastoId }
        if (r != null && gasto != null) {
            reunion = r
            descripcion = gasto.descripcion
            aportes = gasto.aportesIndividuales.mapValues { it.value.toString() }
            consumidores = if (gasto.consumidoPor.isEmpty()) {
                // default: todos consumen
                r.integrantes.associate { it.nombre to it.cantidad.toString() }
            } else {
                gasto.consumidoPor.mapValues { it.value.toString() }
            }
        }
    }

    val montoTotal = aportes.values.sumOf { parseFlexibleDouble(it) ?: 0.0 }

    Scaffold(
        topBar = {
            TopBarReusable(
                title = stringResource(R.string.edit_expenses_screen),
                onBack = onBack,
                onShowInfo = { showInfo = true }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Nombre del gasto (obligatorio)
            item {
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text(stringResource(R.string.expense_description_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                Text(
                    stringResource(R.string.total_amount_label, formatter.format(montoTotal)),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Quién pagó y cuánto (coma/punto aceptados)
            item {
                Text(
                    stringResource(R.string.expense_who_paid_label),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            items(reunion?.integrantes.orEmpty()) { grupo ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(grupo.nombre, modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = aportes[grupo.nombre] ?: "",
                            onValueChange = { nuevo ->
                                // Unificar coma a punto
                                var filtrado = nuevo.replace(',', '.')
                                // Permitir solo dígitos y punto
                                filtrado = filtrado.replace(Regex("[^0-9.]"), "")
                                // Si hay más de un punto, eliminar los extra
                                if (filtrado.count { it == '.' } > 1) {
                                    val firstDot = filtrado.indexOf('.')
                                    filtrado = filtrado.substring(0, firstDot + 1) +
                                            filtrado.substring(firstDot + 1).replace(".", "")
                                }
                                // Limitar a 2 decimales si hay punto
                                if (filtrado.contains('.')) {
                                    val parts = filtrado.split('.')
                                    val enteros = parts[0]
                                    val decimales = parts.getOrNull(1)?.take(2) ?: ""
                                    filtrado = if (decimales.isEmpty()) "$enteros." else "$enteros.$decimales"
                                }

                                aportes = aportes.toMutableMap().apply {
                                    if (filtrado.isNotBlank()) put(grupo.nombre, filtrado) else remove(grupo.nombre)
                                }
                            },
                            modifier = Modifier.width(120.dp),
                            placeholder = { Text("0") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                }
            }

            item { HorizontalDivider() }

            // Quiénes consumieron (Stepper 0..grupo.cantidad)
            item {
                Text(
                    stringResource(R.string.expense_who_consumed_label),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            items(reunion?.integrantes.orEmpty()) { grupo ->
                val actual = (consumidores[grupo.nombre]?.toIntOrNull() ?: grupo.cantidad)
                    .coerceIn(0, grupo.cantidad)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${grupo.nombre} (${grupo.cantidad})", modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Stepper(
                                value = actual,
                                onValueChange = { nuevo ->
                                    consumidores = consumidores.toMutableMap().apply {
                                        put(grupo.nombre, nuevo.coerceIn(0, grupo.cantidad).toString())
                                    }
                                },
                                range = 0..grupo.cantidad
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // Guardar
            item {
                Button(
                    onClick = {
                        scope.launch {
                            // Validaciones
                            if (descripcion.isBlank()) {
                                snackbarHostState.showSnackbar(msgNombreOblig)
                                return@launch
                            }

                            val aporteValido = aportes
                                .mapValues { parseFlexibleDouble(it.value) }
                                .filterValues { it != null }
                                .mapValues { it.value!! }

                            if (aporteValido.values.sum() <= 0.0) {
                                snackbarHostState.showSnackbar(msgSinAporte)
                                return@launch
                            }

                            val consumidoresValidos = consumidores
                                .mapValues { it.value.toIntOrNull() ?: 0 }
                                .filterValues { it > 0 }

                            if (consumidoresValidos.isEmpty()) {
                                snackbarHostState.showSnackbar(msgSinConsumidores)
                                return@launch
                            }

                            val nuevoGasto = Gasto(
                                id = gastoId,
                                descripcion = descripcion.trim(),
                                aportesIndividuales = aporteValido,
                                consumidoPor = consumidoresValidos
                            )

                            reunion?.let { r ->
                                val actualizada = r.copy(
                                    gastos = r.gastos.map { if (it.id == gastoId) nuevoGasto else it }
                                )
                                ReunionesRepository.actualizarReunion(context, actualizada)
                            }

                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.help_title_expense)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.help_expense_1))
                    Text(stringResource(R.string.help_expense_2))
                    Text(stringResource(R.string.help_expense_3))
                    Text(stringResource(R.string.help_expense_4))
                    Text(stringResource(R.string.help_expense_5))
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
