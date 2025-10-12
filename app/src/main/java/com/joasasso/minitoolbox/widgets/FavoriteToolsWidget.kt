package com.joasasso.minitoolbox.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
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
import com.joasasso.minitoolbox.MainActivity
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.data.FAVORITOS_KEYS
import com.joasasso.minitoolbox.data.flujoToolsFavoritas
import com.joasasso.minitoolbox.tools.ToolRegistry
import com.joasasso.minitoolbox.utils.pro.LocalProState
import com.joasasso.minitoolbox.utils.pro.ProRepository
import com.joasasso.minitoolbox.utils.pro.paywallIntent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FavoriteToolsWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val size = LocalSize.current

            val routesFavoritas = FAVORITOS_KEYS.mapNotNull { prefs[it] }
            val toolsFavoritas = routesFavoritas.mapNotNull { route ->
                ToolRegistry.tools.find { it.screen.route == route }
            }

            val columns = when {
                size.width >= 350.dp -> 5
                size.width >= 300.dp -> 4
                size.width >= 250.dp -> 3
                size.width >= 150.dp -> 2
                else -> 1
            }

            val rows = when {
                size.height >= 500.dp -> 9
                size.height >= 350.dp -> 6
                size.height >= 250.dp -> 4
                size.height >= 150.dp -> 3
                else -> 1
            }

            val totalCeldas = columns * rows
            val toolsAMostrar = toolsFavoritas.take(totalCeldas)

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(6.dp)
                    .cornerRadius(12.dp)
                    .background(GlanceTheme.colors.background),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (i in 0 until rows) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (j in 0 until columns) {
                            val index = i * columns + j
                            val tool = toolsAMostrar.getOrNull(index)

                            if (j != 0) {
                                Spacer(modifier = GlanceModifier.width(8.dp))
                            }

                            Box(
                                modifier = GlanceModifier
                                    .padding(12.dp)
                                    .cornerRadius(12.dp)
                                    .background(GlanceTheme.colors.primary)
                                    .let {
                                        if (tool != null) it.clickable(
                                            actionStartActivity(
                                                Intent(context, MainActivity::class.java).apply {
                                                    putExtra("startRoute", tool.screen.route)
                                                    addFlags(
                                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                                                    )
                                                }
                                            )
                                        ) else it
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (tool?.svgResId != null) {
                                    Image(
                                        provider = ImageProvider(resId = tool.svgResId),
                                        contentDescription = tool.name.toString(),
                                        modifier = GlanceModifier.size(32.dp),
                                        colorFilter = ColorFilter.tint(GlanceTheme.colors.background)
                                    )
                                } else {
                                    Image(
                                        provider = ImageProvider(R.drawable.close),
                                        contentDescription = "Empty",
                                        modifier = GlanceModifier.size(32.dp),
                                        colorFilter = ColorFilter.tint(GlanceTheme.colors.background)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // Overlay PRO si no es Pro
            val proState = LocalProState.current
            val isPro = proState.isPro
            if (!isPro) {
                // Badge PRO en la esquina superior derecha
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .background(Color(0xB0000000))
                        .clickable(actionStartActivity(paywallIntent(context))),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(
                            resId = R.drawable.pro_badge
                        ),
                        contentDescription = "PRO Tool",
                        modifier = GlanceModifier.size(40.dp),
                        colorFilter = ColorFilter.tint(ColorProvider(Color(0xFFFFD700), Color(0xFFFFD700))) // Tinte dorado
                    )
                }
            }
        }
    }
}

class FavoriteToolsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FavoriteToolsWidget()

    // Cuando se agrega el primer widget de este tipo
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Sembrar estado inicial desde DataStore → estado del widget
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            actualizarWidgetFavoritos(context)
        }
    }

    // Cuando el sistema pide actualizar (al agregar una nueva instancia, cambiar tamaño, etc.)
    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            actualizarWidgetFavoritos(context)
        }
    }
    class OpenPaywallAction : ActionCallback {
        override suspend fun onAction(
            context: Context,
            glanceId: GlanceId,
            parameters: ActionParameters
        ) {
            val isPro = ProRepository.isProFlow(context).first()
            //Si no es pro mostrar el paywall
            if (!isPro) {
                // Abrir paywall directamente: esto sí ejecuta la navegación
                context.startActivity(paywallIntent(context))
                return
            }
        }
    }
}

suspend fun actualizarWidgetFavoritos(context: Context) {
    val favoritos = context.flujoToolsFavoritas().first()
    val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(FavoriteToolsWidget::class.java)

    glanceIds.forEach { id ->
        updateAppWidgetState(context, FavoriteToolsWidget().stateDefinition, id) { prefs ->
            prefs.toMutablePreferences().apply {
                FAVORITOS_KEYS.forEachIndexed { index, key ->
                    if (index < favoritos.size) set(key, favoritos[index]) else remove(key)
                }
            }
        }
        FavoriteToolsWidget().update(context, id)
    }
}
