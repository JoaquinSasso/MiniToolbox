package com.joasasso.minitoolbox.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.joasasso.minitoolbox.MainActivity
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua.AgregarAguaCallback
import com.joasasso.minitoolbox.tools.organizacion.recordatorios.agua.actualizarWidgetAgua
import com.joasasso.minitoolbox.utils.pro.LocalProState
import com.joasasso.minitoolbox.utils.pro.paywallIntent


class AguaMiniWidget : GlanceAppWidget() {

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
            val frac = (agua / objetivo.toFloat()).coerceIn(0f, 1f)
            val size = LocalSize.current

            val fondoColor = Color(0xFF0F5E9C)
            val progresoColor = Color(0xFF2389DA)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("startRoute", "agua")
            }

            // Widget general: clic abre la app
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(actionStartActivity(launchIntent)),
                contentAlignment = Alignment.Center
            ) {
                // Fondo azul
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ColorProvider(fondoColor, fondoColor))
                ){}

                // Barra de progreso vertical
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(((size.height + 2.dp) * frac * 2.5f))
                            .background(ColorProvider(progresoColor, progresoColor))
                    ){}
                }

                // Texto de cantidad
                Text(
                    text = "${(agua / 1000f).let { "%.2f".format(it) }} L / ${(objetivo / 1000f).let { "%.2f".format(it) }} L",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = ColorProvider(day = Color.White, night = Color.White)
                    )
                )

                // Botón "+" centrado abajo para sumar agua
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = GlanceModifier
                            .padding(bottom = 4.dp)
                            .fillMaxWidth()
                            .clickable(actionRunCallback<AgregarAguaCallback>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 25.sp,
                                color = ColorProvider(day = Color.White, night = Color.White)
                            )
                        )
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