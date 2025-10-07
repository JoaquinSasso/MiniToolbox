package com.joasasso.minitoolbox.widgets

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.flujoNivelLinterna
import com.joasasso.minitoolbox.metrics.widgetUse
import kotlinx.coroutines.flow.first

object FlashWidgetKeys {
    val KEY_IS_ON = booleanPreferencesKey("widget_flash_is_on")
}

class FlashToggleWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val isOn = prefs[FlashWidgetKeys.KEY_IS_ON] ?: false
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.background)
                    .clickable(onClick = actionRunCallback(ToggleFlashAction::class.java)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(
                        resId = if (isOn) R.drawable.flashlight_off else R.drawable.flashlight
                    ),
                    contentDescription = if (isOn) context.getString(R.string.flash_button_off) else context.getString(R.string.flash_button_on),
                    modifier = GlanceModifier.size(40.dp)
                )
            }
        }
    }
}

class FlashToggleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FlashToggleWidget()
}

class ToggleFlashAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val nivel = context.flujoNivelLinterna().first()

        val id = cameraManager.cameraIdList.firstOrNull {
            cameraManager.getCameraCharacteristics(it)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return

        // Obtener estado actual desde preferencias del widget
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        val isCurrentlyOn = prefs[FlashWidgetKeys.KEY_IS_ON] ?: false

        //Agregar uso a las metricas
        widgetUse(context,"widget_flashlight_mini")

        try {
            if (isCurrentlyOn) {
                cameraManager.setTorchMode(id, false)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val maxStrength =
                        cameraManager.getCameraCharacteristics(id)
                            .get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                    val strength = mapUiToStrength(nivel, maxStrength)
                    cameraManager.turnOnTorchWithStrengthLevel(id, strength)
                } else {
                    cameraManager.setTorchMode(id, true)
                }
            }

            // Guardar nuevo estado
            updateAppWidgetState(
                context = context,
                definition = PreferencesGlanceStateDefinition,
                glanceId = glanceId
            ) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[FlashWidgetKeys.KEY_IS_ON] = !isCurrentlyOn
                }
            }

            FlashToggleWidget().updateAll(context)

        } catch (_: Exception) {
            // ignora errores silenciosamente
        }
    }

    private fun mapUiToStrength(level: Int, max: Int): Int {
        if (level <= 0 || max <= 1) return 1
        val frac = (level - 1f) / 19f
        return (1 + frac * (max - 1)).toInt().coerceIn(1, max)
    }
}
