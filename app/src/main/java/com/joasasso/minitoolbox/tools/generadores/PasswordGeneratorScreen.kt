package com.joasasso.minitoolbox.tools.generadores

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneradorContrasenaScreen(onBack: () -> Unit) {
    var showInfo by remember { mutableStateOf(false) }
    var longitud by remember { mutableStateOf(12) }
    var incluirMayusculas by remember { mutableStateOf(true) }
    var incluirMinusculas by remember { mutableStateOf(true) }
    var incluirNumeros by remember { mutableStateOf(true) }
    var incluirSimbolos by remember { mutableStateOf(true) }
    var contrasena by remember { mutableStateOf("") }

    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    fun generar() {
        val mayus = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val minus = "abcdefghijklmnopqrstuvwxyz"
        val nums = "0123456789"
        val simbolos = "!@#\$%&*?_-+=()[]"
        var chars = ""
        if (incluirMayusculas) chars += mayus
        if (incluirMinusculas) chars += minus
        if (incluirNumeros) chars += nums
        if (incluirSimbolos) chars += simbolos

        contrasena = if (chars.isNotEmpty()) {
            (1..longitud)
                .map { chars[Random.nextInt(chars.length)] }
                .joinToString("")
        } else ""
    }

    // Generar una contraseña al inicio
    LaunchedEffect(Unit) { generar() }

    Scaffold(
        topBar = {TopBarReusable("Generador de Contraseñas", onBack, {showInfo = true})},
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Tu nueva contraseña segura",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedTextField(
                value = contrasena,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = 22.sp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        generar()
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Nueva contraseña")
                    Spacer(Modifier.width(4.dp))
                    Text("Generar otra")
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(contrasena))
                        scope.launch { snackbarHostState.showSnackbar("Contraseña copiada") }
                    },
                    enabled = contrasena.isNotBlank()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar contraseña")
                    Spacer(Modifier.width(4.dp))
                    Text("Copiar")
                }
            }

            // Configuración de la contraseña
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Text("Longitud: $longitud", modifier = Modifier.padding(start = 4.dp))
                Slider(
                    value = longitud.toFloat(),
                    onValueChange = {
                        longitud = it.toInt()
                        generar()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    valueRange = 6f..32f,
                    steps = 26,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Mayúsculas")
                    Switch(
                        checked = incluirMayusculas,
                        onCheckedChange = {
                            incluirMayusculas = it
                            generar()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Minúsculas")
                    Switch(
                        checked = incluirMinusculas,
                        onCheckedChange = {
                            incluirMinusculas = it
                            generar()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Números")
                    Switch(
                        checked = incluirNumeros,
                        onCheckedChange = {
                            incluirNumeros = it
                            generar()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Símbolos")
                    Switch(
                        checked = incluirSimbolos,
                        onCheckedChange = {
                            incluirSimbolos = it
                            generar()
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    )
                }
                Spacer(Modifier.height(32.dp))
                Text(
                    "La contraseña se genera de forma local en tu dispositivo y no se almacena ni se envía a ningún servidor.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    lineHeight = 16.sp
                )

            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showInfo = false
            },
            title = { Text("Acerca del generador de contraseñas") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Generá contraseñas seguras y aleatorias para tus cuentas.")
                    Text("• Podés elegir la longitud y qué tipos de caracteres incluir.")
                    Text("• Se recomienda usar contraseñas largas y mezclar mayúsculas, minúsculas, números y símbolos.")
                    Text("• Copiá la contraseña fácilmente con el botón 'Copiar'.")
                    Text("• No guardes contraseñas inseguras o fáciles de adivinar (ejemplo: 123456, password, etc).")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showInfo = false
                }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
