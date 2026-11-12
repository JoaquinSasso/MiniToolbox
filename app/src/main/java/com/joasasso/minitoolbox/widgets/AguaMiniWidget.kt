package com.joasasso.minitoolbox.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.joasasso.minitoolbox.MainActivity
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.metrics.MetricsSource
import com.joasasso.minitoolbox.nav.Screen
import com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua.AgregarAguaCallback
import com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua.AguaFormat
import com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua.actualizarWidgetAgua

class AguaMiniWidget : GlanceAppWidget() {

    companion object {
        val KEY_AGUA = intPreferencesKey("widget_agua_ml")
        val KEY_OBJETIVO = intPreferencesKey("widget_objetivo_ml")
        val KEY_POR_VASO = intPreferencesKey("widget_por_vaso_ml")
    }

    // Con el SizeMode.Single por defecto, LocalSize devolvía el tamaño mínimo declarado (40 dp)
    // y no el real; el código lo compensaba multiplicando por 2.5, y la barra se veía llena al 40 %.
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme(colors = aguaWidgetColors()) {
                val prefs = currentState<Preferences>()
                val agua = prefs[KEY_AGUA] ?: 0
                val objetivo = prefs[KEY_OBJETIVO] ?: 2000
                val porVaso = prefs[KEY_POR_VASO] ?: 250
                val fraccion = if (objetivo > 0) (agua / objetivo.toFloat()).coerceIn(0f, 1f) else 1f
                val size = LocalSize.current

                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(Screen.EXTRA_START_ROUTE_JSON, Screen.toJson(Screen.Water))
                    putExtra(MetricsSource.EXTRA_START_SOURCE, MetricsSource.WIDGET)
                }

                // Riel neutro y relleno de color: el texto onSurfaceVariant se lee sobre los dos
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(16.dp)
                        .background(GlanceTheme.colors.surfaceVariant)
                        .clickable(actionStartActivity(launchIntent)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(size.height * fraccion)
                            .background(GlanceTheme.colors.primaryContainer)
                    ) {}

                    Column(
                        modifier = GlanceModifier.fillMaxSize().padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = context.getString(R.string.water_liters_value, AguaFormat.litros(agua)),
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = context.getString(R.string.water_of_goal, AguaFormat.litros(objetivo)),
                            style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.onSurfaceVariant),
                            maxLines = 1
                        )
                    }

                    // Zona inferior para sumar un vaso sin abrir la app
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp)
                            .semantics { contentDescription = context.getString(R.string.water_add, porVaso) }
                            .clickable(actionRunCallback<AgregarAguaCallback>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}

class AguaMiniWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AguaMiniWidget()
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        actualizarWidgetAgua(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        actualizarWidgetAgua(context)
    }
}
