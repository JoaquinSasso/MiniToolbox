package com.joasasso.minitoolbox.metrics

import com.joasasso.minitoolbox.tools.Tool
import com.joasasso.minitoolbox.tools.ToolRegistry

/**
 * Traduce una ruta de navegación a la herramienta del registry que le corresponde.
 *
 * Existe por un incidente concreto: la ruta cruda se usaba directamente como clave de
 * métrica. Cuando un deep link traía una ruta con argumentos
 * ("pomodoro/detail/2f7a1b3c-…"), esa clave no cumplía el contrato del backend, el lote
 * quedaba congelado y el dispositivo dejaba de reportar métricas de forma permanente.
 *
 * La regla es: **una ruta que no resuelve a una herramienta conocida no genera métrica**.
 * Vale más perder un registro que envenenar la cola de envío. Además, esto elimina las
 * claves fantasma que se generaban desde pantallas que no son herramientas (por ejemplo
 * "pro" al abrir el paywall desde un widget).
 */
object ToolRoutes {

    private val byExactRoute: Map<String, Tool> by lazy {
        ToolRegistry.tools.associateBy { it.screen.route }
    }

    private val parameterized: List<Pair<Regex, Tool>> by lazy {
        ToolRegistry.tools
            .filter { it.screen.route.contains('{') }
            .map { patternToRegex(it.screen.route) to it }
    }

    /** Segmento de ruta válido: lo que puede ocupar el lugar de un {param}. */
    private const val SEGMENT_REGEX = "[A-Za-z0-9._~+%-]+"

    /**
     * Herramienta correspondiente a la ruta, o null si la ruta no es una herramienta.
     * Tolera query strings y fragmentos ("water?from=widget" resuelve a la misma que "water").
     */
    fun findTool(route: String?): Tool? {
        if (route.isNullOrBlank()) return null
        val base = route.substringBefore('?').substringBefore('#')

        byExactRoute[base]?.let { return it }

        return parameterized.firstOrNull { (regex, _) -> regex.matches(base) }?.second
    }

    /**
     * Clave de métrica estable para esa ruta, o null si no corresponde registrar nada.
     */
    fun metricsKey(route: String?): String? = findTool(route)?.metricsKey

    /** Convierte "pomodoro/detail/{timerId}" en un Regex que matchea valores reales. */
    private fun patternToRegex(pattern: String): Regex {
        val sb = StringBuilder()
        var i = 0
        while (i < pattern.length) {
            val ch = pattern[i]
            if (ch == '{') {
                val end = pattern.indexOf('}', startIndex = i + 1)
                if (end == -1) {
                    sb.append(Regex.escape(pattern.substring(i)))
                    break
                }
                sb.append(SEGMENT_REGEX)
                i = end + 1
            } else {
                sb.append(Regex.escape(ch.toString()))
                i++
            }
        }
        return Regex("^$sb$")
    }
}