package com.joasasso.minitoolbox.tools.calculadoras.divisorGastos

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.tools.data.Gasto
import com.joasasso.minitoolbox.tools.data.Reunion
import com.joasasso.minitoolbox.tools.data.ReunionesRepository
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID


@Composable
fun AgregarGastoScreen(
    reunionId: String,
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
    var consumidores: Map<String, Int> by remember(reunion) {
        mutableStateOf(
            reunion?.integrantes?.associate { it.nombre to it.cantidad } ?: emptyMap()
        )
    }



    LaunchedEffect(Unit) {
        val reuniones = ReunionesRepository.flujoReuniones(context).firstOrNull().orEmpty()
        reunion = reuniones.find { it.id == reunionId }
        reunion?.integrantes?.associate { it.nombre to 0 }?.let { consumidores = it }
    }

    val montoTotal = aportes.values.sumOf { it.toDoubleOrNull() ?: 0.0 }

    Scaffold(
        topBar = { TopBarReusable("Nuevo gasto", onBack, { showInfo = true }) }
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
                Text("Total: $${formatter.format(montoTotal)}", style = MaterialTheme.typography.titleMedium)
            }

            item {
                Text("¿Quién pagó y cuánto?", style = MaterialTheme.typography.titleSmall)
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
                                    if (it.isNotBlank()) put(grupo.nombre, it) else remove(grupo.nombre)
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
                Text("¿Quiénes consumieron? (personas por grupo)", style = MaterialTheme.typography.titleSmall)
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
                                    val cantidad = it.toIntOrNull()?.coerceIn(0, grupo.cantidad) ?: 0
                                    put(grupo.nombre, cantidad)
                                }
                            },
                            modifier = Modifier.width(80.dp),
                            placeholder = { Text("0") },
                            singleLine = true
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val aporteValido = aportes.filterValues { it.toDoubleOrNull() != null }
                        val aportesFinales = aporteValido.mapValues { it.value.toDouble() }
                        val consumidoresFinales = consumidores.filterValues { it > 0 }

                        val nuevoGasto = Gasto(
                            id = UUID.randomUUID().toString(),
                            descripcion = descripcion,
                            aportesIndividuales = aportesFinales,
                            consumidoPor = consumidoresFinales
                        )

                        val actualizada = reunion?.copy(
                            gastos = reunion!!.gastos + nuevoGasto
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
                    Text("Guardar")
                }
            }
        }
    }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("¿Cómo registrar un gasto?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Escribe una breve descripción del gasto (por ejemplo: 'Pizza', 'Entrada', 'Coca-Cola').")
                    Text("• Indica cuánto aportó cada grupo al gasto.")
                    Text("• Luego, selecciona cuántas personas de cada grupo participaron como consumidores.")
                    Text("• El total del gasto se calcula automáticamente a partir de los aportes.")
                    Text("• El reparto se hará según el consumo proporcional de cada grupo.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showInfo = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) {
                    Text("Cerrar")
                }
            }
        )
    }

}



