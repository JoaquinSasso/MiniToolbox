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
    val formatter = remember { NumberFormat.getInstance(Locale("es", "AR")) }
    var showInfo by remember { mutableStateOf(false) }

    var reunion by remember { mutableStateOf<Reunion?>(null) }
    var descripcion by remember { mutableStateOf("") }
    var aportes by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var consumidores by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(Unit) {
        val reuniones = ReunionesRepository.flujoReuniones(context).firstOrNull().orEmpty()
        val r = reuniones.find { it.id == reunionId }
        val gasto = r?.gastos?.find { it.id == gastoId }
        if (r != null && gasto != null) {
            reunion = r
            descripcion = gasto.descripcion
            aportes = gasto.aportesIndividuales.mapValues { it.value.toString() }
            consumidores = gasto.consumidoPor.mapValues { it.value.toString() }
        }
    }

    val montoTotal = aportes.values.sumOf { it.toDoubleOrNull() ?: 0.0 }

    Scaffold(
        topBar = {
            TopBarReusable(
                stringResource(R.string.edit_expenses_screen),
                onBack,
                { showInfo = true })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text(stringResource(R.string.expense_description_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    stringResource(R.string.total_amount_label, formatter.format(montoTotal)),
                    style = MaterialTheme.typography.titleMedium
                )
            }

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
                            onValueChange = {
                                aportes = aportes.toMutableMap().apply {
                                    if (it.isNotBlank()) put(
                                        grupo.nombre,
                                        it
                                    ) else remove(grupo.nombre)
                                }
                            },
                            modifier = Modifier.width(100.dp),
                            placeholder = { Text("0.0") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.expense_who_consumed_label),
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
                            value = consumidores[grupo.nombre]?.toString() ?: "0",
                            onValueChange = {
                                consumidores = consumidores.toMutableMap().apply {
                                    put(grupo.nombre, it)
                                }
                            },
                            modifier = Modifier.width(100.dp),
                            placeholder = { Text("0") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val aporteValido = aportes.filterValues { it.toDoubleOrNull() != null }
                        val valoresAporte = aporteValido.mapValues { it.value.toDouble() }

                        val consumidoresValidos =
                            consumidores.mapValues { it.value.toIntOrNull() ?: 0 }

                        val nuevoGasto = Gasto(
                            id = gastoId,
                            descripcion = descripcion,
                            aportesIndividuales = valoresAporte,
                            consumidoPor = consumidoresValidos
                        )

                        val actualizada = reunion?.copy(
                            gastos = reunion!!.gastos.map {
                                if (it.id == gastoId) nuevoGasto else it
                            }
                        )

                        scope.launch {
                            if (actualizada != null) {
                                ReunionesRepository.actualizarReunion(context, actualizada)
                            }
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.save))
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
}
