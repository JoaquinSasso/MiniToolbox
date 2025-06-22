package com.example.minitoolbox.tools.generadores

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalHapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSelectorScreen(onBack: () -> Unit) {
    var selectedSize by remember { mutableIntStateOf(2) }
    var fingerPositions by remember { mutableStateOf<Map<PointerId, Offset>>(emptyMap()) }
    var showError by remember { mutableStateOf(false) }
    var teams by remember { mutableStateOf<Map<PointerId, Int>>(emptyMap()) }
    val haptic = LocalHapticFeedback.current
    val teamColors = remember {
        listOf(
            Color(0xFF2196F3), // Azul
            Color(0xFFF44336), // Rojo
            Color(0xFF4CAF50), // Verde
            Color(0xFFFFC107), // Amarillo
            Color(0xFF9C27B0)  // Violeta
        )
    }

    // Agrupa los dedos en equipos cuando hay dedos y el número es divisible
    // Solo depende de los IDs, no de las posiciones
    val fingerIds = fingerPositions.keys.toList()

    LaunchedEffect(fingerIds, selectedSize) {
        val count = fingerIds.size
        if (count > 0) {
            if (count % selectedSize == 0) {
                showError = false
                delay(1000)
                if (fingerPositions.keys.toSet() == fingerIds.toSet()) {
                    val shuffledIds = fingerIds.shuffled()
                    val newTeams = mutableMapOf<PointerId, Int>()
                    shuffledIds.forEachIndexed { idx, id ->
                        newTeams[id] = (idx / selectedSize)
                    }
                    teams = newTeams
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                }
            } else {
                // Espera medio segundo antes de mostrar el error
                showError = false
                delay(500)
                // Si la situación sigue siendo inválida, muestra el error
                if (
                    fingerPositions.size == count &&
                    fingerPositions.keys.toSet() == fingerIds.toSet() &&
                    count % selectedSize != 0
                ) {
                    showError = true
                }
                teams = emptyMap()
            }
        } else {
            showError = false
            teams = emptyMap()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Selector de grupos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Tamaño de equipo",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                (2..5).forEach { size ->
                    Button(
                        onClick = { selectedSize = size
                                  haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)},
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSize == size) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedSize == size) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(size.toString())
                    }
                    if (size != 5) Spacer(Modifier.width(8.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(selectedSize) {
                        awaitEachGesture {
                            val pointers = mutableMapOf<PointerId, Offset>()
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { change ->
                                    if (change.pressed) {
                                        pointers[change.id] = change.position
                                    } else {
                                        pointers.remove(change.id)
                                    }
                                }
                                fingerPositions = pointers.toMap()
                                if (pointers.isEmpty()) break
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Dibuja los círculos de colores debajo de cada dedo
                Canvas(modifier = Modifier.fillMaxSize()) {
                    fingerPositions.forEach { (id, pos) ->
                        val team = teams[id]
                        val color = if (team != null) teamColors[team % teamColors.size] else Color.Gray
                        drawCircle(
                            color = color,
                            radius = 140f,
                            center = pos
                        )

                    }
                }
                if (fingerPositions.isEmpty()) {
                    Text(
                        text = "Pon aquí los dedos para formar equipos",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (showError) {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x55FF0000))
                    )
                }
            }
        }
    }
}