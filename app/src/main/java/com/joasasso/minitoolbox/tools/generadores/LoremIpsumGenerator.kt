package com.joasasso.minitoolbox.tools.generadores

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
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneradorLoremIpsumScreen(onBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // State
    var modo by remember { mutableStateOf("Párrafos") } // o "Palabras"
    var cantidad by remember { mutableStateOf(3) }
    var textoGenerado by remember { mutableStateOf("") }
    var showInfo by remember { mutableStateOf(false) }

    // Límites dinámicos
    val minCantidad = if (modo == "Palabras") 1 else 1
    val maxCantidad = if (modo == "Palabras") 30 else 10

    // Generar Lorem
    fun generarLorem() {
        textoGenerado = if (modo == "Palabras") {
            generarLoremIpsumPalabras(cantidad)
        } else {
            generarLoremIpsumParrafos(cantidad)
        }
    }

    fun reset() {
        cantidad = if (modo == "Palabras") 10 else 3
        textoGenerado = ""
    }

    Scaffold(
        topBar = {TopBarReusable("Generador de Lorem Ipsum", onBack, {showInfo = true})},
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Genera texto de ejemplo clásico para maquetar o testear.",
                fontSize = 17.sp, color = MaterialTheme.colorScheme.primary
            )
            // Selector de modo
            Row(verticalAlignment = Alignment.CenterVertically) {
                SegmentedButton(
                    options = listOf("Párrafos", "Palabras"),
                    selected = modo,
                    onSelect = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        modo = it
                        cantidad = if (it == "Palabras") 10 else 3
                        textoGenerado = ""
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
            // Slider para cantidad
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (modo == "Palabras") "Cantidad de palabras" else "Cantidad de párrafos",
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "$cantidad",
                    fontSize = 26.sp,
                    color = MaterialTheme.colorScheme.primary
                )
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

            // Botones acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        generarLorem()
                    }
                ) { Text("Generar") }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        textoGenerado = ""
                        reset()
                    }
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Limpiar")
                    Spacer(Modifier.width(4.dp))
                    Text("Limpiar")
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(textoGenerado))
                        scope.launch { snackbarHostState.showSnackbar("Texto copiado") }
                    },
                    enabled = textoGenerado.isNotBlank()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar texto")
                    Spacer(Modifier.width(4.dp))
                    Text("Copiar")
                }
            }
            // Resultado
            if (textoGenerado.isNotBlank()) {
                Divider(Modifier.padding(vertical = 8.dp))
                Text("Texto generado:", fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(18.dp)
                    ) {
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
            title = { Text("¿Qué es Lorem Ipsum?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Es un texto ficticio en latín usado desde hace siglos para probar diseños y maquetar páginas.")
                    Text("• Puedes generar la cantidad de palabras o párrafos que quieras y copiar el texto rápidamente.")
                    Text("• Todo se genera localmente, no se utiliza conexión a internet.")
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
