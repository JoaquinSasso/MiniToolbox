package com.joasasso.minitoolbox.tools.calculadoras.divisorGastos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import com.joasasso.minitoolbox.tools.data.GastosDataStore.obtenerReunionPorId
import com.joasasso.minitoolbox.tools.data.Reunion
import com.joasasso.minitoolbox.tools.data.ReunionesRepository
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.launch
import java.util.UUID


@Composable
fun AgregarGastoScreen(
    reunionId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var reunion by remember { mutableStateOf<Reunion?>(null) }
    var descripcion by remember { mutableStateOf("") }
    var aportes by remember { mutableStateOf(mutableMapOf<String, String>()) }
    var consumen by remember { mutableStateOf(mutableMapOf<String, Boolean>()) }

    // Cargar reunion
    LaunchedEffect(Unit) {
        reunion = obtenerReunionPorId(context, reunionId)
        reunion?.integrantes?.forEach {
            aportes[it] = ""
            consumen[it] = true
        }
    }

    Scaffold(
        topBar = { TopBarReusable("Agregar gasto", onBack) }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(), start = 16.dp, end = 16.dp, bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Nombre del gasto") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text("¿Quién pagó y cuánto?", style = MaterialTheme.typography.titleSmall)
            }

            reunion?.integrantes?.forEach { integrante ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(integrante, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(12.dp))
                            OutlinedTextField(
                                value = aportes[integrante] ?: "",
                                onValueChange = {
                                    aportes = aportes.toMutableMap().apply {
                                        this[integrante] = it
                                    }
                                },
                                label = { Text("Monto") },
                                singleLine = true,
                                modifier = Modifier.width(100.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("¿Quién consume este gasto?", style = MaterialTheme.typography.titleSmall)
            }

            reunion?.integrantes?.forEach { integrante ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(integrante)
                            Switch(
                                checked = consumen[integrante] ?: true,
                                onCheckedChange = {
                                    consumen = consumen.toMutableMap().apply {
                                        this[integrante] = it
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val listaPagadores = aportes
                            .mapNotNull { (nombre, monto) ->
                                if (monto.toDoubleOrNull() != null && monto.toDouble() > 0) nombre else null
                            }

                        val totalMonto = aportes
                            .mapNotNull { it.value.toDoubleOrNull() }
                            .sum()

                        val nuevoGasto = Gasto(
                            id = UUID.randomUUID().toString(),
                            descripcion = descripcion,
                            monto = totalMonto,
                            pagadoPor = listaPagadores,
                            consumidoPor = consumen.filterValues { it }.keys.toList(),
                            aportesIndividuales = aportes.mapNotNullValues { it.toDoubleOrNull() }
                        )

                        scope.launch {
                            val actual = obtenerReunionPorId(context, reunionId) ?: return@launch
                            val actualizada = actual.copy(gastos = actual.gastos + nuevoGasto)
                            ReunionesRepository.actualizarReunion(context, actualizada)
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Guardar gasto")
                }
            }
        }
    }
}

// Función de extensión auxiliar para mapNotNull con valores
private inline fun <K, V, R> Map<K, V>.mapNotNullValues(transform: (V) -> R?): Map<K, R> {
    return buildMap {
        for ((key, value) in this@mapNotNullValues) {
            val result = transform(value)
            if (result != null) put(key, result)
        }
    }
}

