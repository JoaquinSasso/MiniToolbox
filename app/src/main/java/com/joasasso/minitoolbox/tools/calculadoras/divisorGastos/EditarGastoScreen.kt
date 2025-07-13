package com.joasasso.minitoolbox.tools.calculadoras.divisorGastos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.tools.data.Gasto
import com.joasasso.minitoolbox.tools.data.Reunion
import com.joasasso.minitoolbox.tools.data.ReunionesRepository
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Composable
fun EditarGastoScreen(
    reunionId: String,
    gastoId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var reunion by remember { mutableStateOf<Reunion?>(null) }
    var descripcion by remember { mutableStateOf("") }
    var montoTotal by remember { mutableStateOf("") }
    var pagadoPor by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var consumidoPor by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(Unit) {
        val reuniones = ReunionesRepository.flujoReuniones(context).firstOrNull().orEmpty()
        val r = reuniones.find { it.id == reunionId }
        val gasto = r?.gastos?.find { it.id == gastoId }
        if (r != null && gasto != null) {
            reunion = r
            descripcion = gasto.descripcion
            montoTotal = gasto.monto.toString()
            pagadoPor = gasto.aportesIndividuales.mapValues { it.value.toString() }
            consumidoPor = gasto.consumidoPor.toSet()
        }
    }

    Scaffold(
        topBar = { TopBarReusable("Editar gasto", onBack) }
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
                    label = { Text("Descripción del gasto") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = montoTotal,
                    onValueChange = { montoTotal = it },
                    label = { Text("Monto total") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                Text("¿Quién pagó y cuánto?", style = MaterialTheme.typography.titleSmall)
            }

            items(reunion?.integrantes.orEmpty()) { nombre ->
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
                            value = pagadoPor[nombre] ?: "",
                            onValueChange = {
                                pagadoPor = pagadoPor.toMutableMap().apply {
                                    if (it.isNotBlank()) put(nombre, it) else remove(nombre)
                                }
                            },
                            modifier = Modifier.width(100.dp),
                            placeholder = { Text("0.0") },
                            singleLine = true
                        )
                    }
                }
            }

            item {
                Text("¿Quiénes consumieron?", style = MaterialTheme.typography.titleSmall)
            }

            items(reunion?.integrantes.orEmpty()) { nombre ->
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
                        Text(nombre)
                        Switch(
                            checked = nombre in consumidoPor,
                            onCheckedChange = {
                                consumidoPor = if (it) consumidoPor + nombre else consumidoPor - nombre
                            }
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val montoDouble = montoTotal.toDoubleOrNull() ?: 0.0
                        val aporteValido = pagadoPor.filterValues { it.toDoubleOrNull() != null }
                        val pagadores = aporteValido.keys.toList()
                        val valores = aporteValido.mapValues { it.value.toDouble() }

                        val nuevoGasto = Gasto(
                            id = gastoId,
                            descripcion = descripcion,
                            monto = montoDouble,
                            pagadoPor = pagadores,
                            consumidoPor = consumidoPor.toList(),
                            aportesIndividuales = valores
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
                    Text("Guardar cambios")
                }
            }
        }
    }
}

