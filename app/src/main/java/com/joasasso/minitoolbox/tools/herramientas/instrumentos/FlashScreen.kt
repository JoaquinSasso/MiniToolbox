package com.joasasso.minitoolbox.tools.herramientas.instrumentos

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.joasasso.minitoolbox.ui.components.TopBarReusable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var linternaEncendida by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableIntStateOf(0) }

    // Función para encender/apagar el flash
    fun setLinterna(encendida: Boolean) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, encendida)
                linternaEncendida = encendida
                errorMsg = 0
            } else {
                errorMsg = R.string.flash_error_noflash
            }
        } catch (_: CameraAccessException) {
            errorMsg = R.string.flash_error_camera_access
        } catch (_: SecurityException) {
            errorMsg = R.string.flash_error_permission
        }
    }

    // Apagar linterna al salir de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            setLinterna(false)
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
                        if (linternaEncendida)
                            R.string.flash_on_desc
                        else
                            R.string.flash_off_desc
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
                        setLinterna(!linternaEncendida)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    modifier = Modifier.width(200.dp)
                ) {
                    Text(
                        stringResource(
                            if (linternaEncendida)
                                R.string.flash_button_off
                            else
                                R.string.flash_button_on
                        )
                    )
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