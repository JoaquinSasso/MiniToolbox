package com.joasasso.minitoolbox.tools.organizacion.divisorGastos

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.Reunion
import com.joasasso.minitoolbox.data.ReunionesRepository
import com.joasasso.minitoolbox.ui.components.ProToolPaywallDialog
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import com.joasasso.minitoolbox.utils.ads.RewardedManager
import com.joasasso.minitoolbox.utils.pro.CreditAccessManager
import com.joasasso.minitoolbox.utils.pro.LocalProState
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.DateFormat.getDateInstance
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

@Composable
fun DetallesReunionScreen(
    reunionId: String,
    onBack: () -> Unit,
    onEditarGasto: (String, String) -> Unit,
    onAgregarGasto: (String) -> Unit,
    onNavigateToPro: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    var showInfo by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    var reunion by remember { mutableStateOf<Reunion?>(null) }
    var integranteAEditar by remember { mutableStateOf<String?>(null) }
    var nombreEditado by remember { mutableStateOf("") }
    var textoCompartir by remember { mutableStateOf("") }
    var deudas by remember { mutableStateOf(emptyList<String>()) }

    var integranteAEliminar by remember { mutableStateOf<String?>(null) }

    val locale = Locale.getDefault()
    val formatter = NumberFormat.getCurrencyInstance(locale).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }

    val isPro = LocalProState.current.isPro
    var showPaywallDialog by remember { mutableStateOf(false) }
    var hasActivePass by remember { mutableStateOf(CreditAccessManager.hasActivePass(context)) }


    LaunchedEffect(Unit) {
        val reuniones = ReunionesRepository.flujoReuniones(context).firstOrNull().orEmpty()
        reuniones.find { it.id == reunionId.trim() }?.let {
            reunion = it
            textoCompartir = generarTextoCompartible(it, context)
            deudas = calcularDeudas(it, context)
        }
    }

    fun actualizarIntegrante(original: String, nuevo: String) {
        reunion?.let { r ->
            val nuevosIntegrantes = r.integrantes.map { if (it == original) nuevo else it }
            val nuevosGastos = r.gastos.map { g ->
                val nuevosAportes = g.aportesIndividuales.mapKeys {
                    if (it.key == original) nuevo else it.key
                }
                val nuevosConsumidores = g.consumidoPor.mapKeys {
                    if (it.key == original) nuevo else it.key
                }
                g.copy(
                    aportesIndividuales = nuevosAportes,
                    consumidoPor = nuevosConsumidores
                )
            }
            val actualizada = r.copy(integrantes = nuevosIntegrantes, gastos = nuevosGastos)
            scope.launch {
                ReunionesRepository.actualizarReunion(context, actualizada)
                reunion = actualizada
                integranteAEditar = null
                deudas = calcularDeudas(actualizada, context)
            }
        }
    }

    Scaffold(topBar = { TopBarReusable(stringResource(R.string.meeting_details_screen), onBack, { showInfo = true }) })
    { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                reunion?.let { r ->
                    val total = r.gastos.sumOf { it.aportesIndividuales.values.sum() }
                    Text(r.nombre, style = MaterialTheme.typography.titleLarge)
                    Text("${stringResource(R.string.share_date)} ${formatearFecha(r.fecha)}")
                    Text(
                        stringResource(R.string.total_amount_label, formatter.format(total)),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                }
            }

            item { Text(stringResource(R.string.expenses_section), style = MaterialTheme.typography.titleSmall) }

            items(reunion?.gastos ?: emptyList()) { gasto ->
                val totalGasto = gasto.aportesIndividuales.values.sum()
                val totalPersonas = gasto.consumidoPor.values.sum()
                val porPersona = if (totalPersonas > 0) totalGasto / totalPersonas else 0.0
                val resumenConsumidores = if (gasto.consumidoPor.isNotEmpty()) {
                    gasto.consumidoPor.entries.filter { it.value > 0 }.joinToString(", ") { it.key }
                } else {
                    stringResource(R.string.expense_no_consumers)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditarGasto(reunionId, gasto.id) },
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp)) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = gasto.descripcion,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(formatter.format(totalGasto))
                                IconButton(onClick = {
                                    reunion?.let {
                                        val nueva = it.copy(gastos = it.gastos.filterNot { g -> g.id == gasto.id })
                                        scope.launch {
                                            ReunionesRepository.actualizarReunion(context, nueva)
                                            reunion = nueva
                                            deudas = calcularDeudas(reunion!!, context)
                                        }
                                    }
                                }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete_expense_content_desc)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        val textoResumen = if (totalPersonas > 0) {
                            stringResource(
                                R.string.expense_consumers_with_price,
                                resumenConsumidores,
                                formatter.format(porPersona)
                            )
                        } else {
                            stringResource(R.string.expense_consumers_only, resumenConsumidores)
                        }

                        Text(
                            text = textoResumen,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAgregarGasto(reunionId) },
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.add_expense_button))
                    }
                }
            }

            item {
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.members_section), style = MaterialTheme.typography.titleSmall)
            }

            items(reunion?.integrantes ?: emptyList()) { integrante ->
                val totalPagado = reunion?.gastos?.sumOf { it.aportesIndividuales[integrante] ?: 0.0 } ?: 0.0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            integranteAEditar = integrante
                            nombreEditado = integrante
                        },
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(integrante)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatter.format(totalPagado))
                            IconButton(onClick = {
                                integranteAEliminar = integrante
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_member_content_desc))
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            integranteAEditar = ""
                            nombreEditado = ""
                        },
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.add_member_button))
                    }
                }
            }

            item {
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.debts_section), style = MaterialTheme.typography.titleSmall)
            }
            items(deudas) { deuda ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(deuda, modifier = Modifier.padding(16.dp))
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, textoCompartir)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(
                                    sendIntent,
                                    context.resources.getString(R.string.expenses_share_summary_button)
                                )
                                context.startActivity(shareIntent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Text(stringResource(R.string.expenses_share_summary_button), color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        }
    }

    if (integranteAEditar != null) {
        AlertDialog(
            onDismissRequest = { integranteAEditar = null },
            title = {
                Text(
                    if (integranteAEditar!!.isBlank())
                        stringResource(R.string.dialog_new_member_title)
                    else
                        stringResource(R.string.dialog_edit_member_title)
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombreEditado,
                        onValueChange = { nombreEditado = it },
                        label = { Text(stringResource(R.string.expenses_group_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    val nuevoNombre = nombreEditado.trim()
                    val integranteAnterior = integranteAEditar!!

                    if (nuevoNombre.isBlank() || reunion?.integrantes?.contains(nuevoNombre) == true) {
                        integranteAEditar = null
                        return@TextButton
                    }

                    if (integranteAnterior.isNotBlank()) {
                        actualizarIntegrante(integranteAnterior, nuevoNombre)
                        return@TextButton
                    } else {
                        val integrantesActualizados = reunion!!.integrantes + nuevoNombre

                        val gastosActualizados = reunion!!.gastos.map { gasto ->
                            val consumidoPor = gasto.consumidoPor.toMutableMap()
                            consumidoPor[nuevoNombre] = 1
                            gasto.copy(consumidoPor = consumidoPor)
                        }

                        reunion = reunion!!.copy(
                            integrantes = integrantesActualizados,
                            gastos = gastosActualizados
                        )

                        scope.launch {
                            ReunionesRepository.actualizarReunion(context, reunion!!)
                        }

                        integranteAEditar = null
                        deudas = calcularDeudas(reunion!!, context)
                    }
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    integranteAEditar = null
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (integranteAEliminar != null) {
        val i = integranteAEliminar!!
        AlertDialog(
            onDismissRequest = { integranteAEliminar = null },
            title = { Text(stringResource(R.string.expense_delete_member_title)) },
            text = {
                Text(
                    stringResource(R.string.expense_delete_member_message, i)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    reunion?.let {
                        val actualizada = it.copy(
                            integrantes = it.integrantes - i,
                            gastos = it.gastos.map { gasto ->
                                gasto.copy(
                                    aportesIndividuales = gasto.aportesIndividuales - i,
                                    consumidoPor = gasto.consumidoPor - i
                                )
                            }
                        )
                        scope.launch {
                            ReunionesRepository.actualizarReunion(context, actualizada)
                            reunion = actualizada
                            deudas = calcularDeudas(reunion!!, context)
                            integranteAEliminar = null
                        }
                    }
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { integranteAEliminar = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if(showPaywallDialog)
    {
        ProToolPaywallDialog(
            onDismiss = { showPaywallDialog = false },
            onGoToPro = { onNavigateToPro },
            onWatchAd = { showPaywallDialog = false
                if (activity != null) {
                    RewardedManager.show(
                        activity = activity,
                        onReward = {
                            CreditAccessManager.startTimedPassForAd(activity)
                            hasActivePass = true

                            Toast
                                .makeText(activity, R.string.pro_unlocked_toast, android.widget.Toast.LENGTH_SHORT)
                                .show()
                        },
                        onUnavailable = {
                            val used = CreditAccessManager.consumeGrace(activity)
                            if (used) {
                                hasActivePass = true
                                Toast
                                    .makeText(activity, R.string.free_pass_used_toast, android.widget.Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                Toast
                                    .makeText(activity, R.string.paywall_no_ad_try_later, android.widget.Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    )
                }
            })
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.dialog_meeting_info_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.info_meeting_line1))
                    Text(stringResource(R.string.info_meeting_line2))
                    Text(stringResource(R.string.info_meeting_line3))
                    Text(stringResource(R.string.info_meeting_line4))
                    Text(stringResource(R.string.info_meeting_line5))
                    Text(stringResource(R.string.info_meeting_line6))
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfo = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress) }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}


fun formatearFecha(millis: Long): String {
    val formato = getDateInstance()
    return formato.format(Date(millis))
}

fun generarTextoCompartible(reunion: Reunion, context: Context): String {
    val locale = Locale.getDefault()
    val formatoMoneda = NumberFormat.getCurrencyInstance(locale).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }
    val sb = StringBuilder()
    sb.appendLine("📋 ${context.resources.getString(R.string.share_meeting_title)} ${reunion.nombre}")
    sb.appendLine("📅 ${context.resources.getString(R.string.share_date)} ${formatearFecha(reunion.fecha)}")
    sb.appendLine("💰 ${context.resources.getString(R.string.share_total)} ${formatoMoneda.format(reunion.gastos.sumOf { it.aportesIndividuales.values.sum() })}")
    sb.appendLine()

    sb.appendLine("🧾 ${context.resources.getString(R.string.share_expenses)}")
    reunion.gastos.forEach {
        val monto = it.aportesIndividuales.values.sum()
        sb.appendLine("- ${it.descripcion}: ${formatoMoneda.format(monto)}")
    }

    sb.appendLine()
    sb.appendLine("👥 ${context.resources.getString(R.string.share_members)}")
    reunion.integrantes.forEach { integrante ->
        val pagado = reunion.gastos.sumOf { it.aportesIndividuales[integrante] ?: 0.0 }
        sb.appendLine("- $integrante: ${context.resources.getString(R.string.share_paid)} ${formatoMoneda.format(pagado)}")
    }

    sb.appendLine()
    sb.appendLine("💸 ${context.resources.getString(R.string.share_debts)}")
    calcularDeudas(reunion, context).forEach { sb.appendLine("- $it") }

    return sb.toString()
}

fun calcularDeudas(reunion: Reunion, context: Context): List<String> {
    val deudaPorIntegrante = reunion.integrantes.associate { it to 0.0 }.toMutableMap()
    val nombresIntegrantes = reunion.integrantes.toSet()

    for (gasto in reunion.gastos) {
        val consumidoPor = gasto.consumidoPor.filter { it.key in nombresIntegrantes && it.value > 0 }
        val aportes = gasto.aportesIndividuales.filterKeys { it in nombresIntegrantes }
        val totalPersonas = consumidoPor.size
        if (totalPersonas == 0) continue

        val montoTotal = aportes.values.sum()
        val montoPorPersona = montoTotal / totalPersonas

        consumidoPor.forEach { (nombre, _) ->
            deudaPorIntegrante[nombre] = deudaPorIntegrante.getOrDefault(nombre, 0.0) + montoPorPersona
        }
    }

    val pagadoPorIntegrante = reunion.integrantes.associate { nombre ->
        nombre to reunion.gastos.sumOf { it.aportesIndividuales[nombre] ?: 0.0 }
    }

    val balance = reunion.integrantes.associate { nombre ->
        val pagado = pagadoPorIntegrante[nombre] ?: 0.0
        val debe = deudaPorIntegrante[nombre] ?: 0.0
        nombre to (pagado - debe)
    }

    val deudores = balance.filterValues { it < -0.01 }.toMutableMap()
    val acreedores = balance.filterValues { it > 0.01 }.toMutableMap()

    val resultados = mutableListOf<String>()

    val locale = Locale.getDefault()
    val formatoMoneda = NumberFormat.getCurrencyInstance(locale).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }

    for ((deudor, deuda) in deudores) {
        var pendiente = -deuda

        val pagos = mutableListOf<String>()
        val acreedoresKeys = acreedores.keys.toList()

        for (acreedor in acreedoresKeys) {
            val credito = acreedores[acreedor] ?: continue
            if (credito <= 0.01) continue

            val monto = minOf(pendiente, credito)
            pagos.add(
                context.getString(R.string.debt_line, deudor, formatoMoneda.format(monto), acreedor)
            )

            pendiente -= monto
            acreedores[acreedor] = credito - monto

            if (pendiente <= 0.01) break
        }

        resultados.addAll(pagos)
    }

    return resultados
}
