package com.joasasso.minitoolbox.tools.herramientas.instrumentos

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
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
import java.util.Locale
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sqrt


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARRulerScreen(
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val vm = remember { ARulerViewModel() }

    var currentUnits by remember { mutableStateOf(Units.METRIC) }

    // ----- AR Fragment host inside Compose -----
    var arFragment by remember { mutableStateOf<ArFragment?>(null) }
    var sceneView by remember { mutableStateOf<ArSceneView?>(null) }

    // Lifecycle bridge to pause/resume AR session correctly
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                sceneView?.resume()
            }
            override fun onPause(owner: LifecycleOwner) {
                sceneView?.pause()
            }
            override fun onDestroy(owner: LifecycleOwner) {
                sceneView?.destroy()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val ctx = LocalContext.current
    val fragTag = "AR_FRAGMENT"

    DisposableEffect(Unit) {
        onDispose {
            val act = ctx.findActivity()
            val fm = (act as FragmentActivity).supportFragmentManager
            val existing = fm.findFragmentByTag(fragTag)
            if (existing != null) {
                fm.commitNow {
                    remove(existing)
                }
            }
            arFragment = null
            sceneView = null
        }
    }

    Scaffold(floatingActionButtonPosition = FabPosition.Center,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.aruler_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.Close, contentDescription = null)
                    }
                },
                actions = {
                    // Toggle Unidades: Métrico/Imperial
                    AssistChip(
                        onClick = { vm.toggleUnitSystem()
                            val sv = sceneView ?: return@AssistChip
                            currentUnits = currentUnits.other()
                            convertAllArLabels(sv, currentUnits)},
                        label = {
                            Text(
                                if (vm.unitSystem == UnitSystem.METRIC)
                                    stringResource(R.string.aruler_units_metric)
                                else
                                    stringResource(R.string.aruler_units_imperial)
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Straighten, contentDescription = null)
                        }
                    )
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = {
                        vm.resetAll()
                        arFragment?.let { clearAllRendered(it) }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }) { Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset") }

                    Spacer(Modifier.weight(1f))

                    IconButton(onClick = { vm.showTips = !vm.showTips }) {
                        Icon(Icons.Rounded.TipsAndUpdates, contentDescription = "Tips")
                    }
                }
            )
        },
                floatingActionButton = {
            // ¿hay hit válido bajo la retícula?
            var isHitValid by remember { mutableStateOf(false) }

            DisposableEffect(sceneView) {
                val sv = sceneView
                if (sv == null) return@DisposableEffect onDispose {}
                val listener = Scene.OnUpdateListener {
                    val frame = sv.arFrame ?: return@OnUpdateListener
                    isHitValid = doCenterHitTest(frame, sv) != null
                }
                sv.scene.addOnUpdateListener(listener)
                onDispose { sv.scene.removeOnUpdateListener(listener) }
            }

            val container = if (isHitValid)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant

            val content = if (isHitValid)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSurfaceVariant

            FloatingActionButton(
                onClick = {
                    val frame = sceneView?.arFrame ?: return@FloatingActionButton
                    val hit = doCenterHitTest(frame, sceneView) ?: return@FloatingActionButton
                    val anchor = hit.createAnchor()

                    if (vm.pendingStartAnchor == null) {
                        vm.pendingStartAnchor = anchor
                        arFragment?.let { renderSphere(it, anchor) }
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    } else {
                        // Al seleccionar el segundo punto:
                        val start = vm.pendingStartAnchor!!
                        val end = anchor
                        val distM = distanceMeters(start.pose, end.pose)
                        vm.addMeasurement(Measurement(vm.nextId(), start, end, distM))

                        arFragment?.let {
                            renderSphere(it, end) // si ya pintás la esfera del segundo punto
                            renderDynamicLineAndLabel(it, start, end, vm.formatDistance(distM))
                        }
                        vm.pendingStartAnchor = null
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                },
                containerColor = container,
                contentColor = content,
                shape = CircleShape,
                modifier = Modifier.size(64.dp) // más grande y cómodo
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar punto")
            }
        }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            // Host del ArFragment
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { parentContext ->
                    val container = FrameLayout(parentContext).apply { id = View.generateViewId() }
                    val act = parentContext.findActivity() as AppCompatActivity
                    val tag = "AR_FRAGMENT"

                    val isAndroidX = Fragment::class.java
                        .isAssignableFrom(SimpleArFragment::class.java)

                    if (isAndroidX) {
                        val fm = act.supportFragmentManager
                        val existing = fm.findFragmentByTag(tag) as? SimpleArFragment
                        val frag = existing ?: SimpleArFragment.newInstance().also { created ->
                            fm.commit {
                                setReorderingAllowed(true)
                                replace(container.id, created as Fragment, tag)
                            }
                        }
                        arFragment = frag
                        container.post { sceneView = frag.arSceneView }
                    } else {
                        // ♻️ Sceneform legacy: usar fragmentManager (deprecated) y android.app.Fragment
                        @Suppress("DEPRECATION")
                        val fm = act.fragmentManager
                        @Suppress("DEPRECATION")
                        val existing = fm.findFragmentByTag(tag)
                        val frag = (existing as? SimpleArFragment) ?: SimpleArFragment.newInstance().also { created ->
                            @Suppress("DEPRECATION")
                            fm.beginTransaction()
                                .replace(container.id, created as android.app.Fragment, tag)
                                .commitAllowingStateLoss()
                        }
                        arFragment = frag
                        container.post { sceneView = frag.arSceneView }
                    }

                    container
                }
            )



            // Retícula central (verde cuando hay hit válido)
            CrosshairOverlay(
                isValid = remember(sceneView?.arFrame) {
                    derivedStateOf {
                        val frame = sceneView?.arFrame ?: return@derivedStateOf false
                        doCenterHitTest(frame, sceneView) != null
                    }
                }.value
            )

            // Tips / estado
            AnimatedVisibility(
                visible = vm.showTips && vm.pendingStartAnchor == null && vm.measurements.isEmpty(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    tonalElevation = 3.dp,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = stringResource(R.string.aruler_tip_scan),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Mini “historial” textual (opcional) — lista de medidas
            if (vm.measurements.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.aruler_history), style = MaterialTheme.typography.labelLarge)
                        vm.measurements.takeLast(5).reversed().forEach { m ->
                            Text("• ${vm.formatDistance(m.meters)}")
                        }
                    }
                }
            }
        }
    }

    BackHandler { onBack() }
}

/* ----------------------------- ViewModel & Utils ----------------------------- */

private class ARulerViewModel {
    var unitSystem by mutableStateOf(UnitSystem.METRIC)
        private set
    var showTips by mutableStateOf(true)

    var pendingStartAnchor: Anchor? by mutableStateOf(null)
    var measurements by mutableStateOf(listOf<Measurement>())

    private var counter = 0L
    fun nextId() = ++counter

    fun addMeasurement(m: Measurement) {
        measurements = measurements + m
    }

    fun resetAll() {
        measurements.forEach {
            // Liberar anchors (Sceneform libera renderables con el lifecycle del SceneView,
            // pero los anchors de ARCore es sano detacharlos)
            it.start.detach()
            it.end.detach()
        }
        pendingStartAnchor?.detach()
        pendingStartAnchor = null
        measurements = emptyList()
    }

    fun toggleUnitSystem() {
        unitSystem = if (unitSystem == UnitSystem.METRIC) UnitSystem.IMPERIAL else UnitSystem.METRIC
    }

    fun formatDistance(meters: Float): String {
        return when (unitSystem) {
            UnitSystem.METRIC -> formatMetric(meters)
            UnitSystem.IMPERIAL -> formatImperial(meters)
        }
    }
}

private data class Measurement(
    val id: Long,
    val start: Anchor,
    val end: Anchor,
    val meters: Float
)

private enum class UnitSystem { METRIC, IMPERIAL }

private fun formatMetric(m: Float): String {
    return if (m < 1f) {
        // cm con 1-2 decimales
        "${roundTo((m * 100f), 2)} cm"
    } else {
        "${roundTo(m, 2)} m"
    }
}

private fun formatImperial(m: Float): String {
    // 1 m = 39.3701 in, 12 in = 1 ft
    val totalIn = m * 39.3701f
    val feet = (totalIn / 12f).toInt()
    val inches = totalIn - (feet * 12)
    return if (feet >= 1) {
        "${feet}′ ${roundTo(inches, 1)}″"
    } else {
        "${roundTo(inches, 2)}″"
    }
}

private fun roundTo(v: Float, decimals: Int): String {
    val p = pow(decimals)
    val r = round(v * p) / p
    // Formato simple sin locales para no mezclar coma/punto aquí
    return if (decimals == 0) r.toInt().toString() else "%.${decimals}f".format(r)
}

private fun pow(n: Int): Float = generateSequence(1f) { it }.take(n).fold(1f) { acc, _ -> acc * 10f }

private fun distanceMeters(p1: Pose, p2: Pose): Float {
    val dx = p2.tx() - p1.tx()
    val dy = p2.ty() - p1.ty()
    val dz = p2.tz() - p1.tz()
    return sqrt(dx * dx + dy * dy + dz * dz)
}

/* ----------------------------- AR helpers ----------------------------- */

private fun doCenterHitTest(frame: Frame, sceneView: SceneView?): HitResult? {
    val vw = sceneView?.width ?: return null
    val vh = sceneView.height
    if (vw <= 0 || vh <= 0) return null
    val cx = vw / 2f
    val cy = vh / 2f
    return frame.hitTest(cx, cy).firstOrNull { hit ->
        val trackable = hit.trackable
        (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) ||
                (trackable is Point && trackable.orientationMode ==
                        Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)
    }
}


private fun renderSphere(fragment: ArFragment, anchor: Anchor) {
    val anchorNode = AnchorNode(anchor)
    fragment.arSceneView.scene.addChild(anchorNode)

    MaterialFactory.makeOpaqueWithColor(
        fragment.requireContext(),
        com.google.ar.sceneform.rendering.Color(0.2f, 0.7f, 1.0f, 1f)
    ).thenAccept { mat ->
        val sphereRenderable = ShapeFactory.makeSphere(
            /* radius = */ 0.015f, // ~1.5 cm
            Vector3.zero(),
            mat
        )
        val node = Node().apply { renderable = sphereRenderable }
        anchorNode.addChild(node)
    }
}


fun renderDynamicLineAndLabel(
    fragment: ArFragment,
    startAnchor: Anchor,
    endAnchor:   Anchor,
    labelText:   String
) {
    val scene = fragment.arSceneView.scene

    // Nodo padre en el punto medio (rotará y escalará el cilindro)
    val parent = Node().also { scene.addChild(it) }

    // Crea cilindro de altura 1 m (lo escalamos en Y cada frame)
    MaterialFactory.makeOpaqueWithColor(fragment.requireContext(), com.google.ar.sceneform.rendering.Color(1f, 1f, 1f, 1f))
        .thenAccept { mat ->
            val unitCylinder = ShapeFactory.makeCylinder(
                /* radius = */ 0.003f,
                /* height = */ 1.0f,
                Vector3.zero(),
                mat
            )
            val cylNode = Node().apply { renderable = unitCylinder }
            parent.addChild(cylNode)

            // ---- LABEL ----
            val labelNode = Node() // aún no lo agregamos a la escena
            ViewRenderable.builder()
                .setView(fragment.requireContext(), R.layout.view_ar_label)
                .build()
                .thenAccept { r ->
                    // Evitar artefactos / “doble fondo”
                    r.isShadowCaster = false
                    r.isShadowReceiver = false
                    r.renderPriority = Renderable.RENDER_PRIORITY_LAST

                    val tv = r.view.findViewById<android.widget.TextView>(R.id.labelText)
                    tv.text = labelText
                    if (tv.background == null) {
                        tv.setBackgroundResource(R.drawable.bg_ar_label)
                    }

                    labelNode.renderable = r
                    scene.addChild(labelNode)
                }

            // ---- ACTUALIZADOR POR FRAME ----
            val updater = Scene.OnUpdateListener {
                val sp = startAnchor.pose
                val ep = endAnchor.pose

                val s = Vector3(sp.tx(), sp.ty(), sp.tz())
                val e = Vector3(ep.tx(), ep.ty(), ep.tz())
                val diff = Vector3.subtract(e, s)
                val len  = diff.length()
                val mid  = Vector3.add(s, e).scaled(0.5f)

                // Línea
                parent.worldPosition = mid
                parent.worldRotation = rotationFromUpTo(diff)
                cylNode.localScale   = Vector3(1f, len, 1f) // cilindro era de 1m

                // Label (si ya está creado)
                if (labelNode.renderable != null) {
                    // Empújalo ~6 cm hacia la cámara para que nunca lo tape nada
                    val cam = scene.camera.worldPosition
                    val dirToCam = Vector3.subtract(cam, mid).normalized()
                    labelNode.worldPosition = Vector3.add(mid, dirToCam.scaled(0.06f))
                    labelNode.worldRotation = Quaternion.lookRotation(dirToCam, Vector3.up())
                }
            }
            scene.addOnUpdateListener(updater)
        }
}

private fun clearAllRendered(fragment: ArFragment) {
    fragment.arSceneView.scene.children.toList().forEach { child ->
        if (child !is Camera) {
            fragment.arSceneView.scene.removeChild(child)
        }
    }
}

/* ----------------------------- Helpers para el label ----------------------------- */
// Orienta el eje Y hacia 'dir' (cilindro de Sceneform está en Y)
private fun rotationFromUpTo(dir: Vector3): Quaternion {
    val from = Vector3.up().normalized()
    val to = dir.normalized()
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


/* ----------------------------- Overlay (retícula) ----------------------------- */

@Composable
private fun CrosshairOverlay(isValid: Boolean) {
    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                val c = center
                val len = size.minDimension * 0.04f
                val color = if (isValid) Color(0xFF4CAF50) else Color(0xFFB0B0B0)
                // horizontal
                drawLine(color, start = Offset(c.x - len, c.y),
                    end = Offset(c.x + len, c.y),
                    strokeWidth = 4f, cap = StrokeCap.Round)
                // vertical
                drawLine(color, start = Offset(c.x, c.y - len),
                    end = Offset(c.x, c.y + len),
                    strokeWidth = 4f, cap = StrokeCap.Round)
                // punto central
                drawCircle(color = color, radius = 6f, center = c, style = Stroke(width = 4f))
            }
    )
}

/* ----------------------------- ArFragment básico ----------------------------- */

class SimpleArFragment : ArFragment() {
    companion object { fun newInstance() = SimpleArFragment() }
}


// Helper para subir hasta la Activity real
private tailrec fun Context.findActivity(): Activity {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> error("Context no es una Activity")
    }
}

// Helpers para cambiar de sistema de medicion

private enum class Units { METRIC, IMPERIAL; fun other() = if (this == METRIC) IMPERIAL else METRIC }

/** Recorre todos los nodos del Scene, recursivo */
private fun traverseScene(scene: Scene, block: (Node) -> Unit) {
    fun visit(n: Node) {
        block(n)
        n.children.forEach { visit(it) }
    }
    scene.children.forEach { visit(it) }
}


/** Intenta parsear un label ("12.3 cm", "1.25 m", "72.0 in", "5'7.4\"") a metros */
private fun parseMetersFromLabel(text: String): Double? {
    val s = text.trim()
        .replace('’', '\'')   // prime unicode → '
        .replace('′', '\'')
        .replace('“', '"')    // double prime unicode → "
        .replace('”', '"')
        .replace(',', '.')    // por si el locale usa coma

    val lower = s.lowercase(Locale.ROOT)

    // 5'7.4"
    val feetInches = Regex("""^\s*(\d+)\s*'\s*([\d.]+)\s*"\s*$""").matchEntire(lower)
    if (feetInches != null) {
        val feet = feetInches.groupValues[1].toDoubleOrNull() ?: return null
        val inches = feetInches.groupValues[2].toDoubleOrNull() ?: return null
        val totalInches = feet * 12.0 + inches
        return totalInches * 0.0254
    }

    // 72.0 in
    val inchesOnly = Regex("""^\s*([\d.]+)\s*in\s*$""").matchEntire(lower)
    if (inchesOnly != null) {
        val inches = inchesOnly.groupValues[1].toDoubleOrNull() ?: return null
        return inches * 0.0254
    }

    // 1.25 m
    val meters = Regex("""^\s*([\d.]+)\s*m\s*$""").matchEntire(lower)
    if (meters != null) {
        return meters.groupValues[1].toDoubleOrNull()
    }

    // 12.3 cm
    val cm = Regex("""^\s*([\d.]+)\s*cm\s*$""").matchEntire(lower)
    if (cm != null) {
        val v = cm.groupValues[1].toDoubleOrNull() ?: return null
        return v / 100.0
    }

    return null
}

private fun formatMetric(meters: Double): String {
    return if (meters >= 1.0) {
        String.format(Locale.getDefault(), "%.2f m", meters)
    } else {
        String.format(Locale.getDefault(), "%.1f cm", meters * 100.0)
    }
}

private fun formatImperial(meters: Double): String {
    val totalInches = meters / 0.0254
    val feet = floor(totalInches / 12.0).toInt()
    val inches = totalInches - feet * 12
    return if (feet >= 1) {
        // 5'7.4"
        String.format(Locale.getDefault(), "%d'%s\"", feet,
            String.format(Locale.getDefault(), "%.1f", inches))
    } else {
        String.format(Locale.getDefault(), "%.1f in", inches)
    }
}

private fun convertAllArLabels(sceneView: ArSceneView, to: Units) {
    traverseScene(sceneView.scene) { node ->
        val r = node.renderable as? ViewRenderable ?: return@traverseScene
        val tv = r.view.findViewById<TextView?>(R.id.labelText) ?: return@traverseScene
        val current = tv.text?.toString()?.trim().orEmpty()
        val meters = parseMetersFromLabel(current) ?: return@traverseScene
        tv.text = when (to) {
            Units.METRIC   -> formatMetric(meters)
            Units.IMPERIAL -> formatImperial(meters)
        }
    }
}

