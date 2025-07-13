package com.joasasso.minitoolbox.tools.calculadoras.divisorGastos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.tools.data.Reunion
import com.joasasso.minitoolbox.tools.data.ReunionesRepository
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
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
    var textoCompartir by remember { mutableStateOf("") }
    var nombreAEditar by remember { mutableStateOf<String?>(null) }
    var nuevoNombre by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val reuniones = ReunionesRepository.flujoReuniones(context).firstOrNull().orEmpty()
        reuniones.find { it.id == reunionId.trim() }?.let {
            reunion = it
            textoCompartir = generarTextoCompartible(it)
        }
    }

    fun actualizarNombre(nombreViejo: String, nuevo: String) {
        reunion?.let { r ->
            val nuevosIntegrantes = r.integrantes.map { if (it == nombreViejo) nuevo else it }
            val nuevosGastos = r.gastos.map { gasto ->
                gasto.copy(
                    pagadoPor = gasto.pagadoPor.map { if (it == nombreViejo) nuevo else it },
                    consumidoPor = gasto.consumidoPor.map { if (it == nombreViejo) nuevo else it }
                )
            }
            val nuevaReunion = r.copy(integrantes = nuevosIntegrantes, gastos = nuevosGastos)
            scope.launch {
                ReunionesRepository.actualizarReunion(context, nuevaReunion)
                reunion = nuevaReunion
                nombreAEditar = null
            }
        }
    }

    Scaffold(
        topBar = { TopBarReusable(reunion?.nombre ?: "Reunión", onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                reunion?.let { r ->
                    Text("Fecha: ${formatearFecha(r.fecha)}")
                    val total = r.gastos.sumOf { it.monto }
                    Text("Total gastado: $${"%.2f".format(total)}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                }
            }

            item { Text("Gastos", style = MaterialTheme.typography.titleSmall) }

            items(reunion?.gastos ?: emptyList()) { gasto ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditarGasto(reunionId, gasto.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(gasto.descripcion)
                        Row {
                            Text("$${"%.2f".format(gasto.monto)}")
                            IconButton(onClick = {
                                reunion?.let {
                                    val nuevos = it.gastos.filter { g -> g.id != gasto.id }
                                    val actualizada = it.copy(gastos = nuevos)
                                    scope.launch {
                                        ReunionesRepository.actualizarReunion(context, actualizada)
                                        reunion = actualizada
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Box(
                        Modifier.padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+ Agregar gasto", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("Integrantes", style = MaterialTheme.typography.titleSmall)
            }

            items(reunion?.integrantes ?: emptyList()) { nombre ->
                val pagado = reunion?.gastos
                    ?.sumOf { it.aportesIndividuales[nombre] ?: 0.0 }


                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(nombre)
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$${"%.2f".format(pagado)}")
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = {
                                nombreAEditar = nombre
                                nuevoNombre = nombre
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar nombre")
                            }
                            IconButton(onClick = {
                                reunion?.let {
                                    val nuevaLista = it.integrantes - nombre
                                    val nuevosGastos = it.gastos.map { g ->
                                        g.copy(
                                            pagadoPor = g.pagadoPor - nombre,
                                            consumidoPor = g.consumidoPor - nombre
                                        )
                                    }
                                    val actualizada = it.copy(integrantes = nuevaLista, gastos = nuevosGastos)
                                    scope.launch {
                                        ReunionesRepository.actualizarReunion(context, actualizada)
                                        reunion = actualizada
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
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
                            nombreAEditar = ""
                            nuevoNombre = ""
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Box(
                        Modifier.padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+ Agregar integrante", style = MaterialTheme.typography.bodyMedium)
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(2.dp)
                ){
                    Text(deuda, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp))
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

    // Dialogo de edición de nombre
    if (nombreAEditar != null) {
        AlertDialog(
            onDismissRequest = { nombreAEditar = null },
            title = { Text(if (nombreAEditar!!.isBlank()) "Nuevo integrante" else "Editar nombre") },
            text = {
                OutlinedTextField(
                    value = nuevoNombre,
                    onValueChange = { nuevoNombre = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val nuevo = nuevoNombre.trim()
                    if (nuevo.isNotBlank()) {
                        if (nombreAEditar!!.isBlank()) {
                            // Agregar nuevo
                            reunion?.let {
                                val actualizada = it.copy(integrantes = it.integrantes + nuevo)
                                scope.launch {
                                    ReunionesRepository.actualizarReunion(context, actualizada)
                                    reunion = actualizada
                                }
                            }
                        } else {
                            actualizarNombre(nombreAEditar!!, nuevo)
                        }
                    }
                    nombreAEditar = null
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { nombreAEditar = null }) {
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
    val sb = StringBuilder()
    sb.appendLine("📋 Reunión: ${reunion.nombre}")
    sb.appendLine("📅 Fecha: ${formatearFecha(reunion.fecha)}")
    sb.appendLine("💰 Total: $${"%.2f".format(reunion.gastos.sumOf { it.monto })}")
    sb.appendLine()

    sb.appendLine("🧾 Gastos:")
    reunion.gastos.forEach {
        sb.appendLine("- ${it.descripcion}: $${"%.2f".format(it.monto)}")
    }

    sb.appendLine()
    sb.appendLine("👥 Integrantes:")
    reunion.integrantes.forEach { nombre ->
        val pagado = reunion.gastos
            .sumOf { it.aportesIndividuales[nombre] ?: 0.0 }
        sb.appendLine("- $nombre pagó $${"%.2f".format(pagado)}")
    }

    sb.appendLine()
    sb.appendLine("💸 Reparto:")
    calcularDeudas(reunion).forEach { sb.appendLine("- $it") }

    return sb.toString()
}

fun calcularDeudas(reunion: Reunion): List<String> {
    val deudaPorPersona = mutableMapOf<String, Double>()
    reunion.integrantes.forEach { nombre -> deudaPorPersona[nombre] = 0.0 }

    for (gasto in reunion.gastos) {
        val consumidores = gasto.consumidoPor
        val montoPorConsumidor = if (consumidores.isNotEmpty()) gasto.monto / consumidores.size else 0.0
        consumidores.forEach {
            deudaPorPersona[it] = deudaPorPersona[it]!! + montoPorConsumidor
        }
    }


    val balancePorPersona = reunion.integrantes.associateWith { nombre ->
        val pagado = reunion.gastos.sumOf { it.aportesIndividuales[nombre] ?: 0.0 }
        val debe = deudaPorPersona[nombre] ?: 0.0
        pagado - debe
    }


    val deudores = balancePorPersona.filterValues { it < -0.01 }.toMutableMap()
    val acreedores = balancePorPersona.filterValues { it > 0.01 }.toMutableMap()

    val resultados = mutableListOf<String>()

    for ((deudor, deuda) in deudores) {
        var pendiente = -deuda

        val pagos = mutableListOf<String>()
        val acreedoresKeys = acreedores.keys.toList()

        for (acreedor in acreedoresKeys) {
            val credito = acreedores[acreedor] ?: continue
            if (credito <= 0.01) continue

            val monto = minOf(pendiente, credito)
            pagos.add("$deudor debe pagar $${"%.2f".format(monto)} a $acreedor")

            pendiente -= monto
            acreedores[acreedor] = credito - monto

            if (pendiente <= 0.01) break
        }

        resultados.addAll(pagos)
    }

    return resultados
}
