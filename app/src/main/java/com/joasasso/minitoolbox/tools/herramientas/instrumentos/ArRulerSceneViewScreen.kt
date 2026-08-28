package com.joasasso.minitoolbox.tools.herramientas.instrumentos

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import io.github.sceneview.ar.ARScene
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt

/* ═══════════════════════════════════════════════════════════════════
 *  CONSTANTES DE CALIDAD
 *  Más allá de ~5 m la triangulación de ARCore pierde precisión de
 *  forma abrupta. Cortamos ahí en vez de mostrar números inventados.
 * ═══════════════════════════════════════════════════════════════════ */

private const val MAX_HIT_DISTANCE_M = 5.0f

// Por debajo de ~20 cm el teléfono conmuta a la ultra gran angular para
// enfocar. ARCore NO actualiza sus intrínsecas cuando eso pasa: sigue
// creyendo que mira por la principal, y la escala de la sesión se rompe.
// La única defensa fiable es no dejar que ocurra.
private const val MIN_HIT_DISTANCE_M = 0.20f

// Un salto de cámara mayor a esto en un solo frame no es físico. Suele ser
// el síntoma de la conmutación de lente vista desde el VIO.
private const val CAMERA_JUMP_M = 0.35f

// El hit en vivo se filtra con la mediana de las últimas muestras. Si la
// dispersión supera este valor, el rayo está rebotando entre superficies
// distintas y capturar ahí produce un ancla a profundidad equivocada.
private const val LIVE_BUFFER = 6
private const val LIVE_STABLE_M = 0.04f

// Coseno del ángulo entre el segmento y el eje de visión. Por encima de
// esto los dos puntos están casi uno detrás del otro: todo el largo se
// juega en el eje donde la profundidad es menos precisa.
private const val ILL_CONDITIONED_COS = 0.94f
private const val ILL_MIN_LEN_M = 0.05f

// Distancia máxima a la que un punto de profundidad se aplana contra un
// plano detectado. Sube y capturás más pared; baja y respetás mejor los
// objetos que sobresalen de ella.
private const val PLANE_SNAP_MAX_M = 0.08f
private const val NO_SURFACE_FRAMES_FOR_HINT = 90   // ~3 s a 30 fps

// Muestreo del disparo: en vez de tomar UN frame, promediamos varios.
// Ataca directamente la dispersión medida en superficies verticales.
private const val SAMPLE_TARGET = 8       // muestras buenas que buscamos
private const val SAMPLE_MIN = 4          // mínimo aceptable
private const val SAMPLE_TIMEOUT_FRAMES = 25
private const val SAMPLE_OUTLIER_M = 0.03f  // descarte a >3 cm de la mediana

// El Pixel (y otros) conmuta a la ultra gran angular para enfocar de cerca.
// Eso cambia la focal y con ella la escala de la sesión: si pasa entre dos
// puntos de la misma medición, el resultado es basura.
private const val FOCAL_CHANGE_TOLERANCE = 0.02f
private const val LENS_WARN_FRAMES = 90

/* ═══════════════════════════════════════════════════════════════════
 *  UNIDADES Y FORMATO
 *  El redondeo NO es cosmético: comunicar "24,3 cm" implica una
 *  precisión milimétrica que ARCore no entrega. Redondeamos según la
 *  escala para que el número mostrado sea defendible.
 * ═══════════════════════════════════════════════════════════════════ */

private enum class Units { METRIC, IMPERIAL; fun toggle() = if (this == METRIC) IMPERIAL else METRIC }

private object UnitFormat {

    private fun snap(value: Double, step: Double): Double = (value / step).roundToInt() * step

    fun format(meters: Double, units: Units): String = when (units) {
        Units.METRIC -> when {
            // < 30 cm  -> resolución 0,5 cm
            meters < 0.30 -> String.format(Locale.getDefault(), "%.1f cm", snap(meters * 100.0, 0.5))
            // 30 cm a 1 m -> resolución 1 cm
            meters < 1.00 -> String.format(Locale.getDefault(), "%.0f cm", snap(meters * 100.0, 1.0))
            // 1 m a 2 m -> resolución 1 cm
            meters < 2.00 -> String.format(Locale.getDefault(), "%.2f m", snap(meters, 0.01))
            // > 2 m -> resolución 5 cm
            else -> String.format(Locale.getDefault(), "%.2f m", snap(meters, 0.05))
        }

        Units.IMPERIAL -> {
            val totalIn = snap(meters / 0.0254, 0.25)   // resolución 1/4"
            if (totalIn >= 12.0) {
                val ft = floor(totalIn / 12.0).toInt()
                val inch = totalIn - ft * 12.0
                String.format(Locale.getDefault(), "%d' %.2f\"", ft, inch)
            } else {
                String.format(Locale.getDefault(), "%.2f\"", totalIn)
            }
        }
    }
}

/* ═══════════════════════════════════════════════════════════════════
 *  MODELO
 *
 *  Clave del rediseño: UN SOLO ANCLA POR MEDICIÓN.
 *  Los puntos siguientes se guardan como offsets en el espacio local
 *  de ese ancla. Cuando ARCore re-localiza el mapa, el ancla se mueve
 *  y arrastra todos los puntos con ella: la geometría es RÍGIDA y las
 *  distancias son invariantes por construcción.
 * ═══════════════════════════════════════════════════════════════════ */

private enum class MeasureMode { SEGMENT, POLYLINE }

private class Measurement(
    val id: Int,
    val anchor: Anchor,
    val locals: List<FloatArray>,   // puntos en el espacio local del ancla
    val segments: List<Float>,      // longitud de cada tramo, en metros
    val total: Float
)

private enum class ArStatus { INIT, TOO_DARK, TOO_FAST, NO_FEATURES, CAMERA_OFF, NO_SURFACE, TOO_FAR, TOO_CLOSE, UNSTABLE, ILL_CONDITIONED, READY, SAMPLING, LENS_CHANGED }

/**
 * Mediana móvil del hit bajo la retícula. Cumple dos funciones: estabiliza
 * el preview (que antes parpadeaba entre superficies) y mide la dispersión,
 * que es lo que nos dice si es seguro capturar.
 */
private class LiveHitFilter {
    private val buf = ArrayDeque<FloatArray>()
    val value = FloatArray(3)
    var spread = Float.MAX_VALUE; private set
    var stable = false; private set

    fun push(p: FloatArray) {
        buf.addLast(p)
        while (buf.size > LIVE_BUFFER) buf.removeFirst()
        for (axis in 0..2) {
            val sorted = buf.map { it[axis] }.sorted()
            value[axis] = sorted[sorted.size / 2]
        }
        spread = buf.maxOf { dist3(it, value) }
        stable = buf.size >= 4 && spread <= LIVE_STABLE_M
    }

    fun reset() { buf.clear(); spread = Float.MAX_VALUE; stable = false }
}

/**
 * Acumula las posiciones del hit durante varios frames para promediarlas.
 * Antes cada punto era una sola muestra de un solo frame; el ruido de esa
 * muestra iba directo a la medición.
 */
private class HitSampler {
    var active = false
    var frames = 0
    val samples = mutableListOf<FloatArray>()

    fun start() { active = true; frames = 0; samples.clear() }
    fun stop() { active = false; frames = 0; samples.clear() }
}

/**
 * Aplana un punto contra la ecuación INFINITA de un plano detectado.
 * ARCore trackea las paredes en parches chicos, pero el plano matemático
 * se extiende más allá de su polígono: eso es lo que aprovechamos.
 * Devuelve null si el punto está demasiado lejos del plano.
 */
private fun snapToPlane(world: FloatArray, plane: Plane): FloatArray? {
    if (plane.trackingState != TrackingState.TRACKING) return null
    val pose = plane.centerPose
    val n = pose.yAxis              // en ARCore el eje Y del plano es su normal
    val p0 = pose.translation
    val d = (world[0] - p0[0]) * n[0] + (world[1] - p0[1]) * n[1] + (world[2] - p0[2]) * n[2]
    if (abs(d) > PLANE_SNAP_MAX_M) return null
    return floatArrayOf(world[0] - n[0] * d, world[1] - n[1] * d, world[2] - n[2] * d)
}

/**
 * Elige el plano contra el cual proyectar. `preferred` es el plano que ya
 * usó el primer punto de esta medición: mantenerlo evita que un extremo
 * quede sobre plano y el otro sobre profundidad, que es donde aparecían
 * los errores grandes.
 */
private fun bestPlaneFor(session: Session, world: FloatArray, preferred: Plane?): Plane? {
    if (preferred != null && snapToPlane(world, preferred) != null) return preferred

    var best: Plane? = null
    var bestDist = PLANE_SNAP_MAX_M
    runCatching {
        session.getAllTrackables(Plane::class.java).forEach { plane ->
            if (plane.trackingState != TrackingState.TRACKING) return@forEach
            if (plane.subsumedBy != null) return@forEach   // plano absorbido por otro mayor
            val pose = plane.centerPose
            val n = pose.yAxis
            val p0 = pose.translation
            val d = abs(
                (world[0] - p0[0]) * n[0] + (world[1] - p0[1]) * n[1] + (world[2] - p0[2]) * n[2]
            )
            if (d < bestDist) { bestDist = d; best = plane }
        }
    }
    return best
}

/** Mediana por eje, luego media de las muestras cercanas a esa mediana. */
private fun robustCentroid(samples: List<FloatArray>): FloatArray {
    val median = FloatArray(3)
    for (axis in 0..2) {
        val sorted = samples.map { it[axis] }.sorted()
        median[axis] = sorted[sorted.size / 2]
    }
    val kept = samples.filter { dist3(it, median) <= SAMPLE_OUTLIER_M }
    val base = if (kept.size >= 2) kept else samples
    val out = FloatArray(3)
    base.forEach { out[0] += it[0]; out[1] += it[1]; out[2] += it[2] }
    out[0] /= base.size; out[1] /= base.size; out[2] /= base.size
    return out
}

private fun dist3(a: FloatArray, b: FloatArray): Float {
    val dx = b[0] - a[0]; val dy = b[1] - a[1]; val dz = b[2] - a[2]
    return sqrt(dx * dx + dy * dy + dz * dz)
}

/* ═══════════════════════════════════════════════════════════════════
 *  ESTADO DE RENDER
 *
 *  Las matrices se copian en arrays propios dentro de onSessionUpdated.
 *  Nunca guardamos la referencia al Frame: ARCore REUTILIZA ese objeto
 *  entre updates, así que leerlo más tarde puede darte otro instante.
 *
 *  `tick` es lo único observable. Se lee dentro del DrawScope, así que
 *  invalida solo la fase de dibujo — no recompone el árbol.
 * ═══════════════════════════════════════════════════════════════════ */

private class ArOverlay {
    val view = FloatArray(16)
    val proj = FloatArray(16)
    val liveHit = FloatArray(3)
    var liveValid = false
    var ready = false
    var sampling = false
    val prevCam = FloatArray(3)
    var prevCamValid = false
    var tick by mutableIntStateOf(0)
    fun bump() { tick++ }
}

/* ═══════════════════════════════════════════════════════════════════
 *  VIEWMODEL (máquina de estados única)
 *
 *  Antes había dos estados paralelos (firstAnchor/secondAnchor en la
 *  pantalla + pendingStartAnchor en el VM) sincronizados a mano. Esa
 *  duplicación era la causa del bug de "la medida se recalcula".
 * ═══════════════════════════════════════════════════════════════════ */

private class ARulerVM {

    var unitSystem by mutableStateOf(Units.METRIC);            private set
    var mode by mutableStateOf(MeasureMode.SEGMENT);           private set
    var measurements by mutableStateOf(listOf<Measurement>()); private set

    // Medición en construcción
    var draftAnchor by mutableStateOf<Anchor?>(null);          private set
    var draftLocals by mutableStateOf(listOf<FloatArray>());   private set

    private var counter = 0

    fun toggleUnits() { unitSystem = unitSystem.toggle() }

    // OJO: no llamar a esta función `setMode`. La propiedad `var mode`
    // ya genera un setMode() en la JVM y las firmas colisionan.
    fun changeMode(newMode: MeasureMode) {
        if (newMode == mode) return
        cancelDraft()
        mode = newMode
    }

    /** Focal al momento de marcar el primer punto, para detectar cambio de lente. */
    var draftFocalPx: Float = 0f

    /** Plano sobre el que se resolvió el primer punto. Fija la superficie
     *  para toda la medición: sin esto, un extremo puede caer sobre plano
     *  y el otro sobre profundidad, y la diferencia entre ambas fuentes se
     *  suma entera al resultado. */
    var draftPlane: Plane? = null

    /**
     * @param world posición promediada del punto, en coordenadas de mundo.
     * @param hitForAnchor hit FRESCO del frame actual, solo para crear el ancla.
     *
     * El punto A ya no coincide con el origen del ancla: el ancla es apenas el
     * sistema de referencia compartido. Lo que da rigidez es que todos los
     * puntos vivan en el MISMO espacio local, no que uno esté en el origen.
     */
    fun addPointAt(world: FloatArray, hitForAnchor: HitResult, plane: Plane?): Boolean {
        var anchor = draftAnchor

        if (anchor == null) {
            anchor = runCatching { hitForAnchor.createAnchor() }.getOrNull() ?: return false
            draftAnchor = anchor
            draftPlane = plane
            draftLocals = listOf(anchor.pose.inverse().transformPoint(world))
            return true
        }

        if (anchor.trackingState != TrackingState.TRACKING) return false

        draftLocals = draftLocals + listOf(anchor.pose.inverse().transformPoint(world))
        if (mode == MeasureMode.SEGMENT && draftLocals.size >= 2) commitDraft()
        return true
    }

    fun commitDraft(): Boolean {
        val anchor = draftAnchor ?: return false
        if (draftLocals.size < 2) return false
        val segs = draftLocals.zipWithNext { p, q -> dist3(p, q) }
        // La propiedad del ancla pasa a la Measurement: no se detachea acá.
        measurements = measurements + Measurement(++counter, anchor, draftLocals, segs, segs.sum())
        draftAnchor = null
        draftLocals = emptyList()
        draftFocalPx = 0f
        draftPlane = null
        return true
    }

    fun cancelDraft() {
        draftAnchor?.detach()
        draftAnchor = null
        draftLocals = emptyList()
        draftFocalPx = 0f
        draftPlane = null
    }

    fun undo(): Boolean {
        if (draftLocals.isNotEmpty()) {
            if (draftLocals.size <= 1) { cancelDraft(); return true }
            draftLocals = draftLocals.dropLast(1)
            return true
        }
        val last = measurements.lastOrNull() ?: return false
        last.anchor.detach()
        measurements = measurements.dropLast(1)
        return true
    }

    fun remove(id: Int) {
        val m = measurements.firstOrNull { it.id == id } ?: return
        m.anchor.detach()
        measurements = measurements.filterNot { it.id == id }
    }

    fun clearAll() {
        measurements.forEach { it.anchor.detach() }
        measurements = emptyList()
        cancelDraft()
    }

    fun format(meters: Float): String = UnitFormat.format(meters.toDouble(), unitSystem)

    val canUndo: Boolean get() = draftLocals.isNotEmpty() || measurements.isNotEmpty()
    val canFinish: Boolean get() = mode == MeasureMode.POLYLINE && draftLocals.size >= 2
}

/* ═══════════════════════════════════════════════════════════════════
 *  PANTALLA
 * ═══════════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArRulerSceneViewScreen(onBack: () -> Unit) {

    val vm = remember { ARulerVM() }
    val overlay = remember { ArOverlay() }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val density = LocalDensity.current

    var showInfo by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)

    // Estos se leen DENTRO del callback de frame. Los guardamos como
    // objetos State (no con `by`) para que el callback siempre vea el
    // valor actual aunque SceneView conserve una lambda vieja.
    val viewportState = remember { mutableStateOf(IntSize.Zero) }
    val statusState = remember { mutableStateOf(ArStatus.INIT) }
    val depthHintState = remember { mutableStateOf(false) }
    val status by statusState
    val showDepthHint by depthHintState

    // Bandera de captura: el tap NO crea el ancla directo.
    // El HitResult es válido solo dentro del frame que lo generó, así que
    // pedimos la captura y la ejecutamos en el próximo onSessionUpdated.
    val captureRequested = remember { mutableStateOf(false) }
    val noSurfaceFrames = remember { intArrayOf(0) }
    val lensWarnFrames = remember { intArrayOf(0) }
    val debugTickCounter = remember { intArrayOf(0) }
    val sampler = remember { HitSampler() }
    val liveFilter = remember { LiveHitFilter() }

    // HUD de diagnóstico: se activa tocando el banner de estado.
    val debugEnabled = remember { mutableStateOf(false) }
    val debugText = remember { mutableStateOf("") }

    // Paleta
    val activeColor = Color(0xFFFFC107)   // ámbar: medición en curso
    val historyColor = Color(0xFF40C4FF)  // celeste: mediciones confirmadas

    // Métricas de dibujo, calculadas una vez
    val lineWidthPx = with(density) { 3.dp.toPx() }
    val haloWidthPx = with(density) { 6.dp.toPx() }
    val capLenPx = with(density) { 9.dp.toPx() }
    val tickLenPx = with(density) { 4.dp.toPx() }
    val labelTextPx = with(density) { 14.sp.toPx() }
    val labelPadPx = with(density) { 7.dp.toPx() }

    val activeLabel = remember(labelTextPx) { LabelPainter(labelTextPx, labelPadPx, 0xF0000000.toInt()) }
    val historyLabel = remember(labelTextPx) { LabelPainter(labelTextPx * 0.92f, labelPadPx, 0xB3000000.toInt()) }

    // Pantalla encendida durante la sesión + liberación de anclajes al salir
    DisposableEffect(Unit) {
        val activity = runCatching { context.findActivity() }.getOrNull()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            vm.clearAll()
        }
    }

    Scaffold(
        // ── TopBar sin tocar: es un componente compartido entre tools ──
        topBar = {
            TopBarReusable(
                title = stringResource(R.string.tool_ar_ruler),
                onBack = onBack,
                onShowInfo = { showInfo = true }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {

                    AssistChip(
                        onClick = {
                            vm.changeMode(if (vm.mode == MeasureMode.SEGMENT) MeasureMode.POLYLINE else MeasureMode.SEGMENT)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        label = {
                            Text(
                                if (vm.mode == MeasureMode.SEGMENT) stringResource(R.string.aruler_mode_segment)
                                else stringResource(R.string.aruler_mode_polyline)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (vm.mode == MeasureMode.SEGMENT) Icons.Rounded.Straighten else Icons.Rounded.Timeline,
                                contentDescription = null
                            )
                        }
                    )

                    AssistChip(
                        enabled = vm.canUndo,
                        onClick = {
                            if (vm.undo()) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        label = { Text(stringResource(R.string.aruler_undo)) },
                        leadingIcon = { Icon(Icons.Rounded.Undo, contentDescription = null) }
                    )

                    AssistChip(
                        onClick = { vm.toggleUnits() },
                        label = {
                            Text(
                                if (vm.unitSystem == Units.METRIC) stringResource(R.string.aruler_units_metric)
                                else stringResource(R.string.aruler_units_imperial)
                            )
                        },
                        leadingIcon = { Icon(Icons.Rounded.Straighten, contentDescription = null) }
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {

                // "Borrar todo" solo aparece si hay algo que borrar y no hay
                // ninguna medición a medio hacer (ahí el botón correcto es Deshacer).
                // Es mutuamente excluyente con "Finalizar": nunca hay 3 botones apilados.
                AnimatedVisibility(visible = vm.measurements.isNotEmpty() && vm.draftLocals.isEmpty()) {
                    SmallFloatingActionButton(
                        onClick = {
                            showClearConfirm = true
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                    }
                }

                // Botón "Finalizar" solo en modo recorrido y con al menos 2 puntos
                AnimatedVisibility(visible = vm.canFinish) {
                    SmallFloatingActionButton(
                        onClick = {
                            if (vm.commitDraft()) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.aruler_finish))
                    }
                }

                val ready = status == ArStatus.READY
                FloatingActionButton(
                    onClick = {
                        if (!ready) return@FloatingActionButton
                        captureRequested.value = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    containerColor = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (ready) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = CircleShape,
                    modifier = Modifier.size(76.dp)
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.aruler_cd_capture),
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
    ) { padding ->

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding)
                .onSizeChanged { viewportState.value = it }
        ) {

            /* ───── Cámara / sesión AR ───── */
            ARScene(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                modelLoader = modelLoader,
                sessionConfiguration = { session: Session, config: Config ->
                    // Planos verticales: SIN esto las paredes no se buscan nunca.
                    config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL

                    // Depth API: habilita DepthPoint en el hit test, que da
                    // profundidad donde no hay plano ni feature point.
                    config.depthMode =
                        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) Config.DepthMode.AUTOMATIC
                        else Config.DepthMode.DISABLED

                    // Sin autofoco, los objetos a menos de 50 cm salen borrosos
                    // y el tracking se degrada.
                    config.focusMode = Config.FocusMode.AUTO
                    config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    config.instantPlacementMode = Config.InstantPlacementMode.DISABLED

                    // No renderizamos objetos con iluminación: apagarla ahorra CPU.
                    config.lightEstimationMode = Config.LightEstimationMode.DISABLED
                },
                onSessionUpdated = { session, frame ->
                    onFrame(
                        session = session,
                        frame = frame,
                        viewportState = viewportState,
                        overlay = overlay,
                        vm = vm,
                        sampler = sampler,
                        liveFilter = liveFilter,
                        captureRequested = captureRequested,
                        statusState = statusState,
                        depthHintState = depthHintState,
                        noSurfaceFrames = noSurfaceFrames,
                        lensWarnFrames = lensWarnFrames,
                        debugEnabled = debugEnabled,
                        debugText = debugText,
                        debugTickCounter = debugTickCounter
                    )
                }
            )

            /* ───── Overlay 2D unificado ─────
               Todo (líneas, marcas, extremos y etiquetas) se dibuja en un
               único Canvas. Antes las etiquetas eran composables Box/Text
               recreados 60 veces por segundo. */
            Canvas(Modifier.fillMaxSize()) {
                // Leer `tick` acá dentro suscribe SOLO la fase de dibujo:
                // el árbol de composables no se recompone por frame.
                val frameTick = overlay.tick
                if (frameTick < 0 || !overlay.ready || size.width < 1f) return@Canvas

                val vp = IntSize(size.width.roundToInt(), size.height.roundToInt())

                // Mediciones confirmadas
                vm.measurements.forEach { m ->
                    if (m.anchor.trackingState != TrackingState.TRACKING) return@forEach
                    val pose = m.anchor.pose
                    val pts = m.locals.map { local ->
                        projectToScreen(overlay.view, overlay.proj, pose.transformPoint(local), vp)
                    }
                    drawMeasure(
                        pts = pts,
                        segMeters = m.segments,
                        color = historyColor,
                        lineWidth = lineWidthPx * 0.85f,
                        haloWidth = haloWidthPx * 0.85f,
                        capLen = capLenPx * 0.8f,
                        tickLen = tickLenPx,
                        painter = historyLabel,
                        labeler = { vm.format(it) },
                        totalLabel = if (m.segments.size > 1) vm.format(m.total) else null
                    )
                }

                // Medición en curso + preview vivo hacia la retícula
                val draftAnchor = vm.draftAnchor
                val locals = vm.draftLocals
                if (draftAnchor != null && locals.isNotEmpty() &&
                    draftAnchor.trackingState == TrackingState.TRACKING
                ) {
                    val pose = draftAnchor.pose
                    val worlds = locals.map { pose.transformPoint(it) }.toMutableList()
                    val segs = worlds.zipWithNext { a, b -> dist3(a, b) }.toMutableList()

                    // Tramo en vivo: del último punto colocado a donde apunta la mira
                    if (overlay.liveValid) {
                        val live = floatArrayOf(overlay.liveHit[0], overlay.liveHit[1], overlay.liveHit[2])
                        segs.add(dist3(worlds.last(), live))
                        worlds.add(live)
                    }

                    val pts = worlds.map { projectToScreen(overlay.view, overlay.proj, it, vp) }
                    drawMeasure(
                        pts = pts,
                        segMeters = segs,
                        color = activeColor,
                        lineWidth = lineWidthPx,
                        haloWidth = haloWidthPx,
                        capLen = capLenPx,
                        tickLen = tickLenPx,
                        painter = activeLabel,
                        labeler = { vm.format(it) },
                        totalLabel = if (segs.size > 1) vm.format(segs.sum()) else null
                    )
                }

                // Retícula central (ámbar mientras promedia muestras)
                drawCrosshair(valid = status == ArStatus.READY || overlay.sampling, sampling = overlay.sampling)
            }

            /* ───── Estado de tracking + HUD de diagnóstico ───── */
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp, start = 12.dp, end = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusBanner(
                    status = status,
                    showDepthHint = showDepthHint,
                    pointsPlaced = vm.draftLocals.size,
                    mode = vm.mode,
                    modifier = Modifier.clickable { debugEnabled.value = !debugEnabled.value }
                )

                if (debugEnabled.value) {
                    Surface(
                        color = Color(0xE6000000),
                        contentColor = Color(0xFF7CFF7C),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = debugText.value,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }
            }

        }
    }

    BackHandler { onBack() }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.aruler_clear_confirm_title)) },
            text = { Text(stringResource(R.string.aruler_clear_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearAll()
                    showClearConfirm = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showInfo) {
        val cfg = LocalConfiguration.current
        val maxHeight = (cfg.screenHeightDp * 0.75f).dp
        val scroll = rememberScrollState()
        AlertDialog(
            onDismissRequest = {
                showInfo = false
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            title = { Text(stringResource(R.string.aruler_help_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = maxHeight)
                        .verticalScroll(scroll),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.aruler_help_intro))
                    Section(stringResource(R.string.aruler_help_requirements_title), stringArrayResource(R.array.aruler_help_requirements))
                    Section(stringResource(R.string.aruler_help_how_title), stringArrayResource(R.array.aruler_help_steps))
                    Section(stringResource(R.string.aruler_help_ui_title), stringArrayResource(R.array.aruler_help_buttons))
                    Section(stringResource(R.string.aruler_help_tips_title), stringArrayResource(R.array.aruler_help_tips))
                    Section(stringResource(R.string.aruler_help_troubles_title), stringArrayResource(R.array.aruler_help_troubles))
                    Text(stringResource(R.string.aruler_help_privacy), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showInfo = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) { Text(stringResource(R.string.close)) }
            }
        )
    }
}

/* ═══════════════════════════════════════════════════════════════════
 *  LOOP DE FRAME
 *  Todo lo caro pasa acá, fuera de la composición.
 * ═══════════════════════════════════════════════════════════════════ */

private fun onFrame(
    session: Session,
    frame: Frame,
    viewportState: MutableState<IntSize>,
    overlay: ArOverlay,
    vm: ARulerVM,
    sampler: HitSampler,
    liveFilter: LiveHitFilter,
    captureRequested: MutableState<Boolean>,
    statusState: MutableState<ArStatus>,
    depthHintState: MutableState<Boolean>,
    noSurfaceFrames: IntArray,
    lensWarnFrames: IntArray,
    debugEnabled: MutableState<Boolean>,
    debugText: MutableState<String>,
    debugTickCounter: IntArray
) {
    val viewport = viewportState.value
    if (viewport == IntSize.Zero) return

    fun publish(newStatus: ArStatus, hint: Boolean) {
        // El aviso de cambio de lente es pegajoso ~3 s: si no, el frame
        // siguiente lo pisa con READY y el usuario nunca lo lee.
        val effective = if (lensWarnFrames[0] > 0) ArStatus.LENS_CHANGED else newStatus
        if (statusState.value != effective) statusState.value = effective
        if (depthHintState.value != hint) depthHintState.value = hint
    }

    if (lensWarnFrames[0] > 0) lensWarnFrames[0]--

    val camera = frame.camera
    var camJump = 0f

    if (camera.trackingState != TrackingState.TRACKING) {
        overlay.liveValid = false
        overlay.ready = false
        overlay.sampling = false
        overlay.prevCamValid = false
        sampler.stop()
        liveFilter.reset()
        captureRequested.value = false
        publish(
            when (camera.trackingFailureReason) {
                TrackingFailureReason.INSUFFICIENT_LIGHT -> ArStatus.TOO_DARK
                TrackingFailureReason.EXCESSIVE_MOTION -> ArStatus.TOO_FAST
                TrackingFailureReason.INSUFFICIENT_FEATURES -> ArStatus.NO_FEATURES
                TrackingFailureReason.CAMERA_UNAVAILABLE -> ArStatus.CAMERA_OFF
                else -> ArStatus.INIT
            },
            false
        )
        overlay.bump()
        return
    }

    // Snapshot de matrices: nunca guardamos el Frame, que ARCore reutiliza.
    camera.getViewMatrix(overlay.view, 0)
    camera.getProjectionMatrix(overlay.proj, 0, 0.01f, 100f)
    overlay.ready = true

    /* ── Guardia de conmutación de lente ──
       ARCore NO actualiza sus intrínsecas cuando el teléfono cambia de
       lente, así que comparar la focal no alcanza (en un Pixel 8 el valor
       ni se mueve). Lo que sí se ve es el efecto: el VIO interpreta el
       cambio de campo de visión como un desplazamiento enorme de cámara.
       Un salto así en un frame no es físico. */
    val camPos = camera.pose.translation
    if (overlay.prevCamValid) camJump = dist3(overlay.prevCam, camPos)
    overlay.prevCam[0] = camPos[0]; overlay.prevCam[1] = camPos[1]; overlay.prevCam[2] = camPos[2]
    overlay.prevCamValid = true

    val focalPx = runCatching { camera.imageIntrinsics.focalLength[0] }.getOrDefault(0f)
    val focalChanged = vm.draftFocalPx > 0f && focalPx > 0f &&
            abs(focalPx - vm.draftFocalPx) / vm.draftFocalPx > FOCAL_CHANGE_TOLERANCE

    if (vm.draftLocals.isNotEmpty() && (camJump > CAMERA_JUMP_M || focalChanged)) {
        vm.cancelDraft()
        sampler.stop()
        overlay.sampling = false
        captureRequested.value = false
        lensWarnFrames[0] = LENS_WARN_FRAMES
        publish(ArStatus.LENS_CHANGED, false)
        overlay.bump()
        return
    }

    val depthOn = runCatching { session.config.depthMode != Config.DepthMode.DISABLED }.getOrDefault(false)
    val hit = bestHit(
        frame, viewport.width / 2f, viewport.height / 2f,
        preferredPlane = vm.draftPlane,
        allowFeaturePoints = !depthOn
    )

    val geomStatus = when {
        hit == null -> ArStatus.NO_SURFACE
        hit.distance > MAX_HIT_DISTANCE_M -> ArStatus.TOO_FAR
        hit.distance < MIN_HIT_DISTANCE_M -> ArStatus.TOO_CLOSE
        else -> ArStatus.READY
    }

    if (geomStatus == ArStatus.NO_SURFACE) noSurfaceFrames[0]++ else noSurfaceFrames[0] = 0

    // Mediana móvil: el preview deja de parpadear y sabemos si el rayo
    // está rebotando entre dos superficies a profundidades distintas.
    if (geomStatus == ArStatus.READY && hit != null) {
        liveFilter.push(hit.hitPose.translation)
        overlay.liveHit[0] = liveFilter.value[0]
        overlay.liveHit[1] = liveFilter.value[1]
        overlay.liveHit[2] = liveFilter.value[2]
        overlay.liveValid = true
    } else {
        liveFilter.reset()
        overlay.liveValid = false
    }

    /* ── Geometría mal condicionada ──
       Si el segmento apunta casi en la dirección de la mirada, los dos
       puntos quedan uno detrás del otro: en pantalla se superponen y todo
       el largo se juega en el eje de profundidad, que es el peor estimado.
       Es el caso donde dos puntos coincidentes marcaban 69 cm. */
    var illConditioned = false
    val anchorForGeom = vm.draftAnchor
    if (overlay.liveValid && anchorForGeom != null && vm.draftLocals.isNotEmpty() &&
        anchorForGeom.trackingState == TrackingState.TRACKING
    ) {
        val a = anchorForGeom.pose.transformPoint(vm.draftLocals.last())
        val len = dist3(a, overlay.liveHit)
        if (len > ILL_MIN_LEN_M) {
            val fwd = camera.pose.zAxis
            val cos = abs(
                ((overlay.liveHit[0] - a[0]) * fwd[0] +
                        (overlay.liveHit[1] - a[1]) * fwd[1] +
                        (overlay.liveHit[2] - a[2]) * fwd[2]) / len
            )
            illConditioned = cos > ILL_CONDITIONED_COS
        }
    }

    val baseStatus = when {
        geomStatus != ArStatus.READY -> geomStatus
        illConditioned -> ArStatus.ILL_CONDITIONED
        !liveFilter.stable -> ArStatus.UNSTABLE
        else -> ArStatus.READY
    }
    val usable = baseStatus == ArStatus.READY && hit != null

    /* ── Muestreo del disparo ──
       Tocar + no coloca el punto: abre una ventana de ~8 frames, junta
       posiciones, descarta outliers y usa el promedio robusto. */
    if (captureRequested.value && !sampler.active) {
        captureRequested.value = false
        sampler.start()
    }

    if (sampler.active) {
        sampler.frames++
        if (usable) sampler.samples.add(hit!!.hitPose.translation)

        val done = sampler.samples.size >= SAMPLE_TARGET || sampler.frames >= SAMPLE_TIMEOUT_FRAMES
        if (done) {
            if (sampler.samples.size >= SAMPLE_MIN && usable) {
                val raw = robustCentroid(sampler.samples)

                // Proyección sobre plano extendido: el parche chico que
                // ARCore detectó en la pared gobierna toda la pared.
                val plane = bestPlaneFor(session, raw, vm.draftPlane)
                val world = plane?.let { snapToPlane(raw, it) } ?: raw
                lastSnapInfo[0] = if (plane == null) "no"
                else if (plane.type == Plane.Type.VERTICAL) "VERT" else "HORIZ"

                val wasEmpty = vm.draftLocals.isEmpty()
                if (vm.addPointAt(world, hit!!, plane) && wasEmpty) {
                    vm.draftFocalPx = focalPx
                }
            }
            sampler.stop()
        }
    }
    overlay.sampling = sampler.active

    publish(
        if (sampler.active) ArStatus.SAMPLING else baseStatus,
        noSurfaceFrames[0] > NO_SURFACE_FRAMES_FOR_HINT
    )

    /* ── HUD de diagnóstico ── */
    if (debugEnabled.value) {
        debugTickCounter[0]++
        if (debugTickCounter[0] % 15 == 0) {
            debugText.value = buildDebugText(session, hit, focalPx, camJump, vm.draftPlane != null, liveFilter.spread)
        }
    }

    overlay.bump()
}

/**
 * Lee la configuración REAL de la sesión, no la que pedimos. Si SceneView
 * pisa nuestro lambda, acá se ve.
 */
private val lastSnapInfo = arrayOf("-")

private fun buildDebugText(
    session: Session,
    hit: HitResult?,
    focalPx: Float,
    camJump: Float,
    planeLocked: Boolean,
    liveSpread: Float
): String {
    val cfg = runCatching { session.config }.getOrNull()

    var horiz = 0
    var vert = 0
    runCatching {
        session.getAllTrackables(Plane::class.java).forEach { p ->
            if (p.trackingState != TrackingState.TRACKING) return@forEach
            if (p.type == Plane.Type.VERTICAL) vert++ else horiz++
        }
    }

    val hitDesc = when (val t = hit?.trackable) {
        null -> "ninguno"
        is Plane -> "Plane/" + if (t.type == Plane.Type.VERTICAL) "VERT" else "HORIZ"
        is DepthPoint -> "DepthPoint"
        is Point -> "FeaturePoint"
        else -> t.javaClass.simpleName
    }

    val dist = hit?.let { String.format(Locale.US, "%.2fm", it.distance) } ?: "-"

    return buildString {
        append("cfg.depth=").append(cfg?.depthMode ?: "?")
        append("  cfg.planes=").append(cfg?.planeFindingMode ?: "?").append('\n')
        append("planos trackeados: H=").append(horiz).append("  V=").append(vert).append('\n')
        append("hit=").append(hitDesc).append("  d=").append(dist).append('\n')
        append("focal=").append(focalPx.roundToInt()).append("px")
        append("  salto=").append(String.format(Locale.US, "%.3f", camJump)).append('\n')
        append("ultimo snap=").append(lastSnapInfo[0])
        append("  plano fijado=").append(if (planeLocked) "si" else "no").append('\n')
        append("dispersion vivo=")
        append(if (liveSpread > 1f) "-" else String.format(Locale.US, "%.3f", liveSpread))
    }
}

/* ═══════════════════════════════════════════════════════════════════
 *  HIT TEST
 *  El orden importa: hitTest() devuelve los resultados por distancia,
 *  no por calidad. Un feature point suelto puede quedar delante del
 *  plano real y arruinar la medición.
 *    1. Plane      -> el más estable (promedia cientos de puntos)
 *    2. DepthPoint -> denso, funciona donde no hay plano detectado
 *    3. Point      -> último recurso
 * ═══════════════════════════════════════════════════════════════════ */

private fun bestHit(
    frame: Frame,
    x: Float,
    y: Float,
    preferredPlane: Plane?,
    allowFeaturePoints: Boolean
): HitResult? {
    val hits = runCatching { frame.hitTest(x, y) }.getOrNull() ?: return null
    if (hits.isEmpty()) return null

    // Si la medición ya fijó una superficie, un hit sobre ESA superficie
    // gana a cualquier otro.
    if (preferredPlane != null) {
        hits.firstOrNull { it.trackable === preferredPlane }?.let { return it }
    }

    hits.firstOrNull { h ->
        val t = h.trackable
        t is Plane && t.isPoseInPolygon(h.hitPose) && t.trackingState == TrackingState.TRACKING
    }?.let { return it }

    hits.firstOrNull { it.trackable is DepthPoint }?.let { return it }

    // Un ancla atada a un feature point HEREDA su inestabilidad: ARCore
    // re-estima la profundidad de ese punto frame a frame y el ancla lo
    // sigue, deslizándose por la pantalla en sentido contrario al
    // movimiento del teléfono. Solo lo aceptamos si el dispositivo no
    // tiene Depth API y por lo tanto no hay nada mejor.
    if (!allowFeaturePoints) return null

    return hits.firstOrNull { h ->
        val t = h.trackable
        t is Point && t.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
    }
}

/* ═══════════════════════════════════════════════════════════════════
 *  PROYECCIÓN MUNDO -> PANTALLA
 * ═══════════════════════════════════════════════════════════════════ */

private fun projectToScreen(
    view: FloatArray,
    proj: FloatArray,
    world: FloatArray,
    viewport: IntSize
): Offset? {
    val wx = world[0]; val wy = world[1]; val wz = world[2]

    val vx = view[0] * wx + view[4] * wy + view[8] * wz + view[12]
    val vy = view[1] * wx + view[5] * wy + view[9] * wz + view[13]
    val vz = view[2] * wx + view[6] * wy + view[10] * wz + view[14]
    val vw = view[3] * wx + view[7] * wy + view[11] * wz + view[15]

    val cx = proj[0] * vx + proj[4] * vy + proj[8] * vz + proj[12] * vw
    val cy = proj[1] * vx + proj[5] * vy + proj[9] * vz + proj[13] * vw
    val cz = proj[2] * vx + proj[6] * vy + proj[10] * vz + proj[14] * vw
    val cw = proj[3] * vx + proj[7] * vy + proj[11] * vz + proj[15] * vw

    // cw <= 0 significa DETRÁS de la cámara. El código anterior sólo
    // chequeaba cw == 0f, así que esos puntos se proyectaban espejados.
    if (cw <= 1e-6f) return null
    if (cz > cw) return null   // más allá del plano lejano

    val ndcX = cx / cw
    val ndcY = cy / cw
    return Offset(
        (ndcX * 0.5f + 0.5f) * viewport.width,
        (1f - (ndcY * 0.5f + 0.5f)) * viewport.height
    )
}

/* ═══════════════════════════════════════════════════════════════════
 *  DIBUJO
 * ═══════════════════════════════════════════════════════════════════ */

private class LabelPainter(textSizePx: Float, val pad: Float, bgColor: Int) {
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = textSizePx
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    private val bounds = Rect()

    fun draw(canvas: android.graphics.Canvas, cx: Float, cy: Float, s: String) {
        text.getTextBounds(s, 0, s.length, bounds)
        val w = text.measureText(s)
        val h = bounds.height().toFloat()
        canvas.drawRoundRect(
            cx - w / 2f - pad, cy - h - pad,
            cx + w / 2f + pad, cy + pad,
            pad, pad, bg
        )
        canvas.drawText(s, cx, cy, text)
    }
}

private val HALO = Color(0x99000000)

/**
 * Dibuja una medición completa: halo + línea + graduaciones + extremos
 * en T + etiqueta por tramo. Las graduaciones son las que convierten
 * "una línea con un número" en algo que se lee como cinta métrica.
 */
private fun DrawScope.drawMeasure(
    pts: List<Offset?>,
    segMeters: List<Float>,
    color: Color,
    lineWidth: Float,
    haloWidth: Float,
    capLen: Float,
    tickLen: Float,
    painter: LabelPainter,
    labeler: (Float) -> String,
    totalLabel: String?
) {
    val canvas = drawContext.canvas.nativeCanvas

    for (i in 0 until pts.size - 1) {
        val a = pts[i] ?: continue
        val b = pts[i + 1] ?: continue
        val meters = segMeters.getOrNull(i) ?: continue

        val dx = b.x - a.x
        val dy = b.y - a.y
        val pxLen = hypot(dx, dy)
        if (pxLen < 1f) continue

        val ux = dx / pxLen
        val uy = dy / pxLen
        val px = -uy   // perpendicular
        val py = ux

        // Halo oscuro debajo: legible sobre paredes claras
        drawLine(HALO, a, b, strokeWidth = haloWidth, cap = StrokeCap.Round)
        drawLine(color, a, b, strokeWidth = lineWidth, cap = StrokeCap.Round)

        // Graduaciones
        val step = chooseTickStep(meters, pxLen)
        if (step != null) {
            var d = step
            while (d < meters - 1e-4f) {
                val f = d / meters
                val tx = a.x + dx * f
                val ty = a.y + dy * f
                drawLine(
                    color.copy(alpha = 0.85f),
                    Offset(tx - px * tickLen, ty - py * tickLen),
                    Offset(tx + px * tickLen, ty + py * tickLen),
                    strokeWidth = lineWidth * 0.55f
                )
                d += step
            }
        }

        // Extremos en T (marcan el punto exacto mejor que un disco)
        drawTCap(a, px, py, capLen, color, lineWidth, haloWidth)
        drawTCap(b, px, py, capLen, color, lineWidth, haloWidth)

        // Etiqueta del tramo, desplazada al lado "de arriba" de la línea
        if (pxLen > 46f) {
            val mx = (a.x + b.x) / 2f + px * (capLen + 6f) * (if (py < 0) 1f else -1f)
            val my = (a.y + b.y) / 2f + py * (capLen + 6f) * (if (py < 0) 1f else -1f)
            painter.draw(canvas, mx, my, labeler(meters))
        }
    }

    // Total del recorrido, junto al último punto
    val last = pts.lastOrNull { it != null }
    if (totalLabel != null && last != null) {
        painter.draw(canvas, last.x, last.y - capLen * 2.6f, "Σ $totalLabel")
    }
}

private fun DrawScope.drawTCap(
    p: Offset, px: Float, py: Float, capLen: Float,
    color: Color, lineWidth: Float, haloWidth: Float
) {
    val s = Offset(p.x - px * capLen, p.y - py * capLen)
    val e = Offset(p.x + px * capLen, p.y + py * capLen)
    drawLine(HALO, s, e, strokeWidth = haloWidth)
    drawLine(color, s, e, strokeWidth = lineWidth)
}

/** Elige un paso de graduación legible: no más de ~20 marcas ni menos de 9 px entre ellas. */
private fun chooseTickStep(meters: Float, pxLen: Float): Float? {
    if (pxLen < 90f || meters < 0.03f) return null
    val candidates = floatArrayOf(0.01f, 0.05f, 0.10f, 0.25f, 0.50f, 1.00f)
    return candidates.firstOrNull { s ->
        val count = meters / s
        count in 1.5f..20f && (pxLen / count) >= 9f
    }
}

private fun DrawScope.drawCrosshair(valid: Boolean, sampling: Boolean) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val len = size.minDimension * 0.035f
    val color = when {
        sampling -> Color(0xFFFFC107)   // ámbar: promediando muestras
        valid -> Color(0xFF4CAF50)
        else -> Color(0xFFBDBDBD)
    }
    val gap = len * 0.35f

    listOf(
        Offset(cx - len, cy) to Offset(cx - gap, cy),
        Offset(cx + gap, cy) to Offset(cx + len, cy),
        Offset(cx, cy - len) to Offset(cx, cy - gap),
        Offset(cx, cy + gap) to Offset(cx, cy + len)
    ).forEach { (p, q) ->
        drawLine(HALO, p, q, strokeWidth = 6f, cap = StrokeCap.Round)
        drawLine(color, p, q, strokeWidth = 3f, cap = StrokeCap.Round)
    }
    drawCircle(HALO, radius = 5.5f, center = Offset(cx, cy))
    drawCircle(color, radius = 3f, center = Offset(cx, cy))
}

/* ═══════════════════════════════════════════════════════════════════
 *  UI AUXILIAR
 * ═══════════════════════════════════════════════════════════════════ */

@Composable
private fun StatusBanner(
    status: ArStatus,
    showDepthHint: Boolean,
    pointsPlaced: Int,
    mode: MeasureMode,
    modifier: Modifier = Modifier
) {
    val message: String = when (status) {
        ArStatus.INIT -> stringResource(R.string.aruler_status_initializing)
        ArStatus.TOO_DARK -> stringResource(R.string.aruler_status_too_dark)
        ArStatus.TOO_FAST -> stringResource(R.string.aruler_status_too_fast)
        ArStatus.NO_FEATURES -> stringResource(R.string.aruler_status_no_features)
        ArStatus.CAMERA_OFF -> stringResource(R.string.aruler_status_camera_off)
        ArStatus.TOO_FAR -> stringResource(R.string.aruler_status_too_far)
        ArStatus.TOO_CLOSE -> stringResource(R.string.aruler_status_too_close)
        ArStatus.UNSTABLE -> stringResource(R.string.aruler_status_unstable)
        ArStatus.ILL_CONDITIONED -> stringResource(R.string.aruler_status_ill_conditioned)
        ArStatus.SAMPLING -> stringResource(R.string.aruler_status_sampling)
        ArStatus.LENS_CHANGED -> stringResource(R.string.aruler_status_lens_changed)
        ArStatus.NO_SURFACE ->
            if (showDepthHint) stringResource(R.string.aruler_hint_move_sideways)
            else stringResource(R.string.aruler_status_no_surface)
        ArStatus.READY -> when {
            pointsPlaced == 0 -> stringResource(R.string.aruler_tip_place_first)
            mode == MeasureMode.SEGMENT -> stringResource(R.string.aruler_tip_place_second)
            else -> stringResource(R.string.aruler_tip_polyline_next)
        }
    }

    val isProblem = status != ArStatus.READY && status != ArStatus.SAMPLING

    Surface(
        modifier = modifier,
        color = if (isProblem) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f)
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        contentColor = if (isProblem) MaterialTheme.colorScheme.onErrorContainer
        else MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 3.dp
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun Section(title: String, bullets: Array<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        bullets.forEach { line -> Text("• $line", style = MaterialTheme.typography.bodyMedium) }
    }
}

private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("Context no es una Activity")
}