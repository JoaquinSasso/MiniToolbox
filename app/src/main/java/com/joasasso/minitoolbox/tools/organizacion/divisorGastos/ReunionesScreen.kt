package com.joasasso.minitoolbox.tools.organizacion.divisorGastos

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReunionesScreen(
    onBack: () -> Unit,
    onCrearReunion: () -> Unit,
    onReunionClick: (Reunion) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var showInfo by remember { mutableStateOf(false) }
    var reuniones by remember { mutableStateOf<List<Reunion>>(emptyList()) }
    var reunionAEliminar by remember { mutableStateOf<Reunion?>(null) }

    LaunchedEffect(Unit) {
        ReunionesRepository.flujoReuniones(context).collect {
            reuniones = it.sortedByDescending { r -> r.fecha }
        }
    }

    Scaffold(
        topBar = {
            TopBarReusable(stringResource(R.string.tool_expense_splitter), onBack) {
                showInfo = true
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCrearReunion) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.expense_new_meeting_content_desc)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (reuniones.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.expense_no_meetings))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(reuniones) { reunion ->
                        ReunionItem(
                            reunion = reunion,
                            onClick = {
                                onReunionClick(reunion)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDelete = {
                                reunionAEliminar = reunion
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.expense_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.expense_help_line1))
                    Text(stringResource(R.string.expense_help_line2))
                    Text(stringResource(R.string.expense_help_line3))
                    Text(stringResource(R.string.expense_help_line4))
                    Text(stringResource(R.string.expense_help_line5))
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

    if (reunionAEliminar != null) {
        AlertDialog(
            onDismissRequest = { reunionAEliminar = null },
            title = { Text(stringResource(R.string.expense_delete_title)) },
            text = {
                Text(stringResource(R.string.expense_delete_message, reunionAEliminar!!.nombre))
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        ReunionesRepository.eliminarReunion(context, reunionAEliminar!!.id)
                        reunionAEliminar = null
                    }
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    reunionAEliminar = null
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun ReunionItem(reunion: Reunion, onClick: () -> Unit, onDelete: () -> Unit) {
    val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val fechaTexto = formato.format(Date(reunion.fecha))
    val integrantes = reunion.integrantes.map { it.nombre }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onClick() }
                ) {
                    Text(reunion.nombre, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text("${stringResource(R.string.expense_date)} $fechaTexto", style = MaterialTheme.typography.bodyMedium)
                    Text("${stringResource(R.string.expense_members)} ${integrantes.joinToString()}", style = MaterialTheme.typography.bodyMedium)
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
        }
    }
}

