// FavoriteToolsWidget.kt
package com.joasasso.minitoolbox.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.joasasso.minitoolbox.MainActivity
import com.joasasso.minitoolbox.tools.ToolRegistry

class AccesosDirectosWidget : GlanceAppWidget() {

    companion object {
        val FAVORITA_1 = stringPreferencesKey("favorita_1")
        val FAVORITA_2 = stringPreferencesKey("favorita_2")
        val FAVORITA_3 = stringPreferencesKey("favorita_3")
        val FAVORITA_4 = stringPreferencesKey("favorita_4")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val fondo = Color(0xFF1E88E5)
            val tools = listOf("frases", "adivina_capital", "interes_compuesto", "bubble_level", "brujula", "text_binary_converter")
            val size = LocalSize.current

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .cornerRadius(12.dp)
                    .background(GlanceTheme.colors.background),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (i in 0 until 3) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (j in 0 until 2) {
                            val index = i * 2 + j
                            val route = tools.getOrNull(index)
                            val tool = ToolRegistry.tools.find { it.screen.route == route }

                            val intent = Intent(context, MainActivity::class.java).apply {
                                putExtra("startRoute", route)
                            }
                            if (j == 1)
                            {
                                Spacer(modifier = GlanceModifier.width(8.dp))
                            }

                            Box(
                                modifier = GlanceModifier
                                    .padding(12.dp)
                                    .clickable(actionStartActivity(intent))
                                    .background(GlanceTheme.colors.primary)
                                    .cornerRadius(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (tool?.svgResId != null) {
                                    Image(
                                        provider = ImageProvider(resId = tool.svgResId),
                                        contentDescription = tool.name.toString(),
                                        modifier = GlanceModifier.size(35.dp),
                                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary)
                                    )
                                } else {
                                    Text(
                                        text = "❓",
                                        style = TextStyle(
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorProvider(Color.Black, Color.White)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class AccesosDirectosWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AccesosDirectosWidget()
}
