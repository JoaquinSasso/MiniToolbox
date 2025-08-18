package com.joasasso.minitoolbox.widgets

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.setEstadoLinterna
import com.joasasso.minitoolbox.data.setNivelLinterna

object QuickFlashKeys {
    val LEVEL = intPreferencesKey("quick_flash_level")
}

class FlashQuickWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val nivel = prefs[QuickFlashKeys.LEVEL] ?: 0
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .cornerRadius(12.dp)
                    .background(GlanceTheme.colors.background),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .padding(12.dp)
                                .cornerRadius(12.dp)
                                .background(GlanceTheme.colors.primary)
                                .clickable(createFlashLevelAction(5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(resId = R.drawable.flash_low),
                                contentDescription = context.getString(R.string.flash_widget_low),
                                modifier = GlanceModifier.size(35.dp),
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.background)
                            )
                        }
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Box(
                            modifier = GlanceModifier
                                .padding(12.dp)
                                .cornerRadius(12.dp)
                                .background(GlanceTheme.colors.primary)
                                .clickable(createFlashLevelAction(12)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(resId = R.drawable.flash_med),
                                contentDescription = context.getString(R.string.flash_widget_medium),
                                modifier = GlanceModifier.size(35.dp),
                                colorFilter = ColorFilter.tint(GlanceTheme.colors.background)
                            )
                        }
                    }
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier
                            .padding(12.dp)
                            .cornerRadius(12.dp)
                            .background(GlanceTheme.colors.primary)
                            .clickable(createFlashLevelAction(20)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(resId = R.drawable.flash_high),
                            contentDescription = context.getString(R.string.flash_widget_high),
                            modifier = GlanceModifier.size(35.dp),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.background)
                        )
                    }
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Box(
                        modifier = GlanceModifier
                            .padding(12.dp)
                            .cornerRadius(12.dp)
                            .background(GlanceTheme.colors.primary)
                            .clickable(createFlashLevelAction(0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(resId = R.drawable.flash_off),
                            contentDescription = context.getString(R.string.flash_widget_off),
                            modifier = GlanceModifier.size(35.dp),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.background)
                        )
                    }
                }
            }
        }
    }

    private fun createFlashLevelAction(level: Int): Action {
        val parameters = actionParametersOf(QuickFlashSetLevel.KEY_LEVEL to level)
        return actionRunCallback(QuickFlashSetLevel::class.java, parameters)

    }
}

class FlashQuickWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FlashQuickWidget()
}

class QuickFlashSetLevel : ActionCallback {
    companion object {
        val KEY_LEVEL = ActionParameters.Key<Int>("flash_level")
    }

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val level = parameters[KEY_LEVEL] ?: 0
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val id = cameraManager.cameraIdList.firstOrNull {
            cameraManager.getCameraCharacteristics(it)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return

        try {
            if (level <= 0) {
                cameraManager.setTorchMode(id, false)
                context.setEstadoLinterna(false)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val maxStrength =
                        cameraManager.getCameraCharacteristics(id)
                            .get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                    val strength = (1 + ((level - 1) / 19f * (maxStrength - 1)))
                        .toInt().coerceIn(1, maxStrength)
                    cameraManager.turnOnTorchWithStrengthLevel(id, strength)
                } else {
                    cameraManager.setTorchMode(id, true)
                }
                context.setEstadoLinterna(true)
            }

            context.setNivelLinterna(level)

            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) {
                it.toMutablePreferences().apply {
                    this[QuickFlashKeys.LEVEL] = level
                }
            }

            FlashQuickWidget().update(context, glanceId)
        } catch (_: Exception) {
            // Ignore
        }
    }
}
