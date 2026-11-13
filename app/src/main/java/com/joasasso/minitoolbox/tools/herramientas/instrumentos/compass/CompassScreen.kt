package com.joasasso.minitoolbox.tools.herramientas.instrumentos.compass

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.GpsNotFixed
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.material.color.MaterialColors
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.ToolOnboardingKeys
import com.joasasso.minitoolbox.ui.components.ToolOnboardingBanner
import com.joasasso.minitoolbox.ui.components.ToolOnboardingCard
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Opacidad de la lectura cuando no es confiable (sin datos o teléfono inclinado). */
private const val DIMMED_ALPHA = 0.38f

private val DirectionNames = listOf(
    R.string.compass_name_n, R.string.compass_name_ne, R.string.compass_name_e, R.string.compass_name_se,
    R.string.compass_name_s, R.string.compass_name_sw, R.string.compass_name_w, R.string.compass_name_nw
)

@Composable
fun BrujulaScreen(onBack: () -> Unit, viewModel: CompassViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showInfo by rememberSaveable { mutableStateOf(false) }
    var calibrationDismissed by rememberSaveable { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    // Si la precisión se recupera, el aviso vuelve a estar disponible para la próxima caída.
    LaunchedEffect(state.needsCalibration) {
        if (!state.needsCalibration) calibrationDismissed = false
    }

    val dialHeading by animateFloatAsState(
        targetValue = state.continuousHeading,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "compassRotation"
    )

    TickHaptics(headingDeg = dialHeading)

    Scaffold(
        topBar = {
            TopBarReusable(
                title = stringResource(R.string.tool_compass),
                onBack = onBack,
                onShowInfo = { showInfo = true }
            )
        }
    ) { padding ->
        if (!state.isSupported) {
            UnsupportedContent(Modifier.padding(padding))
        } else BoxWithConstraints(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            val isLandscape = maxWidth > maxHeight
            val dial: @Composable (Modifier) -> Unit = { modifier ->
                BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
                    CompassDial(
                        state = state,
                        dialHeading = dialHeading,
                        diameter = min(maxWidth, maxHeight) * 0.92f
                    )
                }
            }
            val info: @Composable ColumnScope.() -> Unit = {
                ToolOnboardingCard(
                    toolKey = ToolOnboardingKeys.COMPASS,
                    message = stringResource(R.string.compass_onboarding_hint),
                    modifier = Modifier.fillMaxWidth()
                )
                ToolOnboardingBanner(
                    visible = state.needsCalibration && !calibrationDismissed,
                    message = stringResource(R.string.compass_calibration_message),
                    onDismiss = { calibrationDismissed = true },
                    icon = Icons.Outlined.ScreenRotation,
                    modifier = Modifier.fillMaxWidth()
                )
                AccuracyChip(
                    accuracy = state.accuracy,
                    onClick = { showInfo = true }
                )
                TiltWarning(visible = state.isTilted)
            }

            if (isLandscape) {
                Row(Modifier.fillMaxSize()) {
                    dial(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        content = info
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    info()
                    dial(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.compass_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.compass_help_line1))
                    Text(stringResource(R.string.compass_help_line2))
                    Text(stringResource(R.string.compass_help_line3))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showInfo = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

// --- Componentes --------------------------------------------------------------

@Composable
private fun UnsupportedContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Explore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = stringResource(R.string.compass_unsupported),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.compass_no_magnetometer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AccuracyChip(accuracy: CompassAccuracy, onClick: () -> Unit) {
    // Sin dato todavía: no se muestra un estado inventado.
    if (accuracy == CompassAccuracy.Unknown) return
    val isGood = accuracy == CompassAccuracy.High || accuracy == CompassAccuracy.Medium
    val label = when (accuracy) {
        CompassAccuracy.High -> R.string.compass_accuracy_high
        CompassAccuracy.Medium -> R.string.compass_accuracy_medium
        CompassAccuracy.Low -> R.string.compass_accuracy_low
        else -> R.string.compass_accuracy_unreliable
    }
    val iconTint = if (isGood) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    AssistChip(
        onClick = onClick,
        label = { Text(stringResource(label)) },
        leadingIcon = {
            Icon(
                imageVector = if (isGood) Icons.Outlined.GpsFixed else Icons.Outlined.GpsNotFixed,
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
        },
        colors = AssistChipDefaults.assistChipColors(leadingIconContentColor = iconTint)
    )
}

@Composable
private fun TiltWarning(visible: Boolean) {
    // Altura reservada para que el dial no salte cuando el aviso aparece y desaparece.
    Box(Modifier.heightIn(min = 24.dp), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            Text(
                text = stringResource(R.string.compass_tilt_warning),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Rojo del norte armonizado con el `primary` del tema, igual que los colores de categoría:
 * conserva la convención del norte en rojo sin romper el contraste en modo oscuro.
 */
@Composable
private fun rememberNorthColor(): Color {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.surface.luminance() > 0.5f
    val primary = scheme.primary
    return remember(primary, isLight) {
        val harmonized = MaterialColors.harmonize(NORTH_SEED.toArgb(), primary.toArgb())
        Color(MaterialColors.getColorRoles(harmonized, isLight).accent)
    }
}

private val NORTH_SEED = Color(0xFFE53935)

@Composable
private fun CompassDial(
    state: CompassUiState,
    dialHeading: Float,
    diameter: Dp
) {
    val colors = MaterialTheme.colorScheme
    val northColor = rememberNorthColor()
    val density = LocalDensity.current
    val ringRotation = -dialHeading // el anillo gira opuesto al rumbo

    val headingInt = state.heading.roundToInt() % 360
    val directionName = stringResource(DirectionNames[state.directionIndex])
    val description = if (state.hasReading) {
        pluralStringResource(R.plurals.compass_dial_description, headingInt, headingInt, directionName)
    } else {
        stringResource(R.string.tool_compass)
    }
    val readoutAlpha by animateFloatAsState(
        targetValue = if (!state.hasReading || state.isTilted) DIMMED_ALPHA else 1f,
        label = "readoutAlpha"
    )
    val bubbleColor by animateColorAsState(
        targetValue = if (state.isTilted) colors.error else colors.primary,
        label = "bubbleColor"
    )

    val cardinals = listOf(
        stringResource(R.string.compass_north_abbr),
        stringResource(R.string.compass_east_abbr),
        stringResource(R.string.compass_south_abbr),
        stringResource(R.string.compass_west_abbr)
    )

    Box(
        modifier = Modifier
            .size(diameter)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseR = size.minDimension / 2f

            fun polar(radius: Float, angleDeg: Float): Offset {
                val rad = Math.toRadians((angleDeg - 90f).toDouble())
                return Offset(
                    center.x + radius * cos(rad).toFloat(),
                    center.y + radius * sin(rad).toFloat()
                )
            }

            // Fondo tonal del dial.
            drawCircle(color = colors.surfaceContainerHigh, radius = baseR, center = center)

            // Marcas cada 5° (mayores cada 30°), giran con el rumbo.
            for (deg in 0 until 360 step 5) {
                val isMajor = deg % 30 == 0
                val outer = baseR * 0.92f
                val inner = outer - if (isMajor) baseR * 0.12f else baseR * 0.06f
                val color = when {
                    deg == 0 -> northColor
                    isMajor -> colors.onSurface
                    else -> colors.onSurfaceVariant.copy(alpha = 0.6f)
                }
                drawLine(
                    color = color,
                    start = polar(outer, deg + ringRotation),
                    end = polar(inner, deg + ringRotation),
                    strokeWidth = if (deg == 0) baseR * 0.03f else if (isMajor) baseR * 0.018f else baseR * 0.008f,
                    cap = StrokeCap.Round
                )
            }

            // Nivel: aro guía y burbuja que se desplaza hacia el borde levantado.
            val levelR = baseR * 0.36f
            drawCircle(
                color = colors.outlineVariant,
                radius = levelR,
                center = center,
                style = Stroke(width = baseR * 0.008f)
            )
            drawCircle(
                color = bubbleColor.copy(alpha = 0.28f),
                radius = baseR * 0.07f,
                center = Offset(
                    center.x + state.bubble.x * levelR,
                    center.y + state.bubble.y * levelR
                )
            )

            // Línea de fe fija: marca hacia dónde apunta el teléfono.
            drawLine(
                color = colors.primary,
                start = Offset(center.x, center.y - baseR * 0.98f),
                end = Offset(center.x, center.y - baseR * 0.74f),
                strokeWidth = baseR * 0.035f,
                cap = StrokeCap.Round
            )
        }

        // Etiquetas cada 30°: puntos cardinales y grados. Se trasladan con el anillo y
        // quedan siempre derechas para leerse sin girar la cabeza.
        val labelRadiusPx = with(density) { diameter.toPx() / 2f } * 0.66f
        for (deg in 0 until 360 step 30) {
            val rad = Math.toRadians((deg + ringRotation - 90f).toDouble())
            val cardinal = if (deg % 90 == 0) cardinals[deg / 90] else null
            Text(
                text = cardinal ?: deg.toString(),
                style = if (cardinal != null) {
                    MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                } else {
                    MaterialTheme.typography.labelMedium
                },
                color = when {
                    deg == 0 -> northColor
                    cardinal != null -> colors.onSurface
                    else -> colors.onSurfaceVariant
                },
                modifier = Modifier.graphicsLayer {
                    translationX = (labelRadiusPx * cos(rad)).toFloat()
                    translationY = (labelRadiusPx * sin(rad)).toFloat()
                }
            )
        }

        // Lectura central: rumbo en grande y nombre del punto cardinal.
        Column(
            modifier = Modifier.alpha(readoutAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (state.hasReading) "$headingInt°" else "—°",
                style = if (diameter < 300.dp) {
                    MaterialTheme.typography.displayMedium
                } else {
                    MaterialTheme.typography.displayLarge
                },
                color = colors.onSurface
            )
            Text(
                text = if (state.hasReading) directionName else "",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TickHaptics(
    headingDeg: Float,
    majorStepDeg: Float = 30f,
    epsilonDeg: Float = 3f,
    cooldownMs: Long = 350L
) {
    val haptic = LocalHapticFeedback.current
    val totalTicks = (360f / majorStepDeg).roundToInt()
    var lastIdx by remember { mutableStateOf<Int?>(null) }
    var lastTime by remember { mutableLongStateOf(0L) }
    var wasInside by remember { mutableStateOf(false) }

    LaunchedEffect(headingDeg) {
        val a = CompassMath.normalize(headingDeg)
        val idx = ((a / majorStepDeg).roundToInt() % totalTicks + totalTicks) % totalTicks
        val inside = abs(CompassMath.signedDelta(a, idx * majorStepDeg)) <= epsilonDeg
        val now = SystemClock.elapsedRealtime()

        if (inside && (!wasInside || idx != lastIdx) && (now - lastTime) >= cooldownMs) {
            val type = if (idx == 0) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
            haptic.performHapticFeedback(type)
            lastTime = now
            lastIdx = idx
        }
        wasInside = inside
    }
}
