package com.joasasso.minitoolbox.tools.herramientas.instrumentos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import io.github.sceneview.*
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.SphereNode
import java.util.Locale
import kotlin.math.floor

private enum class Units { METRIC, IMPERIAL; fun toggle() = if (this == METRIC) IMPERIAL else METRIC }

private object UnitConverter {
    fun format(meters: Double, units: Units): String = when (units) {
        Units.METRIC   -> if (meters >= 1.0) String.format(Locale.getDefault(), "%.2f m", meters)
        else               String.format(Locale.getDefault(), "%.1f cm", meters * 100.0)
        Units.IMPERIAL -> {
            val totalInches = meters / 0.0254
            val feet = floor(totalInches / 12.0).toInt()
            val inches = totalInches - feet * 12
            if (feet >= 1) String.format(Locale.getDefault(), "%d'%s\"", feet,
                String.format(Locale.getDefault(), "%.1f", inches))
            else            String.format(Locale.getDefault(), "%.1f in", inches)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARRulerScreen(onBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val engine = rememberEngine()

    val nodes = rememberNodes()
    val lineNode = remember { mutableStateOf<CylinderNode?>(null) }
    val mainLightNode = rememberMainLightNode()

    var distanceText by remember { mutableStateOf("") }
    var unitSystem by remember { mutableStateOf(Units.METRIC) }
    var isCalibrating by remember { mutableStateOf(false) }
    var knownCalibMeters by remember { mutableStateOf(0.0856) }
    var calibrationScale by remember { mutableStateOf(1.0) }
    var isHitValid by remember { mutableStateOf(false) }

    val blueMaterial = rememberMaterial {
        com.google.android.filament.utils.Color.BLUE
    }
    val whiteMaterial = rememberMaterial {
        com.google.android.filament.utils.Color.WHITE
    }

    fun resetAll() {
        nodes.clear()
        lineNode.value = null
        distanceText = ""
    }

    Scaffold(
        topBar = {
            TopBarReusable(title = stringResource(R.string.tool_ar_ruler), onBack = onBack)
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AssistChip(
                        onClick = {
                            resetAll()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        label = {
                            Text(stringResource(R.string.delete))
                        },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete)) }
                    )

                    AssistChip(
                        onClick = {
                            isCalibrating = true
                            knownCalibMeters = 0.098631 // Card diagonal
                            resetAll()
                        },
                        label = { Text("Calibrar tarjeta") },
                        leadingIcon = { Icon(Icons.Rounded.Straighten, contentDescription = null) }
                    )

                    if (isCalibrating) {
                        val step = if (nodes.filterIsInstance<ArAnchorNode>().size % 2 == 0) "1/2" else "2/2"
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            tonalElevation = 2.dp
                        ) {
                            Text(
                                text = step,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    AssistChip(
                        onClick = {
                            unitSystem = unitSystem.toggle()
                        },
                        label = {
                            Text(
                                if (unitSystem == Units.METRIC)
                                    stringResource(R.string.aruler_units_metric)
                                else stringResource(R.string.aruler_units_imperial)
                            )
                        },
                        leadingIcon = { Icon(Icons.Rounded.Straighten, null) }
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Is handled by ARScene onTap */ },
                containerColor = if (isHitValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isHitValid) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) { Icon(Icons.Filled.Add, contentDescription = "Agregar punto") }
        }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            ARScene(
                modifier = Modifier.fillMaxSize(),
                nodes = nodes + listOfNotNull(lineNode.value) + mainLightNode,
                planeRenderer = true,
                sessionConfiguration = { _, config ->
                    config.focusMode = Config.FocusMode.AUTO
                    config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                },
                onSessionUpdated = { _, frame ->
                    isHitValid = frame.hitTest(frame.camera.displayOrientedPose.tx, frame.camera.displayOrientedPose.ty)
                        .any { hitResult ->
                            hitResult.trackable is Plane &&
                                    (hitResult.trackable as Plane).isPoseInPolygon(hitResult.hitPose)
                        }
                },
                onTap = { hitResult ->
                    if (nodes.filterIsInstance<ArAnchorNode>().size >= 2) {
                        resetAll()
                    }

                    val anchorNode = ArAnchorNode(
                        engine = engine,
                        anchor = hitResult.createAnchor()
                    ).apply {
                        addChildNode(
                            SphereNode(
                                engine = engine,
                                radius = 0.01f,
                                material = blueMaterial
                            )
                        )
                    }
                    nodes.add(anchorNode)

                    val anchorNodes = nodes.filterIsInstance<ArAnchorNode>()
                    if (anchorNodes.size == 2) {
                        val startNode = anchorNodes[0]
                        val endNode = anchorNodes[1]
                        val distance = startNode.position.distance(endNode.position).toDouble()

                        if (isCalibrating) {
                            if (distance in 0.06..0.15) {
                                calibrationScale = (knownCalibMeters / distance)
                                    .coerceIn(0.7, 1.3)
                            }
                            isCalibrating = false
                            resetAll()
                        } else {
                            val calibratedDistance = distance * calibrationScale
                            distanceText = UnitConverter.format(calibratedDistance, unitSystem)

                            val line = CylinderNode(
                                engine = engine,
                                radius = 0.005f,
                                height = distance.toFloat(),
                                center = Position(y = distance.toFloat() / 2.0f),
                                material = whiteMaterial
                            )
                            line.position = Position.lerp(startNode.position, endNode.position, 0.5f)
                            line.rotation = Rotation.lookRotation(
                                direction = (endNode.position - startNode.position).normalized(),
                                up = Position(0f, 1f, 0f)
                            )
                            lineNode.value = line
                        }
                    }
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )

            if (distanceText.isNotEmpty()) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = distanceText,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }

    BackHandler { onBack() }
}
