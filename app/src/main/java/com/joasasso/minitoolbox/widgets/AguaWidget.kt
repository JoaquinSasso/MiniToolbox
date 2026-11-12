package com.joasasso.minitoolbox.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.layout.Box
import androidx.glance.appwidget.SizeMode
import androidx.glance.LocalSize
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.FilledButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.joasasso.minitoolbox.MainActivity
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.metrics.MetricsSource
import com.joasasso.minitoolbox.nav.Screen
import com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua.AgregarAguaCallback
import com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua.AguaFormat
import com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua.QuitarAguaCallback
import com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua.actualizarWidgetAgua

class AguaWidget : GlanceAppWidget() {

    // Exact: LocalSize trae el tamaño real y el layout elige entre la versión compacta y la completa
    override val sizeMode: SizeMode = SizeMode.Exact

    companion object {
        val KEY_AGUA = intPreferencesKey("widget_agua_ml")
        val KEY_OBJETIVO = intPreferencesKey("widget_objetivo_ml")
        val KEY_POR_VASO = intPreferencesKey("widget_por_vaso_ml")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // Antes el texto era blanco fijo sobre el fondo del tema y en tema claro no se leía
            GlanceTheme(colors = aguaWidgetColors()) {
                val prefs = currentState<Preferences>()
                val agua = prefs[KEY_AGUA] ?: 0
                val objetivo = prefs[KEY_OBJETIVO] ?: 2000
                val porVaso = prefs[KEY_POR_VASO] ?: 250
                val fraccion = if (objetivo > 0) (agua / objetivo.toFloat()).coerceIn(0f, 1f) else 1f

                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(Screen.EXTRA_START_ROUTE_JSON, Screen.toJson(Screen.Water))
                    putExtra(MetricsSource.EXTRA_START_SOURCE, MetricsSource.WIDGET)
                }

                val textoProgreso = context.getString(R.string.water_liters_value, AguaFormat.litros(agua)) + " " +
                    context.getString(R.string.water_of_goal, AguaFormat.litros(objetivo))
                val size = LocalSize.current
                // Con una sola fila de alto (la celda por defecto de muchos launchers, incluido el de
                // Xiaomi) el layout vertical no entra: necesita ~106 dp y se cortaba.
                val compacto = size.height < 100.dp

                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(16.dp)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clickable(actionStartActivity(launchIntent)),
                    contentAlignment = Alignment.Center
                ) {
                    if (compacto) {
                        Row(
                            modifier = GlanceModifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = GlanceModifier.defaultWeight()) {
                                Text(
                                    text = textoProgreso,
                                    style = TextStyle(fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface),
                                    maxLines = 1
                                )
                                Spacer(GlanceModifier.height(4.dp))
                                BarraProgreso(fraccion)
                            }
                            Spacer(GlanceModifier.width(8.dp))
                            // Con poco ancho queda solo sumar, que es la acción frecuente
                            if (size.width >= 220.dp) {
                                BotonQuitar(context, porVaso)
                                Spacer(GlanceModifier.width(4.dp))
                            }
                            CircleIconButton(
                                imageProvider = ImageProvider(R.drawable.ic_water_add),
                                contentDescription = context.getString(R.string.water_add, porVaso),
                                onClick = actionRunCallback<AgregarAguaCallback>(),
                                backgroundColor = GlanceTheme.colors.primary,
                                contentColor = GlanceTheme.colors.onPrimary
                            )
                        }
                    } else {
                        Column(
                            modifier = GlanceModifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = textoProgreso,
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = GlanceTheme.colors.onSurface
                                ),
                                maxLines = 1
                            )
                            Spacer(GlanceModifier.height(8.dp))
                            BarraProgreso(fraccion)
                            Spacer(GlanceModifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BotonQuitar(context, porVaso)
                                Spacer(GlanceModifier.width(8.dp))
                                FilledButton(
                                    text = context.getString(R.string.water_add, porVaso),
                                    onClick = actionRunCallback<AgregarAguaCallback>(),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class AguaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AguaWidget()
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

@Composable
private fun BarraProgreso(fraccion: Float) {
    LinearProgressIndicator(
        progress = fraccion,
        modifier = GlanceModifier.fillMaxWidth().height(8.dp).cornerRadius(4.dp),
        color = GlanceTheme.colors.primary,
        backgroundColor = GlanceTheme.colors.secondaryContainer
    )
}

@Composable
private fun BotonQuitar(context: Context, porVaso: Int) {
    CircleIconButton(
        imageProvider = ImageProvider(R.drawable.ic_water_remove),
        contentDescription = context.getString(R.string.water_remove_desc, porVaso),
        onClick = actionRunCallback<QuitarAguaCallback>(),
        backgroundColor = GlanceTheme.colors.secondaryContainer,
        contentColor = GlanceTheme.colors.onSecondaryContainer
    )
}
