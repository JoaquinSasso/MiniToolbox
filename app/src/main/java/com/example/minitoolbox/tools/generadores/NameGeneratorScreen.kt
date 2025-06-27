package com.example.minitoolbox.tools.generadores

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class NombreTipo(val display: String) {
    Masculino("Masculino"),
    Femenino("Femenino"),
    Mixto("Mixto"),
    Gracioso("Gracioso")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneradorNombresScreen(onBack: () -> Unit) {
    val nombresMasculinos = listOf(
        "Lucas", "Mateo", "Juan", "Santiago", "Benjamín", "Lautaro", "Martín", "Joaquín", "Tomás", "Facundo",
        "Emiliano", "Franco", "Bruno", "Andrés", "Ramiro", "Nicolás", "Agustín", "Iván", "Matías", "Esteban"
    )
    val nombresFemeninos = listOf(
        "Sofía", "Valentina", "Martina", "Camila", "Lucía", "Isabella", "Emilia", "Julieta", "Mía", "Agustina",
        "Gabriela", "Aitana", "Milagros", "Paula", "Florencia", "Renata", "Jazmín", "Bianca", "Carolina", "Nicole"
    )
    val apellidos = listOf(
        "Pérez", "Gómez", "Rodríguez", "Fernández", "López", "Díaz", "Martínez", "Romero", "Sosa", "Torres",
        "Álvarez", "Acosta", "Silva", "Suárez", "Castro", "Molina", "Ortiz", "Medina", "Herrera", "Gutiérrez",
        "Cruz", "Moreno", "Reyes", "Ruiz", "Navarro", "Aguirre", "Rojas", "Vega", "Ibáñez", "Muñoz"
    )
    val nombresGraciosos = listOf(
        "Elsa Pato",
        "Aitor Tilla",
        "Mario Neta",
        "Zoila Vaca",
        "Paco Merlo",
        "Igor Dito",
        "Elena Nito",
        "Alan Brito",
        "Olga Casco",
        "Juan Estan Camino",
        "Carlos Perez Gil",
        "Luz Cuesta Mogollón",
        "Elton Tito",
        "Elvis Nieto",
        "Esteban Dido",
        "Lola Mento",
        "Lucho Mucho",
        "Clara Mente",
        "Pancho Villa",
        "Coco Liso",
        "Cacho Castaña",
        "Chiqui Tapia",
        "Lola Lazo",
        "Zoila Cerda",
        "Kito Fokito",
        "Toka Fondo",
        "Aitor Menta",
        "Paco Jones",
        "Keko Jones",
        "Elsa Nitario",
        "Aitor Nillos"
    )


    var tipo by remember { mutableStateOf(NombreTipo.Mixto) }
    var incluirApellido by remember { mutableStateOf(true) }
    var nombre by remember { mutableStateOf("") }
    var showInfo by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    fun generar() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        nombre = when (tipo) {
            NombreTipo.Masculino -> {
                val nom = nombresMasculinos.random()
                if (incluirApellido) "$nom ${apellidos.random()}" else nom
            }
            NombreTipo.Femenino -> {
                val nom = nombresFemeninos.random()
                if (incluirApellido) "$nom ${apellidos.random()}" else nom
            }
            NombreTipo.Mixto -> {
                val nombres = nombresMasculinos + nombresFemeninos
                val nom = nombres.random()
                if (incluirApellido) "$nom ${apellidos.random()}" else nom
            }
            NombreTipo.Gracioso -> nombresGraciosos.random()
        }
    }

    // Generar uno al inicio o cuando cambia tipo o el toggle
    LaunchedEffect(tipo, incluirApellido) { generar() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generador de Nombres") },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showInfo = true
                    }) {
                        Icon(Icons.Filled.Info, contentDescription = "Información")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Selecciona el tipo de nombre",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )

            // Botones en dos filas
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(NombreTipo.Masculino, NombreTipo.Femenino).forEach {
                        Button(
                            onClick = {
                                tipo = it
                                generar()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (tipo == it) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (tipo == it) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(it.display, fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(NombreTipo.Mixto, NombreTipo.Gracioso).forEach {
                        Button(
                            onClick = {
                                tipo = it
                                generar()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (tipo == it) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (tipo == it) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(it.display, fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Nombre generado:",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        nombre,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(2.dp)
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { generar() }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Nuevo nombre")
                    Spacer(Modifier.width(4.dp))
                    Text("Generar otro")
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(nombre))
                        scope.launch { snackbarHostState.showSnackbar("Nombre copiado") }
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar nombre")
                    Spacer(Modifier.width(4.dp))
                    Text("Copiar")
                }
            }
            // Toggle al final
            Spacer(Modifier.height(18.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("¿Agregar apellido?", fontSize = 15.sp)
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = incluirApellido,
                    onCheckedChange = {
                        incluirApellido = it
                        generar()
                    }
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
            title = { Text("¿Para qué sirve?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Genera nombres completos aleatorios: masculinos, femeninos, mixtos o graciosos.")
                    Text("• Elegí si querés incluir o no apellido (excepto en modo gracioso, que el chiste es el nombre completo).")
                    Text("• Útil para juegos, pruebas, equipos, personajes, mascotas, usuarios, etc.")
                    Text("• En modo gracioso genera combinaciones humorísticas.")
                    Text("• Tocá “Generar otro” para una nueva combinación, o copialo si te gusta.")
                    Text("• Sabemos que algunos nombres con doble sentido hacen reír, pero para que la app siga siendo apta para todo público, tuvimos que dejar solo los más inocentes. ¡Gracias por entender!")
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
