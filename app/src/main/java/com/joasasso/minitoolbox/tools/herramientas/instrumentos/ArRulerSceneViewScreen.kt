package com.joasasso.minitoolbox.tools.herramientas.instrumentos

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import io.github.sceneview.ar.ARScene
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

/* ───────────── Unidades ───────────── */

private enum class Units { METRIC, IMPERIAL; fun toggle() = if (this == METRIC) IMPERIAL else METRIC }

private object UnitConverter {
    fun format(meters: Double, units: Units): String = when (units) {
        Units.METRIC ->
            if (meters >= 1.0) String.format("%.2f m", meters)
            else String.format("%.1f cm", meters * 100.0)
        Units.IMPERIAL -> {
            val totalIn = meters / 0.0254
            val ft = floor(totalIn / 12.0).toInt()
            val inch = totalIn - ft * 12.0
            if (ft >= 1) String.format("%d'%s\"", ft, String.format("%.1f", inch))
            else String.format("%.1f in", inch)
        }
    }
}

/* ───────────── Modelo simple ───────────── */

private data class Measurement(val id: Long, val start: Anchor, val end: Anchor, val metersRaw: Double)

private class ARulerVM {
    var unitSystem by mutableStateOf(Units.METRIC);        private set
    var pendingStartAnchor: Anchor? by mutableStateOf(null)
    var measurements by mutableStateOf(listOf<Measurement>())

    private var counter = 0L
    fun nextId() = ++counter
    fun add(m: Measurement) { measurements = measurements + m }
    fun resetAll() {
        measurements.forEach { it.start.detach(); it.end.detach() }
        pendingStartAnchor?.detach(); pendingStartAnchor = null
        measurements = emptyList()
    }
    fun toggleUnits() { unitSystem = unitSystem.toggle() }

    // Calibración por diagonal de tarjeta (~9.8631 cm)
    var isCalibrating by mutableStateOf(false);     private set
    var knownMeters by mutableStateOf(0.098631)
    var scale by mutableStateOf(1.0)                // 1.0 = sin corrección
        private set

    fun startCardCalibration() {
        isCalibrating = true
        knownMeters = 0.098631
        pendingStartAnchor?.detach(); pendingStartAnchor = null
    }
    fun cancelCalibration() { isCalibrating = false }
    fun finishCalibration(measuredMeters: Double) {
        if (measuredMeters in 0.06..0.15) {
            scale = (knownMeters / measuredMeters).coerceIn(0.7, 1.3)
        }
        isCalibrating = false
    }

    fun formatCorrected(metersRaw: Double): String =
        UnitConverter.format(metersRaw * scale, unitSystem)
}

/* ───────────── Pantalla ───────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArRulerSceneViewScreen(onBack: () -> Unit) {
    val vm = remember { ARulerVM() }
    val haptic = LocalHapticFeedback.current

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)

    // Estado visual / frame
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var currentFrame by remember { mutableStateOf<Frame?>(null) }
    var centerHitValid by remember { mutableStateOf(false) }

    // Medición activa (puntos “en construcción”)
    var firstAnchor by remember { mutableStateOf<Anchor?>(null) }
    var secondAnchor by remember { mutableStateOf<Anchor?>(null) }

    // Colores (NO blancos): activos vs historial
    val activeColor = Color(0xFFFFC107)   // ámbar (alta visibilidad)
    val historyColor = Color(0xFF40C4FF)  // celeste (buena separación)

    Scaffold(
        topBar = { TopBarReusable(title = stringResource(R.string.tool_ar_ruler), onBack = onBack) },
        bottomBar = {
            BottomAppBar {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    AssistChip(
                        onClick = {
                            // limpiar todas las mediciones
                            firstAnchor?.detach(); secondAnchor?.detach()
                            firstAnchor = null; secondAnchor = null
                            vm.resetAll()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        label = { Text(stringResource(R.string.delete)) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete)) }
                    )

                    AssistChip(
                        onClick = { vm.startCardCalibration() },
                        label = { Text(stringResource(R.string.aruler_calibration_button)) },
                        leadingIcon = { Icon(Icons.Rounded.Straighten, contentDescription = null) }
                    )

                    if (vm.isCalibrating) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            tonalElevation = 2.dp
                        ) {
                            val step = if (vm.pendingStartAnchor == null) "1/2" else "2/2"
                            Text(step, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }

                    AssistChip(
                        onClick = { vm.toggleUnits() },
                        label = { Text(if (vm.unitSystem == Units.METRIC) stringResource(R.string.aruler_units_metric) else stringResource(R.string.aruler_units_imperial)) },
                        leadingIcon = { Icon(Icons.Rounded.Straighten, contentDescription = null) }
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            val container = if (centerHitValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            val content   = if (centerHitValid) MaterialTheme.colorScheme.onPrimary  else MaterialTheme.colorScheme.onSurfaceVariant
            FloatingActionButton(
                onClick = {
                    val fr = currentFrame ?: return@FloatingActionButton
                    val hit = centerHit(fr, viewport) ?: return@FloatingActionButton
                    val anchor = hit.createAnchor()

                    if (vm.isCalibrating) {
                        if (vm.pendingStartAnchor == null) {
                            vm.pendingStartAnchor = anchor
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        } else {
                            val start = vm.pendingStartAnchor!!
                            val m = distanceMeters(start.pose, anchor.pose).toDouble()
                            vm.finishCalibration(m)
                            start.detach()
                            vm.pendingStartAnchor = null
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        return@FloatingActionButton
                    }

                    // Flujo normal (NO borramos mediciones anteriores)
                    if (firstAnchor == null) {
                        firstAnchor = anchor
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } else if (secondAnchor == null) {
                        secondAnchor = anchor
                        val raw = distanceMeters(firstAnchor!!.pose, secondAnchor!!.pose).toDouble()
                        vm.add(Measurement(vm.nextId(), firstAnchor!!, secondAnchor!!, raw))
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else {
                        // comenzar nueva medición sin eliminar la anterior (queda en historial + overlay)
                        firstAnchor = anchor
                        secondAnchor = null
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                },
                containerColor = container,
                contentColor = content,
                shape = CircleShape
            ) { Text("+", fontSize = 22.sp) }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding)
                .onSizeChanged { viewport = it }
        ) {
            // --- Cámara/Tracking AR ---
            ARScene(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                modelLoader = modelLoader,
                onSessionUpdated = { _, frame ->
                    currentFrame = frame
                    centerHitValid = centerHit(frame, viewport) != null
                }
            )

            // --- Retícula central (verde si hay hit válido) ---
            CrosshairOverlay(isValid = centerHitValid)

            // --- Overlay 2D: historial de mediciones (líneas, puntos y etiquetas) ---
            MeasurementsOverlay(
                frame = currentFrame,
                viewport = viewport,
                items = vm.measurements,
                label = { vm.formatCorrected(it.metersRaw) },
                color = historyColor
            )

            // --- Overlay 2D: medición activa (si hay 1 o 2 puntos) ---
            ActiveMeasureOverlay(
                frame = currentFrame,
                viewport = viewport,
                a = firstAnchor,
                b = secondAnchor,
                label = {
                    val m = (if (firstAnchor != null && secondAnchor != null)
                        distanceMeters(firstAnchor!!.pose, secondAnchor!!.pose).toDouble()
                    else null)
                    m?.let(vm::formatCorrected)
                },
                color = activeColor
            )

            // --- Tips dinámicos ---
            AnimatedVisibility(
                visible = vm.isCalibrating || (vm.pendingStartAnchor == null && vm.measurements.isEmpty() && firstAnchor == null),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp, start = 12.dp, end = 12.dp)
            ) {
                val isStep1 = vm.isCalibrating && vm.pendingStartAnchor == null
                val isStep2 = vm.isCalibrating && vm.pendingStartAnchor != null
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    tonalElevation = 3.dp,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when {
                                isStep1 -> stringResource(R.string.aruler_tip_calib_step1)
                                isStep2 -> stringResource(R.string.aruler_tip_calib_step2)
                                else    -> stringResource(R.string.aruler_tip_scan)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (vm.isCalibrating) {
                            TextButton(onClick = { vm.cancelCalibration() }) { Text(stringResource(R.string.cancel)) }
                        }
                    }
                }
            }

            // --- Historial (lista, igual que antes) ---
            if (vm.measurements.isNotEmpty()) {
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    tonalElevation = 2.dp
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.aruler_history), style = MaterialTheme.typography.labelLarge)
                        vm.measurements.takeLast(5).reversed().forEach { m ->
                            Text("• " + vm.formatCorrected(m.metersRaw))
                        }
                    }
                }
            }
        }
    }

    BackHandler { onBack() }
}

/* ───────────── Overlays 2D ───────────── */

@Composable
private fun MeasurementsOverlay(
    frame: Frame?,
    viewport: IntSize,
    items: List<Measurement>,
    label: (Measurement) -> String,
    color: Color
) {
    if (frame == null || viewport == IntSize.Zero || items.isEmpty()) return
    // Dibujo de líneas + puntos (primero la geometría)
    Canvas(Modifier.fillMaxSize()) {
        items.forEach { m ->
            val pa = projectWorldToScreen(frame, m.start.pose.tx(), m.start.pose.ty(), m.start.pose.tz(), viewport)
            val pb = projectWorldToScreen(frame, m.end.pose.tx(), m.end.pose.ty(), m.end.pose.tz(), viewport)
            if (pa != null && pb != null) {
                drawLine(color, pa, pb, strokeWidth = 4f)
                drawCircle(color, radius = 7f, center = pa)
                drawCircle(color, radius = 7f, center = pb)
            }
        }
    }
    // Etiquetas (encima, para legibilidad)
    items.forEach { m ->
        val pa = projectWorldToScreen(frame, m.start.pose.tx(), m.start.pose.ty(), m.start.pose.tz(), viewport)
        val pb = projectWorldToScreen(frame, m.end.pose.tx(), m.end.pose.ty(), m.end.pose.tz(), viewport)
        if (pa != null && pb != null) {
            val mid = Offset((pa.x + pb.x) / 2f, (pa.y + pb.y) / 2f)
            Box(
                modifier = Modifier
                    .offset { IntOffset(mid.x.roundToInt(), (mid.y - 24f).roundToInt()) }
                    .clip(CircleShape)
                    .background(Color(0xCC000000))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(label(m), color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ActiveMeasureOverlay(
    frame: Frame?,
    viewport: IntSize,
    a: Anchor?,
    b: Anchor?,
    label: () -> String?,
    color: Color
) {
    if (frame == null || viewport == IntSize.Zero) return
    val pa = a?.let { projectWorldToScreen(frame, it.pose.tx(), it.pose.ty(), it.pose.tz(), viewport) }
    val pb = b?.let { projectWorldToScreen(frame, it.pose.tx(), it.pose.ty(), it.pose.tz(), viewport) }

    Canvas(Modifier.fillMaxSize()) {
        if (pa != null) drawCircle(color, radius = 8f, center = pa)
        if (pb != null) drawCircle(color, radius = 8f, center = pb)
        if (pa != null && pb != null) drawLine(color, pa, pb, strokeWidth = 5f)
    }
    if (pa != null && pb != null) {
        val mid = Offset((pa.x + pb.x) / 2f, (pa.y + pb.y) / 2f)
        label()?.let { text ->
            Box(
                modifier = Modifier
                    .offset { IntOffset(mid.x.roundToInt(), (mid.y - 28f).roundToInt()) }
                    .clip(CircleShape)
                    .background(Color(0xF0000000))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(text, color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

/* ───────────── Retícula ───────────── */

@Composable
private fun CrosshairOverlay(isValid: Boolean) {
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val len = size.minDimension * 0.04f
            val color = if (isValid) Color(0xFF4CAF50) else Color(0xFFB0B0B0)
            drawLine(color, Offset(cx - len, cy), Offset(cx + len, cy), 4f)
            drawLine(color, Offset(cx, cy - len), Offset(cx, cy + len), 4f)
            drawCircle(color, 6f, Offset(cx, cy))
        }
    }
}

/* ───────────── AR helpers ───────────── */

private fun centerHit(frame: Frame?, viewport: IntSize): com.google.ar.core.HitResult? {
    if (frame == null || viewport == IntSize.Zero) return null
    if (frame.camera.trackingState != com.google.ar.core.TrackingState.TRACKING) return null
    val x = viewport.width / 2f
    val y = viewport.height / 2f
    val hits = frame.hitTest(x, y)
    return hits.firstOrNull { hit ->
        val t = hit.trackable
        (t is Plane && t.isPoseInPolygon(hit.hitPose)) ||
                (t is Point && t.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)
    }
}

private fun distanceMeters(p1: Pose, p2: Pose): Float {
    val dx = p2.tx() - p1.tx(); val dy = p2.ty() - p1.ty(); val dz = p2.tz() - p1.tz()
    return sqrt(dx * dx + dy * dy + dz * dz)
}

private fun projectWorldToScreen(
    frame: Frame,
    wx: Float, wy: Float, wz: Float,
    viewport: IntSize
): Offset? {
    if (viewport == IntSize.Zero) return null

    val proj = FloatArray(16)
    val view = FloatArray(16)
    frame.camera.getProjectionMatrix(proj, 0, 0.01f, 100f)
    frame.camera.getViewMatrix(view, 0)

    val vx = view[0] * wx + view[4] * wy + view[8] * wz + view[12]
    val vy = view[1] * wx + view[5] * wy + view[9] * wz + view[13]
    val vz = view[2] * wx + view[6] * wy + view[10] * wz + view[14]
    val vw = view[3] * wx + view[7] * wy + view[11] * wz + view[15]

    val cx = proj[0] * vx + proj[4] * vy + proj[8] * vz + proj[12] * vw
    val cy = proj[1] * vx + proj[5] * vy + proj[9] * vz + proj[13] * vw
    val cz = proj[2] * vx + proj[6] * vy + proj[10] * vz + proj[14] * vw
    val cw = proj[3] * vx + proj[7] * vy + proj[11] * vz + proj[15] * vw

    if (cw == 0f || cz > cw) return null
    val ndcX = cx / cw
    val ndcY = cy / cw
    val sx = (ndcX * 0.5f + 0.5f) * viewport.width
    val sy = (1f - (ndcY * 0.5f + 0.5f)) * viewport.height
    return Offset(sx, sy)
}

/* util pequeño */
private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("Context no es una Activity")
}
