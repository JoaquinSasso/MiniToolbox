package com.joasasso.minitoolbox.tools.organizacion.divisorGastos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.Reunion
import com.joasasso.minitoolbox.data.ReunionesRepository
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CrearReunionScreen(
    onBack: () -> Unit,
    onReunionCreada: (String) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var showInfo by remember { mutableStateOf(false) }

    var nombre by remember { mutableStateOf("") }
    var nuevoIntegranteNombre by remember { mutableStateOf("") }
    val integrantes = remember { mutableStateListOf<String>() }
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarError = stringResource(R.string.create_meeting_snackbar_error)

    Scaffold(
        topBar = {
            TopBarReusable(stringResource(R.string.create_meeting_screen), onBack) {
                showInfo = true
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Button(
                onClick = {
                    if (nombre.isBlank() || integrantes.isEmpty()) {
                        scope.launch {
                            snackbarHostState.showSnackbar(snackbarError)
                        }
                        return@Button
                    }

                    val meetingId = UUID.randomUUID().toString()
                    val nuevaReunion = Reunion(
                        id = meetingId,
                        nombre = nombre,
                        fecha = System.currentTimeMillis(),
                        integrantes = integrantes.toList(),
                        gastos = emptyList()
                    )

                    scope.launch {
                        ReunionesRepository.agregarReunion(context, nuevaReunion)
                        onReunionCreada(meetingId)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(stringResource(R.string.create_meeting_button))
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
                    label = { Text(stringResource(R.string.create_meeting_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    fun agregarIntegrante() {
                        val nombreIntegrante = nuevoIntegranteNombre.trim()
                        if (nombreIntegrante.isNotBlank() && !integrantes.contains(nombreIntegrante)) {
                            integrantes.add(nombreIntegrante)
                            nuevoIntegranteNombre = ""
                        }
                    }

                    OutlinedTextField(
                        value = nuevoIntegranteNombre,
                        onValueChange = { nuevoIntegranteNombre = it },
                        label = { Text(stringResource(R.string.create_meeting_group_name)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { agregarIntegrante() }
                        )
                    )
                    IconButton(
                        onClick = { agregarIntegrante() },
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = stringResource(R.string.create_meeting_add_member),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            items(integrantes, key = { it }) { integrante ->
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
                                text = integrante,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            IconButton(onClick = {
                                visible = false
                                scope.launch {
                                    delay(300)
                                    integrantes.remove(integrante)
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.create_meeting_remove_member)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showInfo) {
            AlertDialog(
                onDismissRequest = { showInfo = false },
                title = { Text(stringResource(R.string.create_meeting_help_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.create_meeting_help_line1))
                        Text(stringResource(R.string.create_meeting_help_line2))
                        Text(stringResource(R.string.create_meeting_help_line3))
                        Text(stringResource(R.string.create_meeting_help_line4))
                        Text(stringResource(R.string.create_meeting_help_line5))
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
}
