package com.joasasso.minitoolbox.tools.organizacion.divisorGastos

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.joasasso.minitoolbox.data.Grupo
import com.joasasso.minitoolbox.data.Reunion
import com.joasasso.minitoolbox.data.ReunionesRepository
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    LaunchedEffect(Unit) {
        val reuniones = ReunionesRepository.flujoReuniones(context).firstOrNull().orEmpty()
        reuniones.find { it.id == reunionId.trim() }?.let {
            reunion = it
            textoCompartir = generarTextoCompartible(context, it, context.resources)
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
            }
        }
    }

    Scaffold(topBar = {
        TopBarReusable(
            stringResource(R.string.meeting_details_screen),
            onBack,
            { showInfo = true })
    })
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
                    Text("${stringResource(R.string.date_label)}: ${formatearFecha(r.fecha)}")
                    Text(
                        "${stringResource(R.string.total_amount_label)}: $${"%.2f".format(total)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                }
            }

            item {
                Text(
                    stringResource(R.string.expenses_section),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            items(reunion?.gastos ?: emptyList()) { gasto ->
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
                        Text(gasto.descripcion)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$${"%.2f".format(gasto.aportesIndividuales.values.sum())}")
                            IconButton(onClick = { onEditarGasto(reunionId, gasto.id) }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.edit_expense_content_desc)
                                )
                            }
                            IconButton(onClick = {
                                reunion?.let {
                                    val nueva =
                                        it.copy(gastos = it.gastos.filterNot { g -> g.id == gasto.id })
                                    scope.launch {
                                        ReunionesRepository.actualizarReunion(context, nueva)
                                        reunion = nueva
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
                Text(
                    stringResource(R.string.members_section),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            items(reunion?.integrantes ?: emptyList()) { grupo ->
                val totalPagado =
                    reunion?.gastos?.sumOf { it.aportesIndividuales[grupo.nombre] ?: 0.0 } ?: 0.0
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
                        Text(
                            stringResource(
                                R.string.name_label,
                                grupo.nombre,
                                grupo.cantidad
                            )
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$${"%.2f".format(totalPagado)}")
                            IconButton(onClick = {
                                grupoAEditar = grupo
                                nombreEditado = grupo.nombre
                                cantidadEditada = grupo.cantidad.toString()
                            }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.edit_member_content_desc)
                                )
                            }
                            IconButton(onClick = {
                                reunion?.let {
                                    val actualizada = it.copy(
                                        integrantes = it.integrantes - grupo,
                                        gastos = it.gastos.map { gasto ->
                                            gasto.copy(
                                                aportesIndividuales = gasto.aportesIndividuales - grupo.nombre,
                                                consumidoPor = gasto.consumidoPor - grupo.nombre
                                            )
                                        }
                                    )
                                    scope.launch {
                                        ReunionesRepository.actualizarReunion(context, actualizada)
                                        reunion = actualizada
                                    }
                                }
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete_member_content_desc)
                                )
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
                Text(
                    stringResource(R.string.debts_section),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            items(calcularDeudas(reunion ?: return@LazyColumn, context)) { deuda ->
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
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        reunion?.let { generarTextoCompartible(context, reunion!!, context.resources) }
                                    )
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(
                                    sendIntent,
                                    context.getString(R.string.share_summary_title)
                                )
                                context.startActivity(shareIntent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Text(stringResource(R.string.share))
                    }
                }
            }
        }
    }
}

    fun formatearFecha(millis: Long): String {
    val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formato.format(Date(millis))
}

fun generarTextoCompartible(
    context: Context,
    reunion: Reunion,
    resources: Resources
): String {
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    val formatoMoneda = NumberFormat.getCurrencyInstance(locale).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }

    val sb = StringBuilder()
    sb.appendLine("📋 ${resources.getString(R.string.share_meeting_title)}: ${reunion.nombre}")
    sb.appendLine("📅 ${resources.getString(R.string.share_date)}: ${formatearFecha(reunion.fecha)}")
    sb.appendLine("💰 ${resources.getString(R.string.share_total)}: ${formatoMoneda.format(reunion.gastos.sumOf { it.aportesIndividuales.values.sum() })}")
    sb.appendLine()
    sb.appendLine("🧾 ${resources.getString(R.string.share_expenses)}:")
    reunion.gastos.forEach {
        val monto = it.aportesIndividuales.values.sum()
        sb.appendLine("- ${it.descripcion}: ${formatoMoneda.format(monto)}")
    }

    sb.appendLine()
    sb.appendLine("👥 ${resources.getString(R.string.share_members)}:")
    reunion.integrantes.forEach { grupo ->
        val pagado = reunion.gastos.sumOf { it.aportesIndividuales[grupo.nombre] ?: 0.0 }
        sb.appendLine("- ${grupo.nombre} (${resources.getString(R.string.expense_group_size)} ${grupo.cantidad}) ${resources.getString(R.string.share_paid)} ${formatoMoneda.format(pagado)}")
    }

    sb.appendLine()
    sb.appendLine("💸 ${resources.getString(R.string.share_debts)}:")
    calcularDeudas( reunion, context).forEach { sb.appendLine("- $it") }

    return sb.toString()
}



fun calcularDeudas(reunion: Reunion, context: Context, locale: Locale = Locale.getDefault()): List<String> {
    val deudaPorGrupo = reunion.integrantes.associate { it.nombre to 0.0 }.toMutableMap()

    for (gasto in reunion.gastos) {
        val totalPersonas = gasto.consumidoPor.values.sum()
        if (totalPersonas == 0) continue

        val montoTotal = gasto.aportesIndividuales.values.sum()
        gasto.consumidoPor.forEach { (grupo, cantidad) ->
            val monto = montoTotal * cantidad / totalPersonas
            deudaPorGrupo[grupo] = deudaPorGrupo.getOrDefault(grupo, 0.0) + monto
        }
    }

    val pagadoPorGrupo = reunion.integrantes.associate { grupo ->
        grupo.nombre to reunion.gastos.sumOf { it.aportesIndividuales[grupo.nombre] ?: 0.0 }
    }

    val balance = reunion.integrantes.associate { grupo ->
        val pagado = pagadoPorGrupo[grupo.nombre] ?: 0.0
        val debe = deudaPorGrupo[grupo.nombre] ?: 0.0
        grupo.nombre to (pagado - debe)
    }

    val deudores = balance.filterValues { it < -0.01 }.toMutableMap()
    val acreedores = balance.filterValues { it > 0.01 }.toMutableMap()

    val resultados = mutableListOf<String>()
    val formatoMoneda = NumberFormat.getCurrencyInstance(locale).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
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

