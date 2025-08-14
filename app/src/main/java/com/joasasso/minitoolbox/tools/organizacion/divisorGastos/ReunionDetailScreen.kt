package com.joasasso.minitoolbox.tools.organizacion.divisorGastos

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.Grupo
import com.joasasso.minitoolbox.data.Reunion
import com.joasasso.minitoolbox.data.ReunionesRepository
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.DateFormat.getDateInstance
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

// … imports idénticos a los tuyos …

@Composable
fun DetallesReunionScreen(
    reunionId: String,
    onBack: () -> Unit,
    onEditarGasto: (String, String) -> Unit,
    onAgregarGasto: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showInfo by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    var reunion by remember { mutableStateOf<Reunion?>(null) }
    var grupoAEditar by remember { mutableStateOf<Grupo?>(null) }
    var nombreEditado by remember { mutableStateOf("") }
    var cantidadEditada by remember { mutableStateOf("") }
    var textoCompartir by remember { mutableStateOf("") }
    var deudas by remember { mutableStateOf(emptyList<String>()) }

    // NEW: confirmación de borrado de integrante
    var grupoAEliminar by remember { mutableStateOf<Grupo?>(null) }

    val locale = Locale.getDefault()
    val formatter = NumberFormat.getCurrencyInstance(locale).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }

    LaunchedEffect(Unit) {
        val reuniones = ReunionesRepository.flujoReuniones(context).firstOrNull().orEmpty()
        reuniones.find { it.id == reunionId.trim() }?.let {
            reunion = it
            textoCompartir = generarTextoCompartible(it, context)
            deudas = calcularDeudas(it, context)
        }
    }

    fun actualizarGrupo(original: Grupo, nuevo: Grupo) {
        reunion?.let { r ->
            val nuevosGrupos = r.integrantes.map { if (it == original) nuevo else it }
            val nuevosGastos = r.gastos.map { g ->
                val nuevosAportes = g.aportesIndividuales.mapKeys {
                    if (it.key == original.nombre) nuevo.nombre else it.key
                }
                val nuevosConsumidores = g.consumidoPor.mapKeys {
                    if (it.key == original.nombre) nuevo.nombre else it.key
                }
                g.copy(
                    aportesIndividuales = nuevosAportes,
                    consumidoPor = nuevosConsumidores
                )
            }
            val actualizada = r.copy(integrantes = nuevosGrupos, gastos = nuevosGastos)
            scope.launch {
                ReunionesRepository.actualizarReunion(context, actualizada)
                reunion = actualizada
                grupoAEditar = null
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
                    gasto.consumidoPor.entries.joinToString(", ") { (nombre, cant) -> "$nombre ($cant)" }
                } else {
                    stringResource(R.string.expense_no_consumers)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp)) {

                        // Fila superior: descripción, total, acciones
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
                                IconButton(onClick = { onEditarGasto(reunionId, gasto.id) }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.edit_expense_content_desc)
                                    )
                                }
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

                        // Resumen por gasto
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

            items(reunion?.integrantes ?: emptyList()) { grupo ->
                val totalPagado = reunion?.gastos?.sumOf { it.aportesIndividuales[grupo.nombre] ?: 0.0 } ?: 0.0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${grupo.nombre} (${grupo.cantidad})")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatter.format(totalPagado))
                            IconButton(onClick = {
                                grupoAEditar = grupo
                                nombreEditado = grupo.nombre
                                cantidadEditada = grupo.cantidad.toString()
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_member_content_desc))
                            }
                            IconButton(onClick = {
                                // En lugar de borrar directo, pedimos confirmación
                                grupoAEliminar = grupo
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
                            grupoAEditar = Grupo("", 1)
                            nombreEditado = ""
                            cantidadEditada = "1"
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
                        Text(stringResource(R.string.expenses_share_summary_button))
                    }
                }
            }
        }
    }

    // Diálogo de edición de grupo (igual que antes, solo recalcula deudas al final)
    if (grupoAEditar != null) {
        AlertDialog(
            onDismissRequest = { grupoAEditar = null },
            title = {
                Text(
                    if (grupoAEditar!!.nombre.isBlank())
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
                        label = { Text(stringResource(R.string.create_meeting_group_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cantidadEditada,
                        onValueChange = { cantidadEditada = it },
                        label = { Text(stringResource(R.string.expense_group_size)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    val nuevoNombre = nombreEditado.trim()
                    val nuevaCantidad = cantidadEditada.toIntOrNull()?.coerceAtLeast(1) ?: 1

                    if (nuevoNombre.isNotBlank()) {
                        val grupoAnterior = grupoAEditar!!
                        val grupoNuevo = Grupo(nuevoNombre, nuevaCantidad)

                        val integrantesActualizados = if (grupoAnterior.nombre.isBlank()) {
                            reunion!!.integrantes + grupoNuevo
                        } else {
                            reunion!!.integrantes.map {
                                if (it.nombre == grupoAnterior.nombre) grupoNuevo else it
                            }
                        }

                        val gastosActualizados = reunion!!.gastos.map { gasto ->
                            val consumidoPor = gasto.consumidoPor.toMutableMap()

                            if (grupoAnterior.nombre.isBlank()) {
                                // nuevo grupo: por defecto participa con su cantidad
                                consumidoPor[nuevoNombre] = nuevaCantidad
                            } else if (grupoAnterior.nombre in consumidoPor) {
                                val cantidadAnterior = consumidoPor.remove(grupoAnterior.nombre) ?: 0
                                consumidoPor[nuevoNombre] = cantidadAnterior.coerceAtMost(nuevaCantidad)
                            }

                            gasto.copy(consumidoPor = consumidoPor)
                        }

                        reunion = reunion!!.copy(
                            integrantes = integrantesActualizados,
                            gastos = gastosActualizados
                        )

                        scope.launch {
                            ReunionesRepository.actualizarReunion(context, reunion!!)
                        }

                        grupoAEditar = null
                        deudas = calcularDeudas(reunion!!, context)
                    } else {
                        grupoAEditar = null
                    }
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    grupoAEditar = null
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // NUEVO: confirmación de borrado de integrante
    if (grupoAEliminar != null) {
        val g = grupoAEliminar!!
        AlertDialog(
            onDismissRequest = { grupoAEliminar = null },
            title = { Text(stringResource(R.string.expense_delete_member_title)) },
            text = {
                Text(
                    // Aviso explícito del impacto:
                    stringResource(R.string.expense_delete_member_message, g.nombre)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    reunion?.let {
                        val actualizada = it.copy(
                            integrantes = it.integrantes - g,
                            gastos = it.gastos.map { gasto ->
                                gasto.copy(
                                    // Al borrar un integrante, se quitan sus aportes y consumos (manteniendo coherencia)
                                    aportesIndividuales = gasto.aportesIndividuales - g.nombre,
                                    consumidoPor = gasto.consumidoPor - g.nombre
                                )
                            }
                        )
                        scope.launch {
                            ReunionesRepository.actualizarReunion(context, actualizada)
                            reunion = actualizada
                            deudas = calcularDeudas(reunion!!, context)
                            grupoAEliminar = null
                        }
                    }
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { grupoAEliminar = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
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
    reunion.integrantes.forEach { grupo ->
        val pagado = reunion.gastos.sumOf { it.aportesIndividuales[grupo.nombre] ?: 0.0 }
        sb.appendLine("- ${grupo.nombre} (${grupo.cantidad} ${context.resources.getString(R.string.people_label)}) ${context.resources.getString(R.string.share_paid)} ${formatoMoneda.format(pagado)}")
    }

    sb.appendLine()
    sb.appendLine("💸 ${context.resources.getString(R.string.share_debts)}")
    calcularDeudas(reunion, context).forEach { sb.appendLine("- $it") }

    return sb.toString()
}

fun calcularDeudas(reunion: Reunion, context: Context): List<String> {
    // Inicializar deudas por grupo
    val deudaPorGrupo = reunion.integrantes.associate { it.nombre to 0.0 }.toMutableMap()

    // Calcular cuánto debe cada grupo según los gastos que consumió
    for (gasto in reunion.gastos) {
        val totalPersonas = gasto.consumidoPor.values.sum()
        if (totalPersonas == 0) continue

        val montoTotal = gasto.aportesIndividuales.values.sum()
        gasto.consumidoPor.forEach { (grupo, cantidad) ->
            val monto = montoTotal * cantidad / totalPersonas
            deudaPorGrupo[grupo] = deudaPorGrupo.getOrDefault(grupo, 0.0) + monto
        }
    }

    // Calcular cuánto pagó cada grupo
    val pagadoPorGrupo = reunion.integrantes.associate { grupo ->
        grupo.nombre to reunion.gastos.sumOf { it.aportesIndividuales[grupo.nombre] ?: 0.0 }
    }

    // Calcular balances
    val balance = reunion.integrantes.associate { grupo ->
        val pagado = pagadoPorGrupo[grupo.nombre] ?: 0.0
        val debe = deudaPorGrupo[grupo.nombre] ?: 0.0
        grupo.nombre to (pagado - debe)
    }

    // Separar acreedores y deudores
    val deudores = balance.filterValues { it < -0.01 }.toMutableMap()
    val acreedores = balance.filterValues { it > 0.01 }.toMutableMap()

    val resultados = mutableListOf<String>()

    // Formateador de moneda con puntos de miles y coma decimal
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