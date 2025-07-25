package com.joasasso.minitoolbox.tools.herramientas.generadores

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneradorContrasenaScreen(onBack: () -> Unit) {
    var showInfo by remember { mutableStateOf(false) }
    var longitud by remember { mutableIntStateOf(12) }
    var incluirMayusculas by remember { mutableStateOf(true) }
    var incluirMinusculas by remember { mutableStateOf(true) }
    var incluirNumeros by remember { mutableStateOf(true) }
    var incluirSimbolos by remember { mutableStateOf(true) }
    var contrasena by remember { mutableStateOf("") }
    var sliderValue by remember { mutableFloatStateOf(longitud.toFloat()) }
    var lastCantidad by remember { mutableIntStateOf(longitud) }

    val clipboardManager = LocalClipboardManager.current
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
        topBar = {TopBarReusable(stringResource(R.string.tool_password_generator), onBack, {showInfo = true})}
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
                stringResource(R.string.password_generator_subtitulo),
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
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.password_generator_boton_nuevo))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.password_generator_boton_nuevo))
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        clipboardManager.setText(AnnotatedString(contrasena))
                    },
                    enabled = contrasena.isNotBlank()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.copy))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.copy))
                }
            }

            // Configuración de la contraseña
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Text(
                    stringResource(R.string.password_generator_longitud, longitud),
                    modifier = Modifier.padding(start = 4.dp)
                )
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = it
                        val nuevoValor = it.toInt()
                        if (nuevoValor != lastCantidad) {
                            longitud = nuevoValor
                            lastCantidad = nuevoValor
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        }
                    },
                    valueRange = 6f..32f,
                    steps = 26,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.password_generator_mayusculas))
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
                    Text(stringResource(R.string.password_generator_minusculas))
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
                    Text(stringResource(R.string.password_generator_numeros))
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
                    Text(stringResource(R.string.password_generator_simbolos))
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
                    stringResource(R.string.password_generator_privacidad),
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
            title = { stringResource(R.string.password_generator_ayuda_titulo) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.password_generator_ayuda_linea1))
                    Text(stringResource(R.string.password_generator_ayuda_linea2))
                    Text(stringResource(R.string.password_generator_ayuda_linea3))
                    Text(stringResource(R.string.password_generator_ayuda_linea4))
                    Text(stringResource(R.string.password_generator_ayuda_linea5))
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
