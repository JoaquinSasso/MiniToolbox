// app/src/main/java/com/example/minitoolbox/tools/truco/TrucoScoreBoardScreen.kt
package com.joasasso.minitoolbox.tools.entretenimiento

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.tools.data.ScoreRepository
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Suppress("UnusedBoxWithConstraintsScope")
@Composable
fun PointCounter(
    points: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val fullSquares = points / 5
    val remainder = points % 5
    val totalSquares = fullSquares + if (remainder > 0) 1 else 0
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant

    BoxWithConstraints(modifier = modifier) {
        val squareSize = (maxHeight / 7)

        // ✅ uso directo de maxHeight en height modifier
        Column(
            modifier = Modifier
                .height(maxHeight) // ⚠️ uso directo dentro de Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            repeat(fullSquares) {
                Square(color = color, points = 5, squareSize = squareSize)
            }
            if (remainder > 0) {
                Square(color = color, points = remainder, squareSize = squareSize)
            }
            repeat(6 - totalSquares) {
                Spacer(modifier = Modifier.size(squareSize))
            }
        }

        // Línea divisoria usando squareSize (derivado de maxHeight)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val y = (squareSize * 3).toPx()
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}



@Composable
fun Square(color: Color, points: Int, squareSize: Dp) {
    Box(
        modifier = Modifier
            .size(squareSize)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val padding = 6.dp.toPx()
            val startX = padding
            val startY = padding
            val endX = size.width - padding
            val endY = size.height - padding

            val lines = listOf(
                Offset(startX, startY) to Offset(endX, startY),
                Offset(endX, startY) to Offset(endX, endY),
                Offset(endX, endY) to Offset(startX, endY),
                Offset(startX, endY) to Offset(startX, startY)
            )

            for (i in 0 until points.coerceAtMost(4)) {
                val (start, end) = lines[i]
                drawLine(color = color, start = start, end = end, strokeWidth = 6.dp.toPx(),  cap = StrokeCap.Round)
            }

            if (points == 5) {
                drawLine(
                    color = color,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 6.dp.toPx(),
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

    var showInfo by remember { mutableStateOf(false) }

    val ourPointsFlow by scoreRepo.ourPointsFlow.collectAsState(initial = 0)
    val theirPointsFlow by scoreRepo.theirPointsFlow.collectAsState(initial = 0)

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
        topBar = { TopBarReusable("Anotador de truco", onBack, { showInfo = true }) },
        floatingActionButton = {
            Button(onClick = {
                scope.launch {
                    val confirm = confirmResetDialog(context, haptic)
                    if (confirm) { resetPoints() }
                }
            }) {
                Icon(modifier = Modifier.padding(8.dp) ,
                    imageVector = Icons.Default.Refresh, contentDescription = "Reiniciar")
            }
        },
        content = { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val width = size.width
                            if (offset.x < width / 2) {
                                addPoints("our", 1)
                            } else {
                                addPoints("their", 1)
                            }
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botones izquierda (Nuestras)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = { addPoints("our", 1) }) {
                        Icon(Icons.Default.Add, contentDescription = "+1 Nuestras")
                    }
                    Spacer(Modifier.height(8.dp))
                    IconButton(onClick = { addPoints("our", -1) }) {
                        Icon(Icons.Default.Remove, contentDescription = "-1 Nuestras")
                    }
                }
                Row(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight()
                        .padding(top = 8.dp)
                ){
                    Column(
                        modifier = Modifier.weight(2f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Nuestras", fontWeight = FontWeight.Bold)
                        PointCounter(points = ourPoints, color = Color(0xFF2196F3))
                }
                    Column(
                        modifier = Modifier.weight(2f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ){
                        Text("Suyas", fontWeight = FontWeight.Bold)
                        PointCounter(points = theirPoints, color = Color(0xFFF44336))
                    }
                }

                // Botones derecha (Suyas)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = { addPoints("their", 1) }) {
                        Icon(Icons.Default.Add, contentDescription = "+1 Suyas")
                    }
                    Spacer(Modifier.height(8.dp))
                    IconButton(onClick = { addPoints("their", -1) }) {
                        Icon(Icons.Default.Remove, contentDescription = "-1 Suyas")
                    }
                }
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
                    Text("   – Usa los botones +/– en los costados para sumar/restar puntos.")
                    Text("   – Toca en mitad izquierda o derecha de la pantalla para sumar rápido.")
                    Text("   – Presiona el boton abajo a la izquierda para empezar una nueva partida.")
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

suspend fun confirmResetDialog(
    context: Context,
    haptic: HapticFeedback
): Boolean = suspendCancellableCoroutine { cont ->
    val dialog = AlertDialog.Builder(context)
        .setTitle("Empezar nueva partida")
        .setMessage("¿Estás seguro de que quieres comenzar una nueva partida?\nEsto borrará los puntos actuales.")
        .setPositiveButton("Sí") { _, _ ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            cont.resume(true)
        }
        .setNegativeButton("No") { _, _ ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            cont.resume(false)
        }
        .create()

    dialog.show()
}

