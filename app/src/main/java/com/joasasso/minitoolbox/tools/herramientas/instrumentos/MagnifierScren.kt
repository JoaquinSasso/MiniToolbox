@file:OptIn(ExperimentalMaterial3Api::class)

package com.joasasso.minitoolbox.tools.herramientas.instrumentos

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RenderEffect
import android.os.Build
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.TorchState
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun MagnifierScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    // --- Permiso de cámara ---
    var hasCamPerm by remember { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCamPerm = granted }

    LaunchedEffect(Unit) {
        hasCamPerm = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasCamPerm) permLauncher.launch(Manifest.permission.CAMERA)
    }

    // --- CameraX controller ---
    val controller = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }
    val minRatio = 1f
    LaunchedEffect(hasCamPerm) {
        if (hasCamPerm) {
            controller.bindToLifecycle(lifecycleOwner)
            // En cuanto haya un ZoomState, aseguremos 1x como mínimo inicial
            val cur = controller.zoomState.value?.zoomRatio ?: 1f
            if (cur < minRatio) controller.setZoomRatio(minRatio)
        }
    }

    // Estados observables
    val torchState by rememberTorchState(controller)
    val zoomState by rememberZoomState(controller) // ZoomState? (zoomRatio + linearZoom)

    // PreviewView y pausa
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var frozenBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val isFrozen = frozenBitmap != null

    // Tamaño del contenedor para clamp en pausa
    var containerSize by remember { mutableStateOf(IntSize(0, 0)) }

    // Gestos sobre imagen congelada (zoom + pan)
    var frozenScale by remember { mutableStateOf(1f) }
    var frozenOffsetX by remember { mutableStateOf(0f) }
    var frozenOffsetY by remember { mutableStateOf(0f) }
    val minScale = 1f
    val maxScale = 6f

    // Ayuda
    var showInfo by remember { mutableStateOf(false) }

    // Back: si está pausada, reanudar; si no, salir
    BackHandler {
        if (isFrozen) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            frozenBitmap = null
            frozenScale = 1f
            frozenOffsetX = 0f
            frozenOffsetY = 0f
        } else {
            onBack()
        }
    }

    // --- Filtro automático: alto contraste + leve brillo ---
    fun autoColorFilter(): ColorMatrixColorFilter {
        val contrast = 1.35f
        val brightness = 10f
        val t = 128f * (1f - contrast) + brightness
        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f,       0f,       0f, t,
                0f,       contrast, 0f,       0f, t,
                0f,       0f,       contrast, 0f, t,
                0f,       0f,       0f,       1f, 0f
            )
        )
        return ColorMatrixColorFilter(cm)
    }

    fun applyColorFilterToBitmap(src: Bitmap, filter: ColorMatrixColorFilter): Bitmap {
        val out = createBitmap(src.width, src.height)
        val c = Canvas(out)
        val p = Paint().apply { colorFilter = filter }
        c.drawBitmap(src, 0f, 0f, p)
        return out
    }

    // Pausar / Reanudar
    fun toggleFreeze() {
        if (isFrozen) {
            frozenBitmap = null
            frozenScale = 1f
            frozenOffsetX = 0f
            frozenOffsetY = 0f
            return
        }
        val pv = previewView ?: return
        pv.implementationMode = PreviewView.ImplementationMode.COMPATIBLE

        fun captureAndFilter(bmp: Bitmap?) {
            if (bmp == null) return
            val base = bmp.copy(Bitmap.Config.ARGB_8888, false)
            frozenBitmap = applyColorFilterToBitmap(base, autoColorFilter())
            frozenScale = 1f
            frozenOffsetX = 0f
            frozenOffsetY = 0f
        }

        pv.bitmap?.let { captureAndFilter(it); return }
        pv.post { captureAndFilter(pv.bitmap) }
    }

    Scaffold(
        topBar = {
            TopBarReusable(
                title = stringResource(R.string.magnifier_title),
                onBack = {
                    if (isFrozen) {
                        frozenBitmap = null
                        frozenScale = 1f
                        frozenOffsetX = 0f
                        frozenOffsetY = 0f
                    } else {
                        onBack()
                    }
                },
                onShowInfo = { showInfo = true }
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(Color.Black)
                .onSizeChanged { containerSize = it }
        ) {
            // --- Preview de cámara ---
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val pv = PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        this.controller = controller

                        // Detector de pinch-to-zoom nativo (fluido)
                        val scaleDetector = ScaleGestureDetector(ctx,
                            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                                override fun onScale(detector: ScaleGestureDetector): Boolean {
                                    if (isFrozen) return false
                                    val z = controller.zoomState.value ?: return false
                                    val deviceMax = z.maxZoomRatio
                                    val newRatio = (z.zoomRatio * detector.scaleFactor)
                                        .coerceIn(minRatio, deviceMax)
                                    controller.setZoomRatio(newRatio)
                                    return true
                                }
                            })

                        // Tap corto → pausar/reanudar ; Long‑press → focus+AE (solo si NO está pausado)
                        setOnTouchListener { v, event ->
                            // Alimentar el detector de escala SIEMPRE
                            scaleDetector.onTouchEvent(event)

                            when (event.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    v.tag = TouchState(
                                        downX = event.x,
                                        downY = event.y,
                                        downTime = System.currentTimeMillis(),
                                        longPressTriggered = false
                                    )
                                    if (!isFrozen) {
                                        v.postDelayed({
                                            val st = v.tag as? TouchState ?: return@postDelayed
                                            if (!st.longPressTriggered) {
                                                val factory: MeteringPointFactory =
                                                    SurfaceOrientedMeteringPointFactory(
                                                        width.toFloat(), height.toFloat()
                                                    )
                                                val pt = factory.createPoint(st.downX, st.downY)
                                                val action = FocusMeteringAction.Builder(pt)
                                                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                                    .build()
                                                controller.cameraControl?.startFocusAndMetering(action)
                                                (v.tag as? TouchState)?.longPressTriggered = true
                                            }
                                        }, 300)
                                    }
                                    true
                                }
                                MotionEvent.ACTION_UP -> {
                                    val st = v.tag as? TouchState
                                    val dt = System.currentTimeMillis() - (st?.downTime ?: 0L)
                                    val dx = (event.x - (st?.downX ?: event.x))
                                    val dy = (event.y - (st?.downY ?: event.y))
                                    val threshold = 12f * resources.displayMetrics.density
                                    val moved = (dx * dx + dy * dy) > (threshold * threshold)

                                    if (st?.longPressTriggered == true) {
                                        v.performClick()
                                        return@setOnTouchListener true
                                    }

                                    if (!moved && dt < 200) {
                                        toggleFreeze()
                                        v.performClick()
                                        return@setOnTouchListener true
                                    }
                                    false
                                }
                                else -> false
                            }
                        }

                        // Filtro alto‑contraste en preview
                        if (Build.VERSION.SDK_INT >= 31) {
                            setRenderEffect(RenderEffect.createColorFilterEffect(autoColorFilter()))
                        }
                    }
                    previewView = pv
                    pv
                },
                update = { view ->
                    if (Build.VERSION.SDK_INT >= 31) {
                        view.setRenderEffect(RenderEffect.createColorFilterEffect(autoColorFilter()))
                    }
                    view.controller = controller
                }
            )

            // --- Overlay pausado: transformable fluido + clamp + tap para reanudar ---
            frozenBitmap?.let { bmp ->
                // transformableState entrega cambios continuos de zoom/pan
                val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                    val newScale = (frozenScale * zoomChange).coerceIn(minScale, maxScale)

                    // aplicar pan incremental y luego clamp según escala
                    var nx = frozenOffsetX + panChange.x
                    var ny = frozenOffsetY + panChange.y

                    val contW = containerSize.width.toFloat().coerceAtLeast(1f)
                    val contH = containerSize.height.toFloat().coerceAtLeast(1f)
                    val scaledW = contW * newScale
                    val scaledH = contH * newScale
                    val maxTx = max(0f, (scaledW - contW) / 2f)
                    val maxTy = max(0f, (scaledH - contH) / 2f)

                    nx = nx.coerceIn(-maxTx, maxTx)
                    ny = ny.coerceIn(-maxTy, maxTy)

                    frozenScale = newScale
                    frozenOffsetX = nx
                    frozenOffsetY = ny
                }

                // Re‑clamp ante cambios de escala/tamaño
                LaunchedEffect(frozenScale, containerSize) {
                    val contW = containerSize.width.toFloat().coerceAtLeast(1f)
                    val contH = containerSize.height.toFloat().coerceAtLeast(1f)
                    val scaledW = contW * frozenScale
                    val scaledH = contH * frozenScale
                    val maxTx = max(0f, (scaledW - contW) / 2f)
                    val maxTy = max(0f, (scaledH - contH) / 2f)
                    frozenOffsetX = frozenOffsetX.coerceIn(-maxTx, maxTx)
                    frozenOffsetY = frozenOffsetY.coerceIn(-maxTy, maxTy)
                }

                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        // Tap corto → reanudar
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { toggleFreeze() })
                        }
                        // Gestos multitouch fluidos (zoom + pan)
                        .transformable(transformState)
                        .graphicsLayer {
                            scaleX = frozenScale
                            scaleY = frozenScale
                            translationX = frozenOffsetX
                            translationY = frozenOffsetY
                        }
                )
            }

            // --- Controles inferiores ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            controller.enableTorch(torchState != TorchState.ON)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        enabled = !isFrozen
                    ) {
                        if (torchState == TorchState.ON) {
                            Icon(Icons.Filled.FlashlightOn, contentDescription = stringResource(R.string.cd_torch_off))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.torch_on))
                        } else {
                            Icon(Icons.Filled.FlashlightOff, contentDescription = stringResource(R.string.cd_torch_on))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.torch_off))
                        }
                    }

                    FilledTonalButton(onClick = {
                        toggleFreeze()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }) {
                        if (isFrozen) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.cd_resume))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_resume))
                        } else {
                            Icon(Icons.Filled.Pause, contentDescription = stringResource(R.string.cd_freeze))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_freeze))
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Slider de zoom (preview activa)
                if (!isFrozen) {
                    val deviceMax = zoomState?.maxZoomRatio ?: 8f
                    val minRatio = 1f
                    val allowedMax = minOf(deviceMax, 8f)
                    val ratioFromState = (zoomState?.zoomRatio ?: minRatio).coerceIn(minRatio, allowedMax)

                    // guardamos “ticks” como enteros de décimas (p. ej. 2.3x -> 23)
                    var lastTenths by remember { mutableIntStateOf((ratioFromState * 10f).roundToInt()) }
                    var lastInt    by remember { mutableIntStateOf(ratioFromState.roundToInt()) }

                    Text(
                        text = stringResource(R.string.zoom_label, String.format("%.1f", ratioFromState)),
                        style = MaterialTheme.typography.labelLarge
                    )

                    Slider(
                        value = ratioFromState,
                        onValueChange = { raw ->
                            val target = raw.coerceIn(minRatio, allowedMax)
                            controller.setZoomRatio(target)

                            val tenths = (target * 10f).roundToInt()                    // décima actual
                            if (tenths != lastTenths) {                                 // cambió la décima
                                if (tenths % 10 == 0) {                                 // es entero exacto (2.0x, 3.0x, …)
                                    val currentInt = tenths / 10
                                    if (currentInt != lastInt) {                        // evita doble disparo en el mismo entero
                                        haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                        lastInt = currentInt
                                    }
                                } else {
                                    haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                                }
                                lastTenths = tenths
                            }
                        },
                        valueRange = minRatio..allowedMax
                    )
                }

            }
        }
    }

    // --- Menú de ayuda ---
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(stringResource(R.string.help_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.help_line1))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.help_line2))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.help_line3))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.help_line4))
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfo = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

/* ---------- Helpers para observar LiveData de controller ---------- */

@Composable
private fun rememberTorchState(controller: LifecycleCameraController): State<Int> {
    return produceState(initialValue = TorchState.OFF, controller) {
        val ld = controller.torchState
        val obs = androidx.lifecycle.Observer<Int> { value = it }
        ld.observeForever(obs)
        awaitDispose { ld.removeObserver(obs) }
    }
}

@Composable
private fun rememberZoomState(controller: LifecycleCameraController): State<androidx.camera.core.ZoomState?> {
    return produceState(initialValue = controller.zoomState.value, controller) {
        val ld = controller.zoomState
        val obs = androidx.lifecycle.Observer<androidx.camera.core.ZoomState> { value = it }
        ld.observeForever(obs)
        awaitDispose { ld.removeObserver(obs) }
    }
}

/* ---------- Soporte tap/long‑press en PreviewView ---------- */
private data class TouchState(
    val downX: Float,
    val downY: Float,
    val downTime: Long,
    var longPressTriggered: Boolean
)
