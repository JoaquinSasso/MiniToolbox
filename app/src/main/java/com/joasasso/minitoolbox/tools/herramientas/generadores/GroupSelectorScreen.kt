package com.joasasso.minitoolbox.tools.herramientas.generadores

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private val TeamColors = listOf(
    Color(0xFF1E88E5), // Azul
    Color(0xFFE53935), // Rojo
    Color(0xFF43A047), // Verde
    Color(0xFFFB8C00), // Naranja
    Color(0xFF8E24AA), // Violeta
    Color(0xFF00ACC1), // Cian
    Color(0xFFFDD835), // Amarillo
    Color(0xFFD81B60), // Rosa
    Color(0xFF3949AB), // Índigo
    Color(0xFF7CB342)  // Verde claro
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSelectorScreen(
    onBack: () -> Unit,
    viewModel: GroupSelectorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()

    val currentIsFrozen by rememberUpdatedState(uiState.isFrozen)
    val currentOnReset by rememberUpdatedState { viewModel.reset() }

    var fingerPositions by remember { mutableStateOf<Map<Long, Offset>>(emptyMap()) }
    var holdProgress by remember { mutableFloatStateOf(0f) }

    val minRequired = GroupPartitionEngine.getMinRequired(uiState.mode, uiState.targetValue)
    val fingerCount = fingerPositions.size

    // Manejo de la cuenta regresiva de 1 segundo para fijar los grupos
    LaunchedEffect(fingerPositions.keys, uiState.isFrozen, uiState.mode, uiState.targetValue) {
        if (uiState.isFrozen) {
            holdProgress = 0f
            return@LaunchedEffect
        }

        if (fingerCount >= minRequired) {
            val durationMs = 1000L
            val startTime = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                holdProgress = progress
                if (progress >= 1f) break
                delay(16.milliseconds)
            }
            val touchPoints = fingerPositions.mapValues { TouchPoint(it.value.x, it.value.y) }
            val completed = viewModel.completeSelection(touchPoints)
            if (completed) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            holdProgress = 0f
        } else {
            holdProgress = 0f
        }
    }

    Scaffold(
        topBar = {
            TopBarReusable(
                title = stringResource(R.string.tool_team_generator),
                onBack = onBack,
                onShowInfo = { viewModel.setShowInfoDialog(true) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(color = MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            // Cabecera compacta con controles Material 3
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Fila 1: Selector de modo (N° de equipos vs Por integrantes)
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = uiState.mode == PartitionMode.BY_TEAM_COUNT,
                            onClick = {
                                viewModel.setMode(PartitionMode.BY_TEAM_COUNT)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            label = {
                                Text(
                                    text = stringResource(R.string.team_mode_count),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )
                        SegmentedButton(
                            selected = uiState.mode == PartitionMode.BY_TEAM_SIZE,
                            onClick = {
                                viewModel.setMode(PartitionMode.BY_TEAM_SIZE)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            label = {
                                Text(
                                    text = stringResource(R.string.team_mode_size),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )
                    }

                    // Fila 2: Selector de valor objetivo (2..5)
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        (2..5).forEachIndexed { index, value ->
                            SegmentedButton(
                                selected = uiState.targetValue == value,
                                onClick = {
                                    viewModel.setTargetValue(value)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = 4),
                                label = {
                                    Text(
                                        text = value.toString(),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Lienzo táctil maximizado que ocupa todo el espacio restante
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background)
                    .pointerInput(Unit) {
                        val tracker = TripleTapTracker()
                        val downTimes = mutableMapOf<Long, Long>()
                        val downPositions = mutableMapOf<Long, Offset>()
                        val pointers = mutableMapOf<Long, Offset>()

                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { change ->
                                    val id = change.id.value
                                    if (change.pressed) {
                                        pointers[id] = change.position
                                        if (!change.previousPressed) {
                                            downTimes[id] = change.uptimeMillis
                                            downPositions[id] = change.position
                                        }
                                    } else {
                                        pointers.remove(id)
                                        if (change.previousPressed) {
                                            val startTime = downTimes.remove(id)
                                            val startPos = downPositions.remove(id)
                                            if (startTime != null && startPos != null && currentIsFrozen) {
                                                val duration = change.uptimeMillis - startTime
                                                val dx = change.position.x - startPos.x
                                                val dy = change.position.y - startPos.y
                                                val distance = kotlin.math.sqrt(dx * dx + dy * dy)
                                                if (duration < 400L && distance < 60f) {
                                                    val triggered = tracker.registerTap(change.uptimeMillis)
                                                    if (triggered) {
                                                        currentOnReset()
                                                        tracker.reset()
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                fingerPositions = pointers.toMap()
                            }
                        }
                    }
            ) {
                // Dibujo de los dedos en el Canvas
                val primaryColor = MaterialTheme.colorScheme.primary
                val outlineVariant = MaterialTheme.colorScheme.outlineVariant
                val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (uiState.isFrozen) {
                        // Estado congelado: dibuja los círculos coloreados con el número de equipo
                        uiState.frozenPositions.forEach { (id, touchPoint) ->
                            val teamIdx = uiState.frozenAssignments[id] ?: 0
                            val color = TeamColors[teamIdx % TeamColors.size]
                            val center = Offset(touchPoint.x, touchPoint.y)

                            // Círculo principal del equipo
                            drawCircle(
                                color = color,
                                radius = 110f,
                                center = center
                            )
                            // Borde sutil de contraste
                            drawCircle(
                                color = Color.White.copy(alpha = 0.8f),
                                radius = 110f,
                                center = center,
                                style = Stroke(width = 6f)
                            )

                            // Número identificador de equipo dentro del círculo para máxima claridad
                            val teamLabel = (teamIdx + 1).toString()
                            val textLayout = textMeasurer.measure(
                                text = teamLabel,
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            )
                            drawText(
                                textLayoutResult = textLayout,
                                topLeft = Offset(
                                    center.x - textLayout.size.width / 2f,
                                    center.y - textLayout.size.height / 2f
                                )
                            )
                        }
                    } else {
                        // Estado en vivo mientras apoyan los dedos
                        fingerPositions.forEach { (_, pos) ->
                            // Relleno suave bajo el dedo
                            drawCircle(
                                color = surfaceVariant.copy(alpha = 0.5f),
                                radius = 95f,
                                center = pos
                            )

                            // Si la cuenta regresiva está corriendo, dibuja el aro animado de carga
                            if (holdProgress > 0f) {
                                drawArc(
                                    color = primaryColor,
                                    startAngle = -90f,
                                    sweepAngle = holdProgress * 360f,
                                    useCenter = false,
                                    topLeft = Offset(pos.x - 110f, pos.y - 110f),
                                    size = Size(220f, 220f),
                                    style = Stroke(width = 10f, cap = StrokeCap.Round)
                                )
                            } else {
                                drawCircle(
                                    color = outlineVariant,
                                    radius = 100f,
                                    center = pos,
                                    style = Stroke(width = 4f)
                                )
                            }
                        }
                    }
                }

                // Píldora flotante con estado y previsualización (no ocupa altura de layout)
                val statusText = when {
                    uiState.isFrozen -> stringResource(R.string.team_results_hint)
                    fingerCount == 0 -> stringResource(R.string.place_fingers)
                    fingerCount < minRequired -> stringResource(
                        R.string.team_min_fingers_required,
                        minRequired
                    )
                    else -> {
                        val predictedSizes = GroupPartitionEngine.calculateTeamSizes(
                            fingerCount,
                            uiState.mode,
                            uiState.targetValue
                        )
                        val summary = GroupPartitionEngine.summarizeTeamSizes(predictedSizes)
                        when (summary.size) {
                            1 -> stringResource(
                                R.string.team_group_summary_single,
                                summary[0].teamCount,
                                summary[0].memberCount
                            )
                            2 -> stringResource(
                                R.string.team_group_summary_double,
                                summary[0].teamCount,
                                summary[0].memberCount,
                                summary[1].teamCount,
                                summary[1].memberCount
                            )
                            else -> "${predictedSizes.size} equipos"
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp, start = 16.dp, end = 16.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                    tonalElevation = 3.dp,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Botón flotante para reiniciar el sorteo cuando está congelado
                if (uiState.isFrozen) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            viewModel.reset()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp),
                        icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        text = { Text(stringResource(R.string.team_new_draw)) }
                    )
                }
            }
        }
    }

    if (uiState.showInfoDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowInfoDialog(false) },
            title = { Text(stringResource(R.string.about_team_generator)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.team_generator_purpose))
                    Text(stringResource(R.string.team_help_guide))
                    Text(stringResource(R.string.team_generator_help_1))
                    Text(stringResource(R.string.team_generator_help_2))
                    Text(stringResource(R.string.team_generator_help_3))
                    Text(stringResource(R.string.team_generator_help_4))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setShowInfoDialog(false)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}
