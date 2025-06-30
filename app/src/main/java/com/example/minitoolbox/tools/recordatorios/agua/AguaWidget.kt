package com.example.minitoolbox.tools.recordatorios.agua

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

class AguaWidget : GlanceAppWidget() {

    companion object {
        val KEY_AGUA = intPreferencesKey("widget_agua_ml")
        val KEY_OBJETIVO = intPreferencesKey("widget_objetivo_ml")
        val KEY_POR_VASO = intPreferencesKey("widget_por_vaso_ml")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val agua = prefs[KEY_AGUA] ?: 0
            val objetivo = prefs[KEY_OBJETIVO] ?: 2000
            val porVaso = prefs[KEY_POR_VASO] ?: 250

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(day = Color(0xFFE0F7FA), night = Color(0xFF263238)))
                    .cornerRadius(16.dp)
                    .padding(12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    Text(
                        text = "💧${(agua / 1000f).let { "%.2f".format(it) }} L / ${(objetivo / 1000f).let { "%.2f".format(it) }} L",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(day = Color.Black, night = Color.White)
                        )
                    )
                    Spacer(GlanceModifier.height(8.dp))
                    Row(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            text = "+$porVaso ml",
                            onClick = actionRunCallback<AgregarAguaCallback>()
                        )
                        Spacer(GlanceModifier.width(8.dp))
                        Button(
                            text = "-$porVaso ml",
                            onClick = actionRunCallback<QuitarAguaCallback>()
                        )
                    }
                }
            }
        }
    }
}
class AguaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AguaWidget()
}