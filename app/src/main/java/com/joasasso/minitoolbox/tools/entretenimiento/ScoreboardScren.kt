package com.joasasso.minitoolbox.tools.entretenimiento

import Equipo
import MarcadorPrefs
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarcadorEquiposScreen(onBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var showInfo by remember { mutableStateOf(false) }

    val context = LocalContext.current

    var equipos: List<Equipo> by remember {
        mutableStateOf(
            listOf(
                Equipo(nombre = "Equipo 1", puntos = 0, color = Color(0xFFFFF9C4)),
                Equipo(nombre = "Equipo 2", puntos = 0, color = Color(0xFFFFCCBC))
            )
        )
    }

    LaunchedEffect(Unit) {
        equipos = MarcadorPrefs.loadAll(context)
    }

    LaunchedEffect(equipos) {
        MarcadorPrefs.saveAll(context, equipos)
    }


    val colorOptions = listOf(
        Color(0xFFFFF9C4), Color(0xFFFFCCBC), Color(0xFF76D7C4),
        Color(0xFFB2EBF2), Color(0xFFC8E6C9), Color(0xFFD1C4E9),
        Color(0xFFFFECB3), Color(0xFFC71FE8), Color(0xFFDCEDC8),
        Color(0xFF723855), Color(0xFF5E08C2), Color(0xFF4DBC52),
        Color(0xFFFF746C)
    )

    var showEditDialogFor by remember { mutableStateOf<Int?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var editedColor by remember { mutableStateOf(Color(0xFFFFF9C4)) }

    Scaffold(
        topBar = {
            TopBarReusable(stringResource(R.string.tool_scoreboard), onBack) {
                showInfo = true
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (equipos.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            showResetDialog = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        icon = { Icon(Icons.Default.RestartAlt, contentDescription = stringResource(R.string.marcador_reiniciar_todo_content_desc)) },
                        text = { Text(stringResource(R.string.marcador_reiniciar_todo)) },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                ExtendedFloatingActionButton(onClick = {
                    equipos = equipos + Equipo(nombre = "${context.getString(R.string.marcador_equipo)} ${equipos.size + 1}", puntos = 0, color = colorOptions.random())
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                    icon = { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.marcador_agregar_equipo_content_desc)) },
                    text = { Text(stringResource(R.string.marcador_agregar_equipo_content_desc)) })
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (equipos.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.marcador_no_hay_equipos),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth().padding(all = 16.dp))
                    }
                }
            }
            itemsIndexed(equipos) { index, equipo ->
                val textColor = getContrastingTextColor(equipo.color)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = equipo.color
                    )
                ) {
                    CompositionLocalProvider(LocalContentColor provides textColor)
                    {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(equipo.nombre, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = {
                                    showEditDialogFor = index
                                    editedName = equipo.nombre
                                    editedColor = equipo.color
                                }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.marcador_editar_equipo_content_desc)
                                    )
                                }
                                IconButton(onClick = {
                                    equipos = equipos.toMutableList().also { it.removeAt(index) }
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.marcador_eliminar_equipo_content_desc)
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    if (equipo.puntos > 0) {
                                        equipos = equipos.toMutableList().also {
                                            it[index] =
                                                it[index].copy(puntos = it[index].puntos - 1)
                                        }
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = stringResource(R.string.marcador_restar_punto_content_desc)
                                    )
                                }

                                Text(
                                    equipo.puntos.toString(),
                                    style = MaterialTheme.typography.displaySmall
                                )

                                IconButton(onClick = {
                                    equipos = equipos.toMutableList().also {
                                        it[index] = it[index].copy(puntos = it[index].puntos + 1)
                                    }
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = stringResource(R.string.marcador_sumar_punto_content_desc)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    showEditDialogFor?.let { index ->
        AlertDialog(
            onDismissRequest = { showEditDialogFor = null },
            title = { Text(stringResource(R.string.marcador_editar_equipo_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text(stringResource(R.string.marcador_editar_equipo_nombre)) }
                    )
                    Text(stringResource(R.string.marcador_editar_equipo_color))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        items(colorOptions) { color ->
                            Box(
                                modifier = Modifier
                                    .size( if (color == editedColor) 36.dp else 30.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { editedColor = color }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    equipos = equipos.toMutableList().also {
                        it[index] = it[index].copy(nombre = editedName, color = editedColor)
                    }
                    showEditDialogFor = null
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialogFor = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.marcador_reset_dialog_titulo)) },
            text = { Text(stringResource(R.string.marcador_reset_dialog_texto)) },
            confirmButton = {
                TextButton(onClick = {
                    equipos = emptyList()
                    showResetDialog = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) {
                    Text(stringResource(R.string.marcador_reset_dialog_confirmar))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }) {
                    Text(stringResource(R.string.marcador_reset_dialog_cancelar))
                }
            }
        )
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.marcador_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.marcador_help_linea1))
                    Text(stringResource(R.string.marcador_help_linea2))
                    Text(stringResource(R.string.marcador_help_linea3))
                    Text(stringResource(R.string.marcador_help_linea4))
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

fun getContrastingTextColor(bg: Color): Color {
    return if (bg.luminance() > 0.5f) Color.Black else Color.White
}

