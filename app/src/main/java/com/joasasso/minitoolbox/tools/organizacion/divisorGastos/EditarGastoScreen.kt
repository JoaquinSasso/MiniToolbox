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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import java.text.NumberFormat

@Composable
fun EditarGastoScreen(
    reunionId: String,
    gastoId: String,
    onBack: () -> Unit,
    vm: GastoFormViewModel = viewModel()
) {
    val haptic = LocalHapticFeedback.current

    val locale = LocalLocale.current.platformLocale
    val formatter = NumberFormat.getCurrencyInstance(locale).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }

    var showInfo by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    val msgNombreOblig = stringResource(R.string.expense_name_required)
    val msgSinAporte = stringResource(R.string.expense_amount_required)
    val msgSinConsumidores = stringResource(R.string.expense_consumers_required)

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is GastoFormEvent.SaveSuccess -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBack()
                }
                is GastoFormEvent.ShowError -> {
                    val msg = when (event.messageResId) {
                        R.string.expense_name_required -> msgNombreOblig
                        R.string.expense_amount_required -> msgSinAporte
                        R.string.expense_consumers_required -> msgSinConsumidores
                        else -> ""
                    }
                    snackbarHostState.showSnackbar(msg)
                }
            }
        }
    }

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
            item {
                OutlinedTextField(
                    value = uiState.descripcion,
                    onValueChange = vm::onDescripcionChange,
                    label = { Text(stringResource(R.string.expense_description_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                Text(
                    stringResource(R.string.total_amount_label, formatter.format(uiState.montoTotal)),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            item {
                Text(
                    stringResource(R.string.expense_who_paid_label),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            items(uiState.integrantes) { nombre ->
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
                        Text(nombre, modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = uiState.aportes[nombre] ?: "",
                            onValueChange = { nuevo ->
                                var filtrado = nuevo.replace(',', '.')
                                filtrado = filtrado.replace(Regex("[^0-9.]"), "")
                                if (filtrado.count { it == '.' } > 1) {
                                    val firstDot = filtrado.indexOf('.')
                                    filtrado = filtrado.substring(0, firstDot + 1) +
                                            filtrado.substring(firstDot + 1).replace(".", "")
                                }
                                if (filtrado.contains('.')) {
                                    val parts = filtrado.split('.')
                                    val enteros = parts[0]
                                    val decimales = parts.getOrNull(1)?.take(2) ?: ""
                                    filtrado = if (decimales.isEmpty()) "$enteros." else "$enteros.$decimales"
                                }

                                vm.onAporteChange(nombre, filtrado)
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

            item {
                Text(
                    stringResource(R.string.expense_who_consumed_label),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            items(uiState.integrantes) { nombre ->
                val actual = (uiState.consumidores[nombre] ?: 1) > 0
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
                        Text(nombre, modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = actual,
                                onCheckedChange = { isChecked ->
                                    vm.onConsumidorToggle(nombre, isChecked)
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            item {
                Button(
                    onClick = {
                        vm.guardarGasto()
                    },
                    enabled = !uiState.isSubmitting,
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
