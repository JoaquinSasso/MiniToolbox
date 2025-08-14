package com.joasasso.minitoolbox.tools.herramientas.instrumentos

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.flujoNivelLinterna
import com.joasasso.minitoolbox.data.setNivelLinterna
import com.joasasso.minitoolbox.ui.components.TopBarReusable
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val cameraManager = remember { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }

    var linternaEncendida by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableIntStateOf(0) }

    // Soporte de intensidad (Android 13+)
    var cameraId by remember { mutableStateOf<String?>(null) }
    var maxStrength by remember { mutableIntStateOf(1) } // 1 = solo ON/OFF
    val soportaIntensidad = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && maxStrength > 1

    // Slider 0..20 con "guard band" para el 0 (apagado)
    var rawSlider by remember { mutableFloatStateOf(10f) } // valor crudo 0..20
    var uiLevel by remember { mutableIntStateOf(10) }      // nivel mostrado 0..20
    var lastApplied by remember { mutableIntStateOf(uiLevel) }

    // Arrastrar "un poquito más" para aceptar 0 al soltar
    val zeroGuard = 0.6f


    // === Cargar nivel persistido al entrar ===
    LaunchedEffect(Unit) {
        // leer cámara/soporte
        try {
            val id = cameraManager.cameraIdList.firstOrNull { cid ->
                cameraManager.getCameraCharacteristics(cid)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            cameraId = id
            if (id == null) {
                errorMsg = R.string.flash_error_noflash
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val ch = cameraManager.getCameraCharacteristics(id)
                maxStrength = ch.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
            }
        } catch (_: CameraAccessException) {
            errorMsg = R.string.flash_error_camera_access
        } catch (_: SecurityException) {
            errorMsg = R.string.flash_error_permission
        }

        // leer nivel guardado
        context.flujoNivelLinterna().collect { saved ->
            // Inicializar UI con el nivel persistido
            uiLevel = saved.coerceIn(0, 20)
            rawSlider = uiLevel.toFloat()
            // Por comodidad, si el guardado es >0, recordarlo como "último aplicado"
            if (uiLevel > 0) lastApplied = uiLevel
            // detener la recolección tras el primer valor
            return@collect
        }
    }

    // Torch callback para reflejar estado real
    DisposableEffect(cameraId) {
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(id: String, enabled: Boolean) {
                if (id == cameraId) linternaEncendida = enabled
            }
        }
        cameraManager.registerTorchCallback(callback, null)

        onDispose {
            try {
                cameraId?.let { cameraManager.setTorchMode(it, false) } // apagar al salir
            } catch (_: Exception) { /* ignore */ }
            cameraManager.unregisterTorchCallback(callback)
        }
    }

    // Mapear 1..20 -> 1..maxStrength (lineal). 0 se maneja fuera (apagado)
    fun mapUiToStrength(level: Int, max: Int): Int {
        if (level <= 0) return 1
        if (max <= 1) return 1
        val frac = (level - 1f) / 19f
        return (1 + frac * (max - 1)).roundToInt().coerceIn(1, max)
    }

    // Encender/apagar sin intensidad (fallback)
    fun setLinterna(encendida: Boolean) {
        try {
            val id = cameraId ?: run { errorMsg = R.string.flash_error_noflash; return }
            cameraManager.setTorchMode(id, encendida)
            linternaEncendida = encendida
            errorMsg = 0
        } catch (_: CameraAccessException) {
            errorMsg = R.string.flash_error_camera_access
        } catch (_: SecurityException) {
            errorMsg = R.string.flash_error_permission
        }
    }

    // Aplicar nivel elegido:
    // - Si soporta intensidad: 0 apaga, 1..20 mapean a 1..maxStrength.
    // - Si NO soporta: 0 apaga, 1..20 encienden (ON/OFF).
    fun applyLevel(level: Int) {
        if (!soportaIntensidad) {
            if (level <= 0) setLinterna(false) else setLinterna(true)
            return
        }
        val id = cameraId ?: return
        if (level <= 0) {
            setLinterna(false)
            return
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val strength = mapUiToStrength(level, maxStrength)
                cameraManager.turnOnTorchWithStrengthLevel(id, strength)
                linternaEncendida = true
                errorMsg = 0
            }
        } catch (_: CameraAccessException) {
            errorMsg = R.string.flash_error_camera_access
        } catch (_: SecurityException) {
            errorMsg = R.string.flash_error_permission
        } catch (_: IllegalArgumentException) {
            errorMsg = R.string.flash_error_camera_access
        }
    }

    Scaffold(
        topBar = {
            TopBarReusable(
                stringResource(R.string.tool_flashlight),
                onBack,
                { showInfo = true }
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    if (linternaEncendida) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                    contentDescription = stringResource(
                        if (linternaEncendida) R.string.flash_on_desc else R.string.flash_off_desc
                    ),
                    tint = if (linternaEncendida)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(90.dp)
                )

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (linternaEncendida) {
                            applyLevel(0) // apagar
                        } else {
                            // Si el usuario está en 0, encendemos con el último aplicado o un nivel cómodo
                            val target = if (uiLevel == 0) (lastApplied.takeIf { it > 0 } ?: 10) else uiLevel
                            applyLevel(target)
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    modifier = Modifier.width(200.dp)
                ) {
                    Text(
                        stringResource(
                            if (linternaEncendida) R.string.flash_button_off else R.string.flash_button_on
                        )
                    )
                }

                // === Slider (0..20) con 0 protegido durante el arrastre ===
                Spacer(Modifier.height(20.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (uiLevel == 0)
                            stringResource(R.string.flash_intensity_off)
                        else
                            "${stringResource(R.string.flash_intensity)} $uiLevel/20",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))

                    Slider(
                        value = rawSlider,
                        onValueChange = { v ->
                            rawSlider = v.coerceIn(0f, 20f)
                            // Durante el arrastre, 0 se “protege”: se muestra como 1 hasta que sueltes
                            val previewLevel =
                                if (rawSlider < zeroGuard) 1 else rawSlider.roundToInt().coerceIn(0, 20)
                            if (previewLevel != uiLevel) {
                                uiLevel = previewLevel
                                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            }
                            val finalLevel =
                                if (rawSlider < zeroGuard) 0 else rawSlider.roundToInt().coerceIn(0, 20)

                            if (finalLevel != lastApplied) {
                                applyLevel(finalLevel)
                                lastApplied = finalLevel
                                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            }
                            // Reflejar visualmente el 0 si quedó en la zona guard
                            uiLevel = finalLevel
                            rawSlider = finalLevel.toFloat().coerceIn(0f, 20f)
                        },
                        onValueChangeFinished = {
                            val safe = uiLevel.coerceIn(0, 20)
                            // Guardar el nivel elegido a menos que sea 0 (aunque el equipo no soporte intensidad)
                            if(safe > 0) {
                                scope.launch { context.setNivelLinterna(safe) }
                            }
                        },
                        valueRange = 0f..20f,
                        steps = 19,
                        enabled = true, // útil aun sin niveles (0=off,1..20=on)
                        modifier = Modifier.width(260.dp)
                    )

                    if (!soportaIntensidad) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.flash_error_intensity),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (errorMsg != 0) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        stringResource(errorMsg),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = {
                showInfo = false
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            title = { Text(stringResource(R.string.flash_help_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.flash_help_line1))
                    Text(stringResource(R.string.flash_help_line2))
                    Text(stringResource(R.string.flash_help_line3))
                    Text(stringResource(R.string.flash_help_line4))
                    Text(stringResource(R.string.flash_help_line5))
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

