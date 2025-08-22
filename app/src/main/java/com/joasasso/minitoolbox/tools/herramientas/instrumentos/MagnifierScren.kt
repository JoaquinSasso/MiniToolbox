@file:OptIn(ExperimentalMaterial3Api::class)

package com.joasasso.minitoolbox.tools.magnifier

import android.Manifest
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.os.Build
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.TorchState
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import java.util.concurrent.TimeUnit

@Composable
fun MagnifierScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

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
            // AE/AF automáticos por defecto
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }
    LaunchedEffect(hasCamPerm) {
        if (hasCamPerm) controller.bindToLifecycle(lifecycleOwner)
    }

    // Observables
    val torchState by rememberTorchState(controller)
    val zoomSnapshot by rememberZoomState(controller)

    // PreviewView ref para capturar frame al pausar
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var frozenBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val isFrozen = frozenBitmap != null

    // Ayuda
    var showInfo by remember { mutableStateOf(false) }

    // Filtro automático: alto contraste + leve brillo (sin sliders)
    // Contraste 1.35, brillo +10 ≈ mejora legibilidad de texto impreso frecuente
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

    // Acción pausar/reanudar (captura con PreviewView.bitmap)
    fun toggleFreeze() {
        if (isFrozen) {
            frozenBitmap = null
            return
        }
        val pv = previewView ?: return

        // Asegura COMPATIBLE para tener TextureView detrás de escena y habilitar getBitmap()
        pv.implementationMode = PreviewView.ImplementationMode.COMPATIBLE

        // Intento inmediato
        pv.bitmap?.let { bmp ->
            // Copia inmutable a ARGB_8888
            frozenBitmap = bmp.copy(Bitmap.Config.ARGB_8888, /*isMutable=*/ false)
            return
        }

        // Si todavía no hay frame (bitmap == null), probamos en el siguiente loop del UI
        pv.post {
            pv.bitmap?.let { bmp2 ->
                frozenBitmap = bmp2.copy(Bitmap.Config.ARGB_8888, false)
            }
        }
    }


    Scaffold(
        topBar = { TopBarReusable(title = stringResource(R.string.magnifier_title), onBack = onBack, onShowInfo = {showInfo = true}) }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(Color.Black)
        ) {
            // Capa de cámara (PreviewView)
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        // Pinch to zoom
                        detectTransformGestures { _, _, zoomChange, _ ->
                            if (!isFrozen) {
                                zoomSnapshot?.let { zs ->
                                    val newRatio = (zs.zoomRatio * zoomChange)
                                        .coerceIn(zs.minZoomRatio, zs.maxZoomRatio)
                                    controller.setZoomRatio(newRatio)
                                }
                            }
                        }
                    },
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        // 1.4.x: propiedad controller; si usas 1.2.x cambia a setController(controller)
                        this.controller = controller

                        // Tap‑to‑focus + AE
                        setOnTouchListener { v, event ->
                            if (!isFrozen && event.action == MotionEvent.ACTION_UP) {
                                val factory: MeteringPointFactory =
                                    SurfaceOrientedMeteringPointFactory(width.toFloat(), height.toFloat())
                                val pt = factory.createPoint(event.x, event.y)
                                val action = FocusMeteringAction.Builder(pt)
                                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                    .build()
                                controller.cameraControl?.startFocusAndMetering(action)
                                v?.performClick() // accesibilidad
                                return@setOnTouchListener true
                            }
                            false
                        }

                        // Filtro de alto contraste automático (API 31+)
                        if (Build.VERSION.SDK_INT >= 31) {
                            setRenderEffect(RenderEffect.createColorFilterEffect(autoColorFilter()))
                        }
                    }.also { previewView = it }
                },
                update = { view ->
                    // Reaplicar filtro por si cambia algo del ciclo de vida
                    if (Build.VERSION.SDK_INT >= 31) {
                        view.setRenderEffect(RenderEffect.createColorFilterEffect(autoColorFilter()))
                    }
                    // Garantiza que el controller siga asignado
                    view.controller = controller
                }
            )

            // Si está congelado, superponemos el bitmap
            frozenBitmap?.let { bmp ->
                androidx.compose.foundation.Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Controles inferiores: Torch, Zoom, Pausa
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Torch + Pausar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { controller.enableTorch(torchState != TorchState.ON) },
                        enabled = !isFrozen // opcional: deshabilitar linterna cuando está congelado
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

                    FilledTonalButton(onClick = { toggleFreeze() }) {
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

                // Slider de Zoom (único control que queda)
                zoomSnapshot?.let { z ->
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.zoom_label, String.format("%.1f", z.zoomRatio)),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Slider(
                            value = z.zoomRatio,
                            onValueChange = { if (!isFrozen) controller.setZoomRatio(it) },
                            valueRange = z.minZoomRatio..z.maxZoomRatio
                        )
                    }
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
                TextButton(onClick = { showInfo = false }) {
                    Text(stringResource(R.string.help_ok))
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
