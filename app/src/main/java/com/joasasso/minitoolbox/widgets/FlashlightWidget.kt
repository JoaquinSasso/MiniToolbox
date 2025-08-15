package com.joasasso.minitoolbox.widgets

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.compose.ui.res.stringResource
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.GlanceModifier
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.state.*
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.setNivelLinterna

object FlashSliderKeys {
    val IS_ON = booleanPreferencesKey("flash_slider_is_on")
    val LEVEL = intPreferencesKey("flash_slider_level")
}

class FlashSliderWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val isOn = prefs[FlashSliderKeys.IS_ON] ?: false
            val level = prefs[FlashSliderKeys.LEVEL] ?: 10

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .background(GlanceTheme.colors.background),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (level == 0)
                        context.getString(R.string.flash_widget_off)
                    else
                        context.getString(R.string.flash_widget_level, level),
                    style = TextStyle(color = GlanceTheme.colors.onBackground)
                )

                Spacer(GlanceModifier.height(6.dp))

                Row(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        provider = ImageProvider(R.drawable.remove),
                        contentDescription = context.getString(R.string.flash_widget_decrease),
                        modifier = GlanceModifier
                            .size(32.dp)
                            .clickable(actionRunCallback(DecreaseFlashIntensity::class.java))
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    Image(
                        provider = ImageProvider(R.drawable.add),
                        contentDescription = context.getString(R.string.flash_widget_increase),
                        modifier = GlanceModifier
                            .size(32.dp)
                            .clickable(actionRunCallback(IncreaseFlashIntensity::class.java))
                    )
                }
            }
        }
    }
}

class FlashSliderWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FlashSliderWidget()
}


class ToggleFlashSlider : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        val isOn = prefs[FlashSliderKeys.IS_ON] ?: false
        val level = prefs[FlashSliderKeys.LEVEL] ?: 10
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val id = cameraManager.cameraIdList.firstOrNull {
            cameraManager.getCameraCharacteristics(it)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return

        try {
            if (isOn) {
                cameraManager.setTorchMode(id, false)
            } else {
                encenderConNivel(cameraManager, id, level)
            }

            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) {
                it.toMutablePreferences().apply {
                    this[FlashSliderKeys.IS_ON] = !isOn
                }
            }

            FlashSliderWidget().update(context, glanceId)
        } catch (_: Exception) {}
    }
}

class IncreaseFlashIntensity : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        val oldLevel = prefs[FlashSliderKeys.LEVEL] ?: 10
        val newLevel = (oldLevel + 1).coerceAtMost(20)

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cameraManager.cameraIdList.firstOrNull {
            cameraManager.getCameraCharacteristics(it)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return

        try {
            if (newLevel > 0) {
                encenderConNivel(cameraManager, id, newLevel)
            }

            context.setNivelLinterna(newLevel)

            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) {
                it.toMutablePreferences().apply {
                    this[FlashSliderKeys.LEVEL] = newLevel
                    this[FlashSliderKeys.IS_ON] = true
                }
            }

            FlashSliderWidget().update(context, glanceId)
        } catch (_: Exception) {}
    }
}

class DecreaseFlashIntensity : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        val oldLevel = prefs[FlashSliderKeys.LEVEL] ?: 10
        val newLevel = (oldLevel - 1).coerceAtLeast(0)

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cameraManager.cameraIdList.firstOrNull {
            cameraManager.getCameraCharacteristics(it)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return

        try {
            if (newLevel <= 0) {
                cameraManager.setTorchMode(id, false)
            } else {
                encenderConNivel(cameraManager, id, newLevel)
            }

            context.setNivelLinterna(newLevel)

            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) {
                it.toMutablePreferences().apply {
                    this[FlashSliderKeys.LEVEL] = newLevel
                    this[FlashSliderKeys.IS_ON] = newLevel > 0
                }
            }

            FlashSliderWidget().update(context, glanceId)
        } catch (_: Exception) {}
    }
}

suspend fun encenderConNivel(
    cameraManager: CameraManager,
    id: String,
    level: Int
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val maxStrength = cameraManager.getCameraCharacteristics(id)
            .get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
        val strength = (1 + ((level - 1) / 19f * (maxStrength - 1))).toInt().coerceIn(1, maxStrength)
        cameraManager.turnOnTorchWithStrengthLevel(id, strength)
    } else {
        cameraManager.setTorchMode(id, true)
    }
}



