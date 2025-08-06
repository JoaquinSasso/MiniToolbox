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
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
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
            val tools = listOf("frases", "adivina_capital", "interes_compuesto", "bubble_level")
            val size = LocalSize.current

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(fondo, fondo))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (i in 0 until 2) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .defaultWeight(),
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

                            Box(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .fillMaxHeight()
                                    .padding(4.dp)
                                    .clickable(actionStartActivity(intent))
                                    .background(ColorProvider(Color.White, Color.White)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (tool?.svgResId != null) {
                                    Image(
                                        provider = ImageProvider(resId = tool.svgResId),
                                        contentDescription = tool.name.toString(),
                                        modifier = GlanceModifier.size(28.dp),
                                        colorFilter = ColorFilter.tint(ColorProvider(Color.Black, Color.Black))
                                    )
                                } else {
                                    Text(
                                        text = "❓",
                                        style = TextStyle(
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorProvider(Color.Black, Color.Black)
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
