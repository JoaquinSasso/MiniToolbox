package com.joasasso.minitoolbox.tools.herramientas.instrumentos.arruler

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.Undo
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import kotlin.math.hypot
import kotlin.math.roundToInt

/* ═══════════════════════════════════════════════════════════════════
 *  CONSTANTES DE CALIDAD Y TRACKING
 * ═══════════════════════════════════════════════════════════════════ */

private const val MAX_HIT_DISTANCE_M = 5.0f
private const val MIN_HIT_DISTANCE_M = 0.20f
private const val CAMERA_JUMP_M = 0.35f
private const val LIVE_BUFFER = 6
private const val LIVE_STABLE_M = 0.04f
private const val ILL_CONDITIONED_COS = 0.94f
private const val ILL_MIN_LEN_M = 0.05f
private const val PLANE_SNAP_MAX_M = 0.08f
private const val NO_SURFACE_FRAMES_FOR_HINT = 90
private const val SAMPLE_TARGET = 8
private const val SAMPLE_MIN = 4
private const val SAMPLE_TIMEOUT_FRAMES = 25
private const val SAMPLE_OUTLIER_M = 0.03f
private const val FOCAL_CHANGE_TOLERANCE = 0.02f
private const val LENS_WARN_FRAMES = 90

/* ═══════════════════════════════════════════════════════════════════
 *  ESTADO DE RENDER AR
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
 *  PANTALLA
 * ═══════════════════════════════════════════════════════════════════ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArRulerSceneViewScreen(
    onBack: () -> Unit,
    vm: ArRulerViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    val overlay = remember { ArOverlay() }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val density = LocalDensity.current

    var showInfo by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)

    // Gestión de anclajes nativos en el ciclo de vida de la vista
    val measurementAnchors = remember { mutableMapOf<Int, Anchor>() }
    var draftAnchor by remember { mutableStateOf<Anchor?>(null) }
    var draftPlane by remember { mutableStateOf<Plane?>(null) }
    var draftFocalPx by remember { mutableFloatStateOf(0f) }

    // Reconciliación declarativa: desvincular anclas removidas
    LaunchedEffect(uiState.measurements) {
        val activeIds = uiState.measurements.map { it.id }.toSet()
        val removedIds = measurementAnchors.keys - activeIds
        removedIds.forEach { id ->
            measurementAnchors.remove(id)?.detach()
        }
    }

    // Si el borrador se cancela o vacía, desvincular el ancla provisional
    LaunchedEffect(uiState.draftLocals.isEmpty()) {
        if (uiState.draftLocals.isEmpty() && draftAnchor != null) {
            draftAnchor?.detach()
            draftAnchor = null
            draftPlane = null
            draftFocalPx = 0f
        }
    }

    // Callbacks de frame
    val viewportState = remember { mutableStateOf(IntSize.Zero) }
    val statusState = remember { mutableStateOf(ArStatus.INIT) }
    val depthHintState = remember { mutableStateOf(false) }
    val status by statusState
    val showDepthHint by depthHintState

    val captureRequested = remember { mutableStateOf(false) }
    val noSurfaceFrames = remember { intArrayOf(0) }
    val lensWarnFrames = remember { intArrayOf(0) }
    val debugTickCounter = remember { intArrayOf(0) }
    val sampler = remember { HitSampler(SAMPLE_TARGET, SAMPLE_MIN, SAMPLE_TIMEOUT_FRAMES) }
    val liveFilter = remember { LiveHitFilter(LIVE_BUFFER, LIVE_STABLE_M) }

    // HUD de diagnóstico
    val debugEnabled = remember { mutableStateOf(false) }
    val debugText = remember { mutableStateOf("") }

    // Paleta
    val activeColor = Color(0xFFFFC107)
    val historyColor = Color(0xFF40C4FF)

    // Métricas de dibujo
    val lineWidthPx = with(density) { 3.dp.toPx() }
    val haloWidthPx = with(density) { 6.dp.toPx() }
    val capLenPx = with(density) { 9.dp.toPx() }
    val tickLenPx = with(density) { 4.dp.toPx() }
    val labelTextPx = with(density) { 14.sp.toPx() }
    val labelPadPx = with(density) { 7.dp.toPx() }

    val activeLabel = remember(labelTextPx) { LabelPainter(labelTextPx, labelPadPx, 0xF0000000.toInt()) }
    val historyLabel = remember(labelTextPx) { LabelPainter(labelTextPx * 0.92f, labelPadPx, 0xB3000000.toInt()) }

    // Pantalla encendida + limpieza integral de anclas nativas al salir
    DisposableEffect(Unit) {
        val activity = runCatching { context.findActivity() }.getOrNull()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            draftAnchor?.detach()
            draftAnchor = null
            draftPlane = null
            measurementAnchors.values.forEach { it.detach() }
            measurementAnchors.clear()
            vm.clearAll()
        }
    }

    Scaffold(
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
                            vm.changeMode(if (uiState.mode == MeasureMode.SEGMENT) MeasureMode.POLYLINE else MeasureMode.SEGMENT)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        label = {
                            Text(
                                if (uiState.mode == MeasureMode.SEGMENT) stringResource(R.string.aruler_mode_segment)
                                else stringResource(R.string.aruler_mode_polyline)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (uiState.mode == MeasureMode.SEGMENT) Icons.Rounded.Straighten else Icons.Rounded.Timeline,
                                contentDescription = null
                            )
                        }
                    )

                    AssistChip(
                        enabled = uiState.canUndo,
                        onClick = {
                            if (vm.undo()) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        label = { Text(stringResource(R.string.aruler_undo)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null) }
                    )

                    AssistChip(
                        onClick = { vm.toggleUnits() },
                        label = {
                            Text(
                                if (uiState.unitSystem == Units.METRIC) stringResource(R.string.aruler_units_metric)
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

                AnimatedVisibility(visible = uiState.measurements.isNotEmpty() && uiState.draftLocals.isEmpty()) {
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

                AnimatedVisibility(visible = uiState.canFinish) {
                    SmallFloatingActionButton(
                        onClick = {
                            val committedId = vm.commitDraft()
                            if (committedId != null) {
                                draftAnchor?.let { measurementAnchors[committedId] = it }
                                draftAnchor = null
                                draftPlane = null
                                draftFocalPx = 0f
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
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

            ARScene(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                modelLoader = modelLoader,
                sessionConfiguration = { session: Session, config: Config ->
                    config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    config.depthMode =
                        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) Config.DepthMode.AUTOMATIC
                        else Config.DepthMode.DISABLED
                    config.focusMode = Config.FocusMode.AUTO
                    config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                    config.lightEstimationMode = Config.LightEstimationMode.DISABLED
                },
                onSessionUpdated = { session, frame ->
                    onFrame(
                        session = session,
                        frame = frame,
                        viewportState = viewportState,
                        overlay = overlay,
                        uiState = uiState,
                        vm = vm,
                        getDraftAnchor = { draftAnchor },
                        setDraftAnchor = { draftAnchor = it },
                        getDraftPlane = { draftPlane },
                        setDraftPlane = { draftPlane = it },
                        getDraftFocalPx = { draftFocalPx },
                        setDraftFocalPx = { draftFocalPx = it },
                        onMeasurementCommitted = { id, anchor -> measurementAnchors[id] = anchor },
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

            Canvas(Modifier.fillMaxSize()) {
                val frameTick = overlay.tick
                if (frameTick < 0 || !overlay.ready || size.width < 1f) return@Canvas

                val vp = IntSize(size.width.roundToInt(), size.height.roundToInt())

                // Mediciones confirmadas
                uiState.measurements.forEach { m ->
                    val anchor = measurementAnchors[m.id] ?: return@forEach
                    if (anchor.trackingState != TrackingState.TRACKING) return@forEach
                    val pose = anchor.pose
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

                // Medición en curso + preview vivo
                val currentAnchor = draftAnchor
                val locals = uiState.draftLocals
                if (currentAnchor != null && locals.isNotEmpty() &&
                    currentAnchor.trackingState == TrackingState.TRACKING
                ) {
                    val pose = currentAnchor.pose
                    val worlds = locals.map { pose.transformPoint(it) }.toMutableList()
                    val segs = worlds.zipWithNext { a, b -> dist3(a, b) }.toMutableList()

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

                drawCrosshair(valid = status == ArStatus.READY || overlay.sampling, sampling = overlay.sampling)
            }

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
                    pointsPlaced = uiState.draftLocals.size,
                    mode = uiState.mode,
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
 * ═══════════════════════════════════════════════════════════════════ */

private fun onFrame(
    session: Session,
    frame: Frame,
    viewportState: MutableState<IntSize>,
    overlay: ArOverlay,
    uiState: ArRulerUiState,
    vm: ArRulerViewModel,
    getDraftAnchor: () -> Anchor?,
    setDraftAnchor: (Anchor?) -> Unit,
    getDraftPlane: () -> Plane?,
    setDraftPlane: (Plane?) -> Unit,
    getDraftFocalPx: () -> Float,
    setDraftFocalPx: (Float) -> Unit,
    onMeasurementCommitted: (Int, Anchor) -> Unit,
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

    camera.getViewMatrix(overlay.view, 0)
    camera.getProjectionMatrix(overlay.proj, 0, 0.01f, 100f)
    overlay.ready = true

    val camPos = camera.pose.translation
    if (overlay.prevCamValid) camJump = dist3(overlay.prevCam, camPos)
    overlay.prevCam[0] = camPos[0]; overlay.prevCam[1] = camPos[1]; overlay.prevCam[2] = camPos[2]
    overlay.prevCamValid = true

    val focalPx = runCatching { camera.imageIntrinsics.focalLength[0] }.getOrDefault(0f)
    val draftFocal = getDraftFocalPx()
    val focalChanged = draftFocal > 0f && focalPx > 0f &&
            abs(focalPx - draftFocal) / draftFocal > FOCAL_CHANGE_TOLERANCE

    if (uiState.draftLocals.isNotEmpty() && (camJump > CAMERA_JUMP_M || focalChanged)) {
        vm.cancelDraft()
        getDraftAnchor()?.detach()
        setDraftAnchor(null)
        setDraftPlane(null)
        setDraftFocalPx(0f)
        sampler.stop()
        overlay.sampling = false
        captureRequested.value = false
        lensWarnFrames[0] = LENS_WARN_FRAMES
        publish(ArStatus.LENS_CHANGED, false)
        overlay.bump()
        return
    }

    val depthOn = runCatching { session.config.depthMode != Config.DepthMode.DISABLED }.getOrDefault(false)
    val currentDraftPlane = getDraftPlane()
    val hit = bestHit(
        frame, viewport.width / 2f, viewport.height / 2f,
        preferredPlane = currentDraftPlane,
        allowFeaturePoints = !depthOn
    )

    val geomStatus = when {
        hit == null -> ArStatus.NO_SURFACE
        hit.distance > MAX_HIT_DISTANCE_M -> ArStatus.TOO_FAR
        hit.distance < MIN_HIT_DISTANCE_M -> ArStatus.TOO_CLOSE
        else -> ArStatus.READY
    }

    if (geomStatus == ArStatus.NO_SURFACE) noSurfaceFrames[0]++ else noSurfaceFrames[0] = 0

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

    var illConditioned = false
    val anchorForGeom = getDraftAnchor()
    if (overlay.liveValid && anchorForGeom != null && uiState.draftLocals.isNotEmpty() &&
        anchorForGeom.trackingState == TrackingState.TRACKING
    ) {
        val a = anchorForGeom.pose.transformPoint(uiState.draftLocals.last())
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

    if (captureRequested.value && !sampler.active) {
        captureRequested.value = false
        sampler.start()
    }

    if (sampler.active) {
        sampler.advanceFrame()
        if (usable) sampler.addSample(hit!!.hitPose.translation)

        if (sampler.isComplete) {
            if (sampler.hasEnoughSamples && usable) {
                val raw = robustCentroid(sampler.samples, SAMPLE_OUTLIER_M)
                val plane = bestPlaneFor(session, raw, currentDraftPlane)
                val world = plane?.let { snapToPlane(raw, it) } ?: raw
                lastSnapInfo[0] = if (plane == null) "no"
                else if (plane.type == Plane.Type.VERTICAL) "VERT" else "HORIZ"

                var currentAnchor = getDraftAnchor()
                if (currentAnchor == null) {
                    currentAnchor = runCatching { hit!!.createAnchor() }.getOrNull()
                    if (currentAnchor != null) {
                        setDraftAnchor(currentAnchor)
                        setDraftPlane(plane)
                        setDraftFocalPx(focalPx)
                        val local = currentAnchor.pose.inverse().transformPoint(world)
                        vm.addPoint(local)
                    }
                } else if (currentAnchor.trackingState == TrackingState.TRACKING) {
                    val local = currentAnchor.pose.inverse().transformPoint(world)
                    val committedId = vm.addPoint(local)
                    if (committedId != null) {
                        onMeasurementCommitted(committedId, currentAnchor)
                        setDraftAnchor(null)
                        setDraftPlane(null)
                        setDraftFocalPx(0f)
                    }
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

    if (debugEnabled.value) {
        debugTickCounter[0]++
        if (debugTickCounter[0] % 15 == 0) {
            debugText.value = buildDebugText(session, hit, focalPx, camJump, currentDraftPlane != null, liveFilter.spread)
        }
    }

    overlay.bump()
}

private fun snapToPlane(world: FloatArray, plane: Plane): FloatArray? {
    if (plane.trackingState != TrackingState.TRACKING) return null
    val pose = plane.centerPose
    return snapToPlaneEquation(world, pose.yAxis, pose.translation, PLANE_SNAP_MAX_M)
}

private fun bestPlaneFor(session: Session, world: FloatArray, preferred: Plane?): Plane? {
    if (preferred != null && snapToPlane(world, preferred) != null) return preferred

    var best: Plane? = null
    var bestDist = PLANE_SNAP_MAX_M
    runCatching {
        session.getAllTrackables(Plane::class.java).forEach { plane ->
            if (plane.trackingState != TrackingState.TRACKING) return@forEach
            if (plane.subsumedBy != null) return@forEach
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

private fun bestHit(
    frame: Frame,
    x: Float,
    y: Float,
    preferredPlane: Plane?,
    allowFeaturePoints: Boolean
): HitResult? {
    val hits = runCatching { frame.hitTest(x, y) }.getOrNull() ?: return null
    if (hits.isEmpty()) return null

    if (preferredPlane != null) {
        hits.firstOrNull { it.trackable === preferredPlane }?.let { return it }
    }

    hits.firstOrNull { h ->
        val t = h.trackable
        t is Plane && t.isPoseInPolygon(h.hitPose) && t.trackingState == TrackingState.TRACKING
    }?.let { return it }

    hits.firstOrNull { it.trackable is DepthPoint }?.let { return it }

    if (!allowFeaturePoints) return null

    return hits.firstOrNull { h ->
        val t = h.trackable
        t is Point && t.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
    }
}

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

    if (cw <= 1e-6f) return null
    if (cz > cw) return null

    val ndcX = cx / cw
    val ndcY = cy / cw
    return Offset(
        (ndcX * 0.5f + 0.5f) * viewport.width,
        (1f - (ndcY * 0.5f + 0.5f)) * viewport.height
    )
}

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
        val px = -uy
        val py = ux

        drawLine(HALO, a, b, strokeWidth = haloWidth, cap = StrokeCap.Round)
        drawLine(color, a, b, strokeWidth = lineWidth, cap = StrokeCap.Round)

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

        drawTCap(a, px, py, capLen, color, lineWidth, haloWidth)
        drawTCap(b, px, py, capLen, color, lineWidth, haloWidth)

        if (pxLen > 46f) {
            val mx = (a.x + b.x) / 2f + px * (capLen + 6f) * (if (py < 0) 1f else -1f)
            val my = (a.y + b.y) / 2f + py * (capLen + 6f) * (if (py < 0) 1f else -1f)
            painter.draw(canvas, mx, my, labeler(meters))
        }
    }

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

private fun DrawScope.drawCrosshair(valid: Boolean, sampling: Boolean) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val len = size.minDimension * 0.035f
    val color = when {
        sampling -> Color(0xFFFFC107)
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
