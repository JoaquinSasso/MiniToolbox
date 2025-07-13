package com.joasasso.minitoolbox.tools.calculadoras.divisorGastos

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.tools.data.Grupo
import com.joasasso.minitoolbox.tools.data.Reunion
import com.joasasso.minitoolbox.tools.data.ReunionesRepository
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

    var reunion by remember { mutableStateOf<Reunion?>(null) }
    var grupoAEditar by remember { mutableStateOf<Grupo?>(null) }
    var nombreEditado by remember { mutableStateOf("") }
    var cantidadEditada by remember { mutableStateOf("") }
    var textoCompartir by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val reuniones = ReunionesRepository.flujoReuniones(context).firstOrNull().orEmpty()
        reuniones.find { it.id == reunionId.trim() }?.let {
            reunion = it
            textoCompartir = generarTextoCompartible(it)
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

    Scaffold(topBar = { TopBarReusable(reunion?.nombre ?: "Reunión", onBack) }) { padding ->
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
                    Text("Fecha: ${formatearFecha(r.fecha)}")
                    Text("Total gastado: $${"%.2f".format(total)}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                }
            }

            item { Text("Gastos", style = MaterialTheme.typography.titleSmall) }

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
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            Text("$${"%.2f".format(gasto.aportesIndividuales.values.sum())}")
                            IconButton(onClick = { onEditarGasto(reunionId, gasto.id) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar gasto")
                            }
                            IconButton(onClick = {
                                reunion?.let {
                                    val nueva = it.copy(gastos = it.gastos.filterNot { g -> g.id == gasto.id })
                                    scope.launch {
                                        ReunionesRepository.actualizarReunion(context, nueva)
                                        reunion = nueva
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar gasto")
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
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("+ Agregar gasto")
                    }
                }
            }

            item {
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("Integrantes", style = MaterialTheme.typography.titleSmall)
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
                        Text("${grupo.nombre} (${grupo.cantidad} personas)")
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ){
                            Text("$${"%.2f".format(totalPagado)}")
                            IconButton(onClick = {
                                grupoAEditar = grupo
                                nombreEditado = grupo.nombre
                                cantidadEditada = grupo.cantidad.toString()
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar grupo")
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
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar grupo")
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
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("+ Agregar integrante")
                    }
                }
            }

            item {
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("Deudas", style = MaterialTheme.typography.titleSmall)
            }

            items(calcularDeudas(reunion ?: return@LazyColumn)) { deuda ->
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
                            scope.launch {
                                val sendIntent = android.content.Intent().apply {
                                    action = android.content.Intent.ACTION_SEND
                                    putExtra(android.content.Intent.EXTRA_TEXT, textoCompartir)
                                    type = "text/plain"
                                }
                                val shareIntent = android.content.Intent.createChooser(
                                    sendIntent,
                                    "Compartir resumen"
                                )
                                context.startActivity(shareIntent)
                            }
                        }
                    ) {
                        Text("Compartir")
                    }
                }
            }
        }
    }

    if (grupoAEditar != null) {
        AlertDialog(
            onDismissRequest = { grupoAEditar = null },
            title = { Text(if (grupoAEditar!!.nombre.isBlank()) "Nuevo integrante" else "Editar integrante") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombreEditado,
                        onValueChange = { nombreEditado = it },
                        label = { Text("Nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cantidadEditada,
                        onValueChange = { cantidadEditada = it },
                        label = { Text("Cantidad de personas") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val nombre = nombreEditado.trim()
                    val cantidad = cantidadEditada.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    if (nombre.isNotBlank()) {
                        val nuevo = Grupo(nombre, cantidad)
                        actualizarGrupo(grupoAEditar!!, nuevo)
                    } else grupoAEditar = null
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { grupoAEditar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}





fun formatearFecha(millis: Long): String {
    val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formato.format(Date(millis))
}

fun generarTextoCompartible(reunion: Reunion): String {
    val formatoMoneda = NumberFormat.getCurrencyInstance(Locale("es", "AR")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }
    val sb = StringBuilder()
    sb.appendLine("📋 Reunión: ${reunion.nombre}")
    sb.appendLine("📅 Fecha: ${formatearFecha(reunion.fecha)}")
    sb.appendLine("💰 Total: ${formatoMoneda.format(reunion.gastos.sumOf { it.aportesIndividuales.values.sum() })}")
    sb.appendLine()

    sb.appendLine("🧾 Gastos:")
    reunion.gastos.forEach {
        val monto = it.aportesIndividuales.values.sum()
        sb.appendLine("- ${it.descripcion}: ${formatoMoneda.format(monto)}")
    }

    sb.appendLine()
    sb.appendLine("👥 Integrantes:")
    reunion.integrantes.forEach { grupo ->
        val pagado = reunion.gastos.sumOf { it.aportesIndividuales[grupo.nombre] ?: 0.0 }
        sb.appendLine("- ${grupo.nombre} (${grupo.cantidad} personas) pagó ${formatoMoneda.format(pagado)}")
    }

    sb.appendLine()
    sb.appendLine("💸 Reparto:")
    calcularDeudas(reunion).forEach { sb.appendLine("- $it") }

    return sb.toString()
}

fun calcularDeudas(reunion: Reunion): List<String> {
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
    val formatoMoneda = NumberFormat.getCurrencyInstance(Locale("es", "AR")).apply {
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
            pagos.add("$deudor debe pagar ${formatoMoneda.format(monto)} a $acreedor")

            pendiente -= monto
            acreedores[acreedor] = credito - monto

            if (pendiente <= 0.01) break
        }

        resultados.addAll(pagos)
    }

    return resultados
}
