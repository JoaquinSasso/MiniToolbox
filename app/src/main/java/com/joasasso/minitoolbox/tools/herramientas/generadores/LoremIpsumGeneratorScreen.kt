package com.joasasso.minitoolbox.tools.herramientas.generadores

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneradorLoremIpsumScreen(onBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var showInfo by remember { mutableStateOf(false) }

    // Opciones de modo
    val modoParrafos = stringResource(R.string.lorem_modo_parrafos)
    val modoPalabras = stringResource(R.string.lorem_modo_palabras)
    val opcionesModo = listOf(modoParrafos, modoPalabras)

    var modo by remember { mutableStateOf(modoParrafos) }
    var cantidad by remember { mutableStateOf(3) }
    var textoGenerado by remember { mutableStateOf("") }

    val minCantidad = 1
    val maxCantidad = if (modo == modoPalabras) 30 else 10

    fun generarLorem() {
        textoGenerado = if (modo == modoPalabras) {
            generarLoremIpsumPalabras(cantidad)
        } else {
            generarLoremIpsumParrafos(cantidad)
        }
    }

    fun reset() {
        cantidad = if (modo == modoPalabras) 10 else 3
        textoGenerado = ""
    }

    Scaffold(
        topBar = {
            TopBarReusable(
                title = stringResource(R.string.tool_lorem_ipsum),
                onBack = onBack,
                 { showInfo = true }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.lorem_subtitulo),
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.primary
            )

            // Selector de modo
            SegmentedButton(
                options = opcionesModo,
                selected = modo,
                onSelect = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    modo = it
                    reset()
                }
            )

            // Selector de cantidad
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (modo == modoPalabras)
                        stringResource(R.string.lorem_cantidad_palabras)
                    else
                        stringResource(R.string.lorem_cantidad_parrafos),
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(2.dp))
                Text("$cantidad", fontSize = 26.sp, color = MaterialTheme.colorScheme.primary)
                Slider(
                    value = cantidad.toFloat(),
                    onValueChange = {
                        cantidad = it.toInt()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    valueRange = minCantidad.toFloat()..maxCantidad.toFloat(),
                    steps = maxCantidad - minCantidad - 1,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // Botones de acción
            Row(horizontalArrangement = Arrangement.Center) {
                Button(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    generarLorem()
                }) {
                    Text(stringResource(R.string.lorem_boton_generar))
                }
                Spacer(Modifier.width(12.dp))
                Button(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    reset()
                }) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.clear))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.lorem_boton_limpiar))
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        clipboardManager.setText(AnnotatedString(textoGenerado))
                        scope.launch { /* Snackbar opcional */ }
                    },
                    enabled = textoGenerado.isNotBlank()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.copy))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.lorem_boton_copiar))
                }
            }

            // Resultado generado
            if (textoGenerado.isNotBlank()) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(stringResource(R.string.lorem_etiqueta_resultado), fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Box(Modifier.padding(18.dp)) {
                        SelectionContainer {
                            Text(textoGenerado, fontSize = 15.sp)
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
            title = { Text(stringResource(R.string.lorem_help_titulo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.lorem_help_linea1))
                    Text(stringResource(R.string.lorem_help_linea2))
                    Text(stringResource(R.string.lorem_help_linea3))
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

// --- Utilidades para generar Lorem Ipsum ---

private val LOREM_IPSUM = """
    Lorem ipsum dolor sit amet, consectetur adipiscing elit. Pellentesque vel malesuada neque. Nam non dictum erat. Suspendisse at viverra elit, ac convallis neque. Proin suscipit luctus dui, ut dictum nunc convallis ut. Etiam sollicitudin augue eu porttitor cursus. Etiam pulvinar, lacus at dictum rutrum, neque magna posuere arcu, nec euismod massa purus et massa. Sed id nibh vel ex dictum facilisis. Mauris vel nibh tellus. Sed varius, velit in gravida venenatis, enim massa blandit purus, ut tempor ex enim ac sem.
""".trimIndent().split(Regex("\\s+"))

fun generarLoremIpsumPalabras(cant: Int): String {
    val base = LOREM_IPSUM
    val resultado = mutableListOf<String>()
    var i = 0
    while (resultado.size < cant) {
        resultado.add(base[i % base.size])
        i++
    }
    return resultado.joinToString(" ")
}

fun generarLoremIpsumParrafos(cant: Int): String {
    val parrafo = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Pellentesque vel malesuada neque. Nam non dictum erat. Suspendisse at viverra elit, ac convallis neque. Proin suscipit luctus dui, ut dictum nunc convallis ut. Etiam sollicitudin augue eu porttitor cursus. Etiam pulvinar, lacus at dictum rutrum, neque magna posuere arcu, nec euismod massa purus et massa. Sed id nibh vel ex dictum facilisis. Mauris vel nibh tellus. Sed varius, velit in gravida venenatis, enim massa blandit purus, ut tempor ex enim ac sem."
    return (1..cant).joinToString("\n\n") { parrafo }
}

// --- Segmentado sencillo reutilizable ---
@Composable
fun SegmentedButton(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        Modifier
            .height(40.dp)
            .wrapContentWidth()
    ) {
        options.forEach { option ->
            val isSelected = selected == option
            Button(
                onClick = { onSelect(option) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier
                    .widthIn(min = 90.dp)
                    .height(40.dp)
            ) {
                Text(option)
            }
            if (option != options.last()) Spacer(Modifier.width(2.dp))
        }
    }
}
