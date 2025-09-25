package com.joasasso.minitoolbox.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager

object WidgetWaterKindResolver {

    // 👇 Ajustá estos nombres si tus receivers se llaman distinto
    private const val WATER_MINI_PROVIDER   = "com.joasasso.minitoolbox.widgets.AguaMiniWidgetReceiver"
    private const val WATER_NORMAL_PROVIDER = "com.joasasso.minitoolbox.widgets.AguaWidgetReceiver"

    private fun providerClassName(context: Context, glanceId: GlanceId): String? = try {
        val glanceMgr = GlanceAppWidgetManager(context)
        val appWidgetId = glanceMgr.getAppWidgetId(glanceId)
        val info = AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId)
        info?.provider?.className
    } catch (_: Throwable) { null }

    /** Devuelve "widget_water_mini" o "widget_water_normal" (o "widget_water_unknown" si no matchea). */
    fun resolve(context: Context, glanceId: GlanceId): String =
        when (providerClassName(context, glanceId)) {
            WATER_MINI_PROVIDER   -> "widget_water_mini"
            WATER_NORMAL_PROVIDER -> "widget_water_normal"
            else -> "widget_water_unknown"
        }
}
