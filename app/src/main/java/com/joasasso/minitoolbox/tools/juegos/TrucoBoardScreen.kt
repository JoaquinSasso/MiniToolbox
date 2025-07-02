// app/src/main/java/com/example/minitoolbox/tools/truco/TrucoScoreBoardScreen.kt
package com.joasasso.minitoolbox.tools.juegos

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.launch

@Composable
fun PointCounter(points: Int, color: Color) {
    val squares = points / 5
    val remaining = points % 5

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        for (i in 0 until squares) {
            Square(color = color, points = 5)
        }
        if (remaining > 0) {
            Square(color = color, points = remaining)
        }
    }
}

@Composable
fun Square(color: Color, points: Int) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .padding(10.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val squareSizePx = 60.dp.toPx()
            val lines = listOf(
                Offset(10f, 10f) to Offset(squareSizePx, 10f),
                Offset(squareSizePx, 10f) to Offset(squareSizePx, squareSizePx),
                Offset(squareSizePx, squareSizePx) to Offset(10f, squareSizePx),
                Offset(10f, squareSizePx) to Offset(10f, 10f)
            )
            for (i in 0 until points.coerceAtMost(4)) {
                val (start, end) = lines[i]
                drawLine(
                    color = color,
                    start = start,
                    end = end,
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            if (points == 5) {
                drawLine(
                    color = color,
                    start = Offset(10f, 10f),
                    end = Offset(size.width - 10f, size.height - 10f),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrucoScoreBoardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vibrator = context.getSystemService(Vibrator::class.java)
    val haptic = LocalHapticFeedback.current
    val scoreRepo = remember { ScoreRepository(context) }
    val scope = rememberCoroutineScope()

    // Estado para mostrar ventana de información
    var showInfo by remember { mutableStateOf(false) }

    // Flujos persistentes de puntos
    val ourPointsFlow by scoreRepo.ourPointsFlow.collectAsState(initial = 0)
    val theirPointsFlow by scoreRepo.theirPointsFlow.collectAsState(initial = 0)

    // Estado local sincronizado con DataStore
    var ourPoints by remember { mutableStateOf(ourPointsFlow) }
    var theirPoints by remember { mutableStateOf(theirPointsFlow) }
    LaunchedEffect(ourPointsFlow, theirPointsFlow) {
        ourPoints = ourPointsFlow
        theirPoints = theirPointsFlow
    }

    val maxPoints = 30

    fun addPoints(team: String, amount: Int) {
        if (team == "our") {
            ourPoints = (ourPoints + amount).coerceIn(0, maxPoints)
        } else {
            theirPoints = (theirPoints + amount).coerceIn(0, maxPoints)
        }
        vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        scope.launch { scoreRepo.savePoints(ourPoints, theirPoints) }
    }

    fun resetPoints() {
        ourPoints = 0
        theirPoints = 0
        vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        scope.launch { scoreRepo.savePoints(0, 0) }
    }

    Scaffold(
        topBar = {TopBarReusable("Anotador de truco", onBack, {showInfo = true})},
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            if (offset.x < size.width / 2) addPoints("our", 1)
                            else                              addPoints("their", 1)
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(0.5f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("Nuestras", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        PointCounter(points = ourPoints, color = Color(0xFF2196F3))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(0.5f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Suyas", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        PointCounter(points = theirPoints, color = Color(0xFFF44336))
                    }
                }
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .absoluteOffset(y = 272.dp),
                    thickness = 3.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    IconButton(onClick = { addPoints("our", 1) }) {
                        Icon(Icons.Default.Add, contentDescription = "+1")
                    }
                    IconButton(onClick = { addPoints("their", 1) }) {
                        Icon(Icons.Default.Add, contentDescription = "+1")
                    }
                }
                Spacer(Modifier.height(5.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    IconButton(onClick = { addPoints("our", -1) }) {
                        Icon(Icons.Default.Remove, contentDescription = "-1")
                    }
                    IconButton(onClick = { addPoints("their", -1) }) {
                        Icon(Icons.Default.Remove, contentDescription = "-1")
                    }
                }
                Spacer(Modifier.height(15.dp))
                Button(
                    onClick = { resetPoints() },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reiniciar")
                    Spacer(Modifier.width(8.dp))
                    Text("Reiniciar")
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    )

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Acerca de Anotador de Truco") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Para qué sirve: Te ayuda a llevar la cuenta de los puntos de ambos equipos en una partida de Truco.")
                    Text("• Guía rápida:")
                    Text("   – Toca en el lado izquierdo de la pantalla para sumar un punto a “Nuestras”.")
                    Text("   – Toca en el lado derecho para sumar un punto a “Suyas”.")
                    Text("   – Usa los botones +/– en la parte inferior para ajustar manualmente.")
                    Text("   – Presiona “Reiniciar” para empezar una nueva partida.")
                    Text("   – La partida se guarda automáticamente si sales y regresas más tarde.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showInfo = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
