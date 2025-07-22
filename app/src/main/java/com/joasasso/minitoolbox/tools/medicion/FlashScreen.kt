package com.joasasso.minitoolbox.tools.medicion

import android.content.Context
import android.hardware.camera2.CameraAccessException
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joasasso.minitoolbox.ui.components.TopBarReusable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var linternaEncendida by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Función para encender/apagar el flash
    fun setLinterna(encendida: Boolean) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, encendida)
                linternaEncendida = encendida
                errorMsg = null
            } else {
                errorMsg = "No se encontró flash en este dispositivo."
            }
        } catch (e: CameraAccessException) {
            errorMsg = "No se pudo acceder al flash de la cámara."
        } catch (e: SecurityException) {
            errorMsg = "Permiso denegado para usar el flash."
        }
    }

    // Apagar linterna al salir de la pantalla
    DisposableEffect(Unit) {
        onDispose {
            setLinterna(false)
        }
    }

    Scaffold(
        topBar = {TopBarReusable("Linterna", onBack, {showInfo = true})},
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
                    contentDescription = if (linternaEncendida) "Linterna encendida" else "Linterna apagada",
                    tint = if (linternaEncendida) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                    Text(if (linternaEncendida) "Apagar linterna" else "Encender linterna")
                }
                if (errorMsg != null) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        errorMsg ?: "",
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
            title = { Text("¿Cómo funciona la linterna?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Usa el flash de la cámara como linterna para iluminar en la oscuridad.")
                    Text("• Pulsa el botón para encender o apagar la linterna.")
                    Text("• El flash se apaga automáticamente al salir de la pantalla.")
                    Text("• Si ves un error, es posible que tu dispositivo no tenga flash o que otra app lo esté usando.")
                    Text("• La linterna solo tiene una intensidad (encendido/apagado). No es posible ajustar el brillo, ya que es una limitación del hardware en casi todos los teléfonos.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showInfo = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
