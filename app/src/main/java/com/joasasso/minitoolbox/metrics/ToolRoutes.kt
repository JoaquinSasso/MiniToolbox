package com.joasasso.minitoolbox.metrics

import com.joasasso.minitoolbox.nav.Screen
import com.joasasso.minitoolbox.tools.Tool
import com.joasasso.minitoolbox.tools.ToolRegistry

/**
 * Traduce una ruta de navegación o destino Screen a la herramienta del registry que le corresponde.
 * Unifica subpantallas (como detalle de pomodoro, estadísticas de agua o pantallas del divisor de gastos)
 * bajo la clave de métrica de su herramienta correspondiente.
 */
object ToolRoutes {

    private val byExactRoute: Map<String, Tool> by lazy {
        ToolRegistry.tools.associateBy { it.screen.route }
    }

    private val pomodoroTool: Tool? by lazy {
        ToolRegistry.tools.firstOrNull { it.screen == Screen.PomodoroList }
    }

    private val waterTool: Tool? by lazy {
        ToolRegistry.tools.firstOrNull { it.screen == Screen.Water }
    }

    private val meetingsTool: Tool? by lazy {
        ToolRegistry.tools.firstOrNull { it.screen == Screen.Meetings }
    }

    private val basicPhrasesTool: Tool? by lazy {
        ToolRegistry.tools.firstOrNull { it.screen == Screen.BasicPhrases }
    }

    /**
     * Resuelve la herramienta correspondiente a partir de un destino Screen.
     */
    fun findToolByScreen(screen: Screen?): Tool? {
        if (screen == null) return null
        return when (screen) {
            is Screen.PomodoroList, is Screen.PomodoroDetail -> pomodoroTool
            is Screen.Water, is Screen.WaterStats -> waterTool
            is Screen.Meetings, is Screen.MeetingCreate, is Screen.MeetingDetail,
            is Screen.ExpenseAdd, is Screen.ExpenseEdit -> meetingsTool
            is Screen.BasicPhrases -> basicPhrasesTool
            else -> ToolRegistry.tools.firstOrNull { it.screen == screen }
        }
    }

    /**
     * Herramienta correspondiente a la ruta, o null si la ruta no es una herramienta.
     * Tolera query strings y fragmentos ("water?from=widget" resuelve a la misma que "water").
     */
    fun findTool(route: String?): Tool? {
        if (route.isNullOrBlank()) return null
        val base = route.substringBefore('?').substringBefore('#')

        val screen = Screen.fromRouteString(base)
        if (screen != null) {
            val tool = findToolByScreen(screen)
            if (tool != null) return tool
        }

        return byExactRoute[base]
    }

    /**
     * Clave de métrica estable para esa ruta, o null si no corresponde registrar nada.
     */
    fun metricsKey(route: String?): String? = findTool(route)?.metricsKey

    /**
     * Clave de métrica estable para ese destino Screen, o null si no corresponde registrar nada.
     */
    fun metricsKey(screen: Screen): String? = findToolByScreen(screen)?.metricsKey
}