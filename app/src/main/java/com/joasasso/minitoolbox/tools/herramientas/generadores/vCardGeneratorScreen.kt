package com.joasasso.minitoolbox.tools.herramientas.generadores

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.tools.data.QrContacto
import com.joasasso.minitoolbox.tools.data.QrContactoDataStore
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import com.lightspark.composeqr.QrCodeView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneradorQrContactoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dataStore = remember { QrContactoDataStore(context) }
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val datosGuardados by dataStore.datos.collectAsState(initial = QrContacto())

    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var showInfo by remember { mutableStateOf(false) }

    // Cargar datos guardados al iniciar
    LaunchedEffect(datosGuardados) {
        nombre = datosGuardados.nombre
        telefono = datosGuardados.telefono
        email = datosGuardados.email
    }

    fun guardarDatos() {
        scope.launch {
            dataStore.guardar(
                QrContacto(
                    nombre = nombre,
                    telefono = telefono,
                    email = email
                )
            )
        }
    }

    // vCard builder minimalista
    fun generarVCard(): String {
        return buildString {
            appendLine("BEGIN:VCARD")
            appendLine("VERSION:3.0")
            if (nombre.isNotBlank()) appendLine("FN:${nombre.trim()}")
            if (telefono.isNotBlank()) appendLine("TEL;TYPE=CELL:${telefono.trim()}")
            if (email.isNotBlank()) appendLine("EMAIL:${email.trim()}")
            appendLine("END:VCARD")
        }
    }

    val vCard = generarVCard()
    val qrEnabled = nombre.isNotBlank() && telefono.isNotBlank()

    Scaffold(
        topBar = {
            TopBarReusable(
                stringResource(R.string.tool_qr_vcard),
                onBack,
                { showInfo = true })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        guardarDatos()
                    },
                    label = { Text(stringResource(R.string.qr_vcard_label_nombre)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = telefono,
                    onValueChange = {
                        telefono =
                            it.filter { c -> c.isDigit() || c == '+' || c == ' ' || c == '-' }
                        guardarDatos()
                    },
                    label = { Text(stringResource(R.string.qr_vcard_label_telefono)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        guardarDatos()
                    },
                    label = { Text(stringResource(R.string.qr_vcard_label_email)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(vCard))
                        },
                        enabled = qrEnabled
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.copy) + " vCard"
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.copy) + " vCard")
                    }

                    Spacer(Modifier.width(16.dp))

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            nombre = ""
                            telefono = ""
                            email = ""
                            scope.launch { dataStore.limpiar() }
                        },
                        enabled = nombre.isNotBlank() || telefono.isNotBlank() || email.isNotBlank()
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.clear)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.clear))
                    }
                }

                HorizontalDivider(
                    Modifier.padding(vertical = 8.dp),
                    DividerDefaults.Thickness,
                    DividerDefaults.color
                )

                Card(
                    modifier = Modifier
                        .size(300.dp)
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (qrEnabled) {
                            QrCodeView(
                                data = vCard,
                                modifier = Modifier.size(270.dp)
                            )
                        } else {
                            Text(
                                stringResource(R.string.qr_vcard_texto_incompleto),
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showInfo = false
            },
            title = { Text(stringResource(R.string.qr_vcard_help_titulo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.qr_vcard_help_linea1))
                    Text(stringResource(R.string.qr_vcard_help_linea2))
                    Text(stringResource(R.string.qr_vcard_help_linea3))
                    Text(stringResource(R.string.qr_vcard_help_linea4))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showInfo = false
                }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

