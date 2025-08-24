package com.joasasso.minitoolbox.tools.herramientas.instrumentos

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnAttach
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Pose
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Camera
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.SceneView
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import java.util.Locale
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import com.google.ar.sceneform.rendering.Color as SColor

/* ────────────────────────────── Unidades (único lugar) ───────────────────────────── */

private enum class Units { METRIC, IMPERIAL; fun toggle() = if (this == METRIC) IMPERIAL else METRIC }

/** Conversión/parseo/format común para historial y labels. */
private object UnitConverter {
    /** Devuelve el texto formateado en la unidad pedida a partir de metros. */
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

    /** Intenta parsear un label ("12.3 cm", "1.25 m", "72.0 in", "5'7.4\"") a metros. */
    fun parseMetersFromLabel(text: String): Double? {
        val s = text.trim()
            .replace('’', '\'').replace('′', '\'')
            .replace('“', '"').replace('”', '"')
            .replace(',', '.')
        val lower = s.lowercase(Locale.ROOT)

        Regex("""^\s*(\d+)\s*'\s*([\d.]+)\s*"\s*$""").matchEntire(lower)?.let {
            val feet = it.groupValues[1].toDoubleOrNull() ?: return null
            val inches = it.groupValues[2].toDoubleOrNull() ?: return null
            return (feet * 12.0 + inches) * 0.0254
        }
        Regex("""^\s*([\d.]+)\s*in\s*$""").matchEntire(lower)?.let {
            return (it.groupValues[1].toDoubleOrNull() ?: return null) * 0.0254
        }
        Regex("""^\s*([\d.]+)\s*m\s*$""").matchEntire(lower)?.let {
            return it.groupValues[1].toDoubleOrNull()
        }
        Regex("""^\s*([\d.]+)\s*cm\s*$""").matchEntire(lower)?.let {
            val v = it.groupValues[1].toDoubleOrNull() ?: return null
            return v / 100.0
        }
        return null
    }

    /** Convierte TODOS los labels visibles del AR scene al sistema indicado. */
    fun convertAllArLabels(sceneView: ArSceneView, to: Units) {
        traverseScene(sceneView.scene) { node ->
            val r = node.renderable as? ViewRenderable ?: return@traverseScene
            val tv = r.view.findViewById<TextView?>(R.id.labelText) ?: return@traverseScene
            val meters = parseMetersFromLabel(tv.text?.toString() ?: "") ?: return@traverseScene
            tv.text = format(meters, to)
        }
    }
}

/* ─────────────────────────────── ViewModel compacto ─────────────────────────────── */

private data class Measurement(
    val id: Long,
    val start: Anchor,
    val end: Anchor,
    val meters: Double
)

private class ARulerViewModel {
    var unitSystem by mutableStateOf(Units.METRIC);       private set
    var pendingStartAnchor: Anchor? by mutableStateOf(null)
    var measurements by mutableStateOf(listOf<Measurement>())
    private val cardDiagonal = 0.098631

    private var counter = 0L

    var showTips by mutableStateOf(false)
    fun nextId() = ++counter
    fun addMeasurement(m: Measurement) { measurements = measurements + m }
    fun resetAll() {
        measurements.forEach { it.start.detach(); it.end.detach() }
        pendingStartAnchor?.detach(); pendingStartAnchor = null
        measurements = emptyList()
    }
    fun toggleUnits() { unitSystem = unitSystem.toggle() }
    fun formatDistance(meters: Double): String = UnitConverter.format(meters, unitSystem)

    // Factor de calibración (1f = sin corrección)
    var calibrationScale by mutableFloatStateOf(1f)
        private set

    // Modo calibración
    var isCalibrating by mutableStateOf(false)
        private set
    var knownCalibMeters by mutableDoubleStateOf(0.0856) // por defecto, ANCHO de tarjeta (85.60 mm)

    fun cancelCalibration() { isCalibrating = false }

    /** Aplica factor al valor bruto en metros (para labels e historial). */
    fun applyCalibration(meters: Double): Double = meters * calibrationScale

    fun formatDistanceCorrected(metersRaw: Double): String =
        UnitConverter.format(applyCalibration(metersRaw), unitSystem)

    fun startCalibration(knownMeters: Double) {
        isCalibrating = true
        knownCalibMeters = knownMeters
        showTips = true
        pendingStartAnchor?.detach()
        pendingStartAnchor = null
    }

    // en ARulerViewModel
    fun startCardCalibration() {
        isCalibrating = true
        knownCalibMeters = cardDiagonal
        showTips = true
        pendingStartAnchor?.detach()
        pendingStartAnchor = null
    }

    // opcional: rangos más estrictos para diagonal (≈10 cm)
    fun finishCalibration(measuredMeters: Double) {
        // filtra medidas absurdas: entre 6 y 15 cm está OK para la diagonal
        if (measuredMeters in 0.06..0.15) {
            calibrationScale = (knownCalibMeters / measuredMeters)
                .toFloat()
                .coerceIn(0.7f, 1.3f) // rango más conservador
        }
        isCalibrating = false
    }


}

/* ───────────────────────────── UI principal (Compose) ───────────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARRulerScreen(onBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val vm = remember { ARulerViewModel() }

    //Escena de realidad aumentada
    var arFragment by remember { mutableStateOf<ArFragment?>(null) }
    var sceneView by remember { mutableStateOf<ArSceneView?>(null) }


    // Limpieza del fragment al salir de la pantalla (evita cámara negra al volver)
    val ctx = LocalContext.current
    val fragTag = "AR_FRAGMENT"
    DisposableEffect(Unit) {
        onDispose {
            val act = ctx.findActivity() as FragmentActivity
            act.supportFragmentManager.findFragmentByTag(fragTag)?.let {
                act.supportFragmentManager.commitNow { remove(it) }
            }
            arFragment = null; sceneView = null
        }
    }

    Scaffold(
        topBar = {
            TopBarReusable(title = stringResource(R.string.tool_ar_ruler), onBack = onBack)
        },
        bottomBar = {
            BottomAppBar{
                    Row(horizontalArrangement = Arrangement.SpaceAround,
                        modifier = Modifier.fillMaxWidth()) {

                        AssistChip(
                            onClick = {
                                vm.resetAll()
                                arFragment?.let { clearAllRendered(it) }
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            label = {
                                Text(stringResource(R.string.delete))
                            },
                            leadingIcon = {Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete)) }
                        )

                        AssistChip(
                            onClick = { vm.startCardCalibration() },
                            label = { Text("Calibrar tarjeta") },
                            leadingIcon = { Icon(Icons.Rounded.Straighten, contentDescription = null) }
                        )

                        // Indicador de paso, alineado arriba del FAB
                        if (vm.isCalibrating) {
                            val step = if (vm.pendingStartAnchor == null) "1/2" else "2/2"
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
                                vm.toggleUnits()
                                sceneView?.let {
                                    UnitConverter.convertAllArLabels(
                                        it,
                                        vm.unitSystem
                                    )
                                }
                            },
                            label = {
                                Text(
                                    if (vm.unitSystem == Units.METRIC)
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
            var isHitValid by remember { mutableStateOf(false) }
            DisposableEffect(sceneView) {
                val sv = sceneView ?: return@DisposableEffect onDispose {}
                val listener = Scene.OnUpdateListener {
                    val frame = sv.arFrame ?: return@OnUpdateListener
                    isHitValid = doCenterHitTest(frame, sv) != null
                }
                sv.scene.addOnUpdateListener(listener)
                onDispose { sv.scene.removeOnUpdateListener(listener) }
            }
            val container = if (isHitValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            val content   = if (isHitValid) MaterialTheme.colorScheme.onPrimary  else MaterialTheme.colorScheme.onSurfaceVariant

            FloatingActionButton(
                onClick = {
                    val frame = sceneView?.arFrame ?: return@FloatingActionButton
                    val hit = doCenterHitTest(frame, sceneView) ?: return@FloatingActionButton
                    val anchor = hit.createAnchor()

                    if (vm.isCalibrating) {
                        // Flujo de calibración: 2 toques → calcula scale
                        if (vm.pendingStartAnchor == null) {
                            vm.pendingStartAnchor = anchor
                            arFragment?.let { renderAutoscaledSphere(it, anchor) } // esfera chica opcional
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        } else {
                            val start = vm.pendingStartAnchor!!
                            val end = anchor
                            val measured = distanceMeters(start.pose, end.pose).toDouble()
                            vm.finishCalibration(measured)
                            // Limpieza visual rápida (opcional)
                            arFragment?.let { clearAllRendered(it) }
                            vm.pendingStartAnchor = null
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            // Re-render opcional del historial/labels (ver nota al final)
                        }
                        return@FloatingActionButton
                    }

                    // Flujo normal de medición
                    if (vm.pendingStartAnchor == null) {
                        vm.pendingStartAnchor = anchor
                        arFragment?.let { renderAutoscaledSphere(it, anchor) }
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } else {
                        val start = vm.pendingStartAnchor!!
                        val end = anchor
                        val rawM = distanceMeters(start.pose, end.pose).toDouble()
                        val id = vm.nextId()
                        vm.addMeasurement(Measurement(id, start, end, rawM))
                        arFragment?.let {
                            renderAutoscaledSphere(it, end)
                            renderDynamicLineAndLabel(
                                fragment = it,
                                startAnchor = start,
                                endAnchor   = end,
                                labelText   = vm.formatDistanceCorrected(rawM) // ← ya aplica scale
                            )
                        }
                        vm.pendingStartAnchor = null
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
                containerColor = container,
                contentColor = content,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) { Icon(Icons.Filled.Add, contentDescription = "Agregar punto") }
        }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {

            // Host del ArFragment dentro de Compose
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { parentContext ->
                    val container = FrameLayout(parentContext).apply { id = View.generateViewId() }

                    val act = parentContext.findActivity() as FragmentActivity
                    val fm  = act.supportFragmentManager
                    val tag = "AR_FRAGMENT"

                    // Limpia cualquier fragment anterior (esto no requiere que el container exista)
                    fm.findFragmentByTag(tag)?.let { fm.commitNow { remove(it) } }

                    // Recién cuando el container esté adjunto, montamos el fragment
                    container.doOnAttach {
                        // Evita doble attach si Compose recompone
                        if (fm.findFragmentByTag(tag) == null) {
                            val created = SimpleArFragment.newInstance().also { frag ->
                                frag.setOnViewCreatedListener { sv ->
                                    sv.cameraStream.depthOcclusionMode =
                                        com.google.ar.sceneform.rendering.CameraStream.DepthOcclusionMode
                                            .DEPTH_OCCLUSION_DISABLED
                                }
                            }
                            fm.commit {
                                setReorderingAllowed(true)
                                replace(container.id, created as Fragment, tag)
                            }
                            arFragment = created
                            container.post { sceneView = created.arSceneView }
                        }
                    }

                    container
                }
            )

            // Retícula central
            CrosshairOverlay(
                isValid = remember(sceneView?.arFrame) {
                    derivedStateOf {
                        val frame = sceneView?.arFrame ?: return@derivedStateOf false
                        doCenterHitTest(frame, sceneView) != null
                    }
                }.value
            )

            // Tips
            // Banner de tips (dinámico)
            AnimatedVisibility(
                visible = vm.isCalibrating || (vm.showTips && vm.pendingStartAnchor == null && vm.measurements.isEmpty()),
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
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Texto del tip
                        Text(
                            text = when {
                                isStep1 -> "Calibración (1/2): apoyá una tarjeta en un plano. Alineá la cruz con una esquina y tocá +."
                                isStep2 -> "Calibración (2/2): mové la cruz a la esquina opuesta y tocá +."
                                else    -> "Mové el teléfono para detectar superficies. Tocá + para fijar un punto."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )

                        // Botón cancelar sólo en calibración
                        if (vm.isCalibrating) {
                            TextButton(
                                onClick = { vm.cancelCalibration() }
                            ) { Text("Cancelar") }
                        }
                    }
                }
            }


            // Historial (usa el mismo formatter)
            if (vm.measurements.isNotEmpty()) {
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    tonalElevation = 2.dp
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.aruler_history), style = MaterialTheme.typography.labelLarge)
                        vm.measurements.takeLast(5).reversed().forEach { m ->
                            Text("• ${vm.formatDistanceCorrected(m.meters)}")
                        }
                    }
                }
            }
        }
    }

    BackHandler { onBack() }
}

/* ───────────────────────────── Render & AR helpers ───────────────────────────── */

private fun doCenterHitTest(frame: Frame, sceneView: SceneView?): com.google.ar.core.HitResult? {
    val vw = sceneView?.width ?: return null
    val vh = sceneView.height
    if (vw <= 0 || vh <= 0) return null
    val cx = vw / 2f
    val cy = vh / 2f
    return frame.hitTest(cx, cy).firstOrNull { hit ->
        val trackable = hit.trackable
        (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) ||
                (trackable is Point && trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)
    }
}

fun renderAutoscaledSphere(
    fragment: ArFragment,
    anchor: Anchor,
    baseRadiusAt1m: Float = 0.02f,  // 2 cm a 1 m
    minRadius: Float = 0.007f,       // 7 mm
    maxRadius: Float = 0.03f        // 3 cm
) {
    val scene = fragment.arSceneView.scene
    val anchorNode = AnchorNode(anchor).also { scene.addChild(it) }

    MaterialFactory
        .makeOpaqueWithColor(fragment.requireContext(), SColor(0.2f, 0.7f, 1.0f, 1f))
        .thenAccept { mat ->
            val renderable = ShapeFactory.makeSphere(baseRadiusAt1m, Vector3.zero(), mat)
            val sphereNode = Node().apply { this.renderable = renderable }
            anchorNode.addChild(sphereNode)

            // Actualiza el tamaño cada frame según distancia cámara ↔ punto
            val listener = Scene.OnUpdateListener {
                val cam = scene.camera.worldPosition
                val p = anchorNode.worldPosition
                val d = Vector3.subtract(cam, p).length().coerceAtLeast(0.0001f)

                // radio deseado proporcional a la distancia, con límites
                val desiredRadius = (d * baseRadiusAt1m).coerceIn(minRadius, maxRadius)
                val scale = desiredRadius / baseRadiusAt1m
                sphereNode.localScale = Vector3(scale, scale, scale)
            }
            scene.addOnUpdateListener(listener)
        }
}

fun renderDynamicLineAndLabel(
    fragment: ArFragment,
    startAnchor: Anchor,
    endAnchor: Anchor,
    labelText: String
) {
    val scene = fragment.arSceneView.scene
    val parent = Node().also { scene.addChild(it) }

    MaterialFactory.makeOpaqueWithColor(fragment.requireContext(), SColor(1f, 1f, 1f, 1f))
        .thenAccept { mat ->
            val cylinder = ShapeFactory.makeCylinder(0.003f, 1.0f, Vector3.zero(), mat)
            val cylNode = Node().apply { renderable = cylinder }
            parent.addChild(cylNode)

            val labelNode = Node()
            ViewRenderable.builder()
                .setView(fragment.requireContext(), R.layout.view_ar_label)
                .build()
                .thenAccept { r ->
                    r.isShadowCaster = false
                    r.isShadowReceiver = false
                    r.renderPriority = Renderable.RENDER_PRIORITY_LAST
                    r.view.findViewById<TextView>(R.id.labelText).apply {
                        text = labelText
                        if (background == null) setBackgroundResource(R.drawable.bg_ar_label)
                    }
                    labelNode.renderable = r
                    scene.addChild(labelNode)
                }

            val updater = Scene.OnUpdateListener {
                val sp = startAnchor.pose; val ep = endAnchor.pose
                val s = Vector3(sp.tx(), sp.ty(), sp.tz())
                val e = Vector3(ep.tx(), ep.ty(), ep.tz())
                val diff = Vector3.subtract(e, s)
                val len  = diff.length()
                val mid  = Vector3.add(s, e).scaled(0.5f)

                parent.worldPosition = mid
                parent.worldRotation = rotationFromUpTo(diff)
                cylNode.localScale   = Vector3(1f, len, 1f)

                if (labelNode.renderable != null) {
                    val cam = scene.camera.worldPosition
                    val dirToCam = Vector3.subtract(cam, mid).normalized()
                    labelNode.worldPosition = Vector3.add(mid, dirToCam.scaled(0.06f))
                    labelNode.worldRotation = Quaternion.lookRotation(dirToCam, Vector3.up())
                }
            }
            scene.addOnUpdateListener(updater)
        }
}

private fun rotationFromUpTo(dir: Vector3): Quaternion {
    val from = Vector3.up().normalized(); val to = dir.normalized()
    val dot = max(-1f, min(1f, Vector3.dot(from, to)))
    if (abs(dot - 1f) < 1e-6f) return Quaternion.identity()
    if (abs(dot + 1f) < 1e-6f) {
        val axis = Vector3.cross(from, Vector3.right()).let {
            if (it.length() < 1e-6f) Vector3.cross(from, Vector3.forward()) else it
        }.normalized()
        return Quaternion.axisAngle(axis, 180f)
    }
    val axis = Vector3.cross(from, to).normalized()
    val angleDeg = Math.toDegrees(acos(dot).toDouble()).toFloat()
    return Quaternion.axisAngle(axis, angleDeg)
}

private fun clearAllRendered(fragment: ArFragment) {
    fragment.arSceneView.scene.children.toList().forEach { child ->
        if (child !is Camera) fragment.arSceneView.scene.removeChild(child)
    }
}

private fun distanceMeters(p1: Pose, p2: Pose): Float {
    val dx = p2.tx() - p1.tx(); val dy = p2.ty() - p1.ty(); val dz = p2.tz() - p1.tz()
    return sqrt(dx*dx + dy*dy + dz*dz)
}

/* ────────────────────────────── Retícula y Fragment ────────────────────────────── */

@Composable
private fun CrosshairOverlay(isValid: Boolean) {
    Box(Modifier.fillMaxSize().drawBehind {
        val c = center; val len = size.minDimension * 0.04f
        val color = if (isValid) Color(0xFF4CAF50) else Color(0xFFB0B0B0)
        drawLine(color, Offset(c.x - len, c.y), Offset(c.x + len, c.y), 4f, StrokeCap.Round)
        drawLine(color, Offset(c.x, c.y - len), Offset(c.x, c.y + len), 4f, StrokeCap.Round)
        drawCircle(color, 6f, c, style = Stroke(width = 4f))
    })
}

class SimpleArFragment : ArFragment() {
    companion object { fun newInstance() = SimpleArFragment() }
}

/* ────────────────────────────── utilidades varias ────────────────────────────── */

private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("Context no es una Activity")
}

private fun traverseScene(scene: Scene, block: (Node) -> Unit) {
    val stack = ArrayDeque<Node>()
    scene.children.forEach { stack.add(it) }
    while (stack.isNotEmpty()) {
        val n = stack.removeLast()
        block(n)
        n.children.forEach { stack.add(it) }
    }
}