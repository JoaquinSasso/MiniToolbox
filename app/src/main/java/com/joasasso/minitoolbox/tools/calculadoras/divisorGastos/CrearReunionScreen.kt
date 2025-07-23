package com.joasasso.minitoolbox.tools.calculadoras.divisorGastos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.tools.data.Grupo
import com.joasasso.minitoolbox.tools.data.Reunion
import com.joasasso.minitoolbox.tools.data.ReunionesRepository
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CrearReunionScreen(
    onBack: () -> Unit,
    onReunionCreada: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var showInfo by remember { mutableStateOf(false) }

    var nombre by remember { mutableStateOf("") }
    var nuevoGrupoNombre by remember { mutableStateOf("") }
    var nuevaCantidad by remember { mutableStateOf("1") }
    val grupos = remember { mutableStateListOf<Grupo>() }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = { TopBarReusable(stringResource(R.string.create_meeting_screen), onBack, { showInfo = true }) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Button(
                onClick = {
                    if (nombre.isBlank() || grupos.isEmpty()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Completa el nombre y agrega al menos una persona / grupo")
                        }
                        return@Button
                    }

                    val nuevaReunion = Reunion(
                        id = UUID.randomUUID().toString(),
                        nombre = nombre,
                        fecha = System.currentTimeMillis(),
                        integrantes = grupos.toList(),
                        gastos = emptyList()
                    )

                    scope.launch {
                        ReunionesRepository.agregarReunion(context, nuevaReunion)
                        onReunionCreada()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Guardar reunión")
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre de la reunión") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = nuevoGrupoNombre,
                        onValueChange = { nuevoGrupoNombre = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = nuevaCantidad,
                        onValueChange = { nuevaCantidad = it },
                        label = { Text("Personas") },
                        modifier = Modifier.width(90.dp),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                    )
                    IconButton(
                        onClick = {
                            val nombreGrupo = nuevoGrupoNombre.trim()
                            val cantidad = nuevaCantidad.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            if (nombreGrupo.isNotBlank()) {
                                grupos.add(Grupo(nombreGrupo, cantidad))
                                nuevoGrupoNombre = ""
                                nuevaCantidad = "1"
                            }
                        },
                        enabled = nuevoGrupoNombre.isNotBlank()
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Agregar integrante")
                    }
                }
            }

            items(grupos, key = { it.nombre }) { grupo ->
                var visible by remember { mutableStateOf(true) }

                AnimatedVisibility(
                    visible = visible,
                    enter = expandVertically() + fadeIn(),
                    exit = slideOutHorizontally() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${grupo.nombre} (${grupo.cantidad} personas)",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            IconButton(onClick = {
                                visible = false
                                scope.launch {
                                    delay(300)
                                    grupos.remove(grupo)
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar integrante"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("¿Cómo crear una reunión?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Asigna un nombre para identificar la reunión y agrega los grupos que participarán (personas, familias, amigos, etc.).")
                    Text("• Cada grupo puede tener un nombre y una cantidad de personas asociadas.")
                    Text("• La cantidad de personas servirá luego para repartir los gastos proporcionalmente.")
                    Text("• Podrás agregar y editar gastos una vez creada la reunión.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfo = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)}) {
                    Text("Cerrar")
                }
            }
        )
    }
}

