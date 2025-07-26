package com.joasasso.minitoolbox.tools.organizacion.recordatorios

import EventoImportante
import EventosDataStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.tools.info.DateVisualTransformation
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Composable
fun EventosImportantesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val eventosFlow = remember { EventosDataStore.flujoEventos(context) }
    var eventos by remember { mutableStateOf<List<EventoImportante>>(emptyList()) }

    var showDialog by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        eventosFlow.collect { eventos = it }
    }

    Scaffold(
        topBar = {
            TopBarReusable(stringResource(R.string.tool_event_tracker), onBack, { showInfo = !showInfo })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.event_add_content_desc)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val eventosOrdenados = eventos.sortedBy { LocalDate.parse(it.fecha) }
            if (eventosOrdenados.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            stringResource(R.string.event_empty_message),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(eventosOrdenados) { evento ->
                    var remainingTime by remember { mutableStateOf(Duration.ZERO) }

                    LaunchedEffect(evento) {
                        while (isActive) {
                            val now = LocalDateTime.now()
                            val objetivo = LocalDate.parse(evento.fecha).atStartOfDay()
                            remainingTime =
                                Duration.between(now, objetivo).coerceAtLeast(Duration.ZERO)
                            delay(1000)
                        }
                    }

                    val dias = remainingTime.toDays().toInt()
                    val horas = (remainingTime.toHours() % 24)
                    val minutos = (remainingTime.toMinutes() % 60)
                    val segundos = (remainingTime.seconds % 60)

                    val bgColor = getColorForRemainingDays(dias)
                    val fgColor = getTextColorForCard(bgColor)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    evento.nombre,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = fgColor
                                )
                                Text(
                                    stringResource(R.string.event_date_prefix) +
                                            LocalDate.parse(evento.fecha)
                                                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                    color = fgColor
                                )
                                Text(
                                    stringResource(
                                        R.string.event_remaining_prefix,
                                        dias, horas, minutos, segundos
                                    ),
                                    color = fgColor
                                )
                            }

                            IconButton(onClick = {
                                scope.launch {
                                    EventosDataStore.eliminarEvento(context, evento.id)
                                    eventosFlow.collect { eventos = it }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = fgColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        DialogAgregarEventoImportante(
            onDismiss = { showDialog = false },
            onSave = { nombre, fecha ->
                scope.launch {
                    val nuevo = EventoImportante(
                        id = UUID.randomUUID().toString(),
                        nombre = nombre,
                        fecha = fecha.toString() // ISO-8601
                    )
                    EventosDataStore.agregarEvento(context, nuevo)
                    showDialog = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        )
    }
    // Menú de ayuda
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.event_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.event_help_line1))
                    Text(stringResource(R.string.event_help_line2))
                    Text(stringResource(R.string.event_help_line3))
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

@Composable
fun DialogAgregarEventoImportante(
    onDismiss: () -> Unit,
    onSave: (String, LocalDate) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var rawFecha by remember { mutableStateOf("") }
    var fechaError by remember { mutableStateOf<String?>(null) }

    val errorPasado = stringResource(R.string.event_error_past)
    val errorIncompleto = stringResource(R.string.event_error_incomplete)
    val errorInvalido = stringResource(R.string.event_error_invalid)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.event_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text(stringResource(R.string.event_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rawFecha,
                    onValueChange = {
                        rawFecha = it.filter { c -> c.isDigit() }.take(8)
                        fechaError = null
                    },
                    label = { Text(stringResource(R.string.event_date_label)) },
                    singleLine = true,
                    isError = fechaError != null,
                    supportingText = {
                        fechaError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = DateVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nombre.isBlank()) return@TextButton

                if (rawFecha.length == 8) {
                    val d = rawFecha.substring(0, 2).toInt()
                    val m = rawFecha.substring(2, 4).toInt()
                    val y = rawFecha.substring(4, 8).toInt()
                    try {
                        val date = LocalDate.of(y, m, d)
                        if (date.isBefore(LocalDate.now())) {
                            fechaError = errorPasado
                        } else {
                            onSave(nombre.trim(), date)
                        }
                    } catch (_: Exception) {
                        fechaError = errorInvalido
                    }
                } else {
                    fechaError = errorIncompleto
                }
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

fun getColorForRemainingDays(dias: Int): Color = when {
    dias <= 0 -> Color(0xFFC8E6C9) // verde suave (evento hoy)
    dias == 1 -> Color(0xFFAED581) // verde más definido
    dias == 2 -> Color(0xFFFFF59D) // amarillo pastel vibrante
    dias == 3 -> Color(0xFFFFE082) // amarillo-naranja claro
    dias == 4 -> Color(0xFFFFCC80) // naranja pastel
    dias == 5 -> Color(0xFFFFAB91) // salmón
    dias == 6 -> Color(0xFFFFCDD2) // rosado pastel
    dias in 7..9 -> Color(0xFFE1BEE7) // violeta claro
    dias in 10..13 -> Color(0xFFCE93D8) // violeta medio pastel
    dias in 14..20 -> Color(0xFFB3E5FC) // celeste claro
    dias in 21..30 -> Color(0xFFB0BEC5) // gris azulado
    dias in 31..45 -> Color(0xFFCFD8DC) // gris neutro suave
    dias in 46..60 -> Color(0xFFE0E0E0) // gris claro definido
    dias in 61..90 -> Color(0xFFEEEEEE) // gris muy claro
    else -> Color(0xFFF5F5F5) // blanco grisáceo (evento lejano)
}





