package com.joasasso.minitoolbox.metrics

import com.joasasso.minitoolbox.nav.Screen
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Cuándo una navegación cuenta como un uso nuevo de herramienta.
 *
 * Antes se comparaba la ruta: ir de la lista de Pomodoro al detalle volvía a contar
 * `pomodoro`, y Agua -> Categorías -> Agua no contaba la segunda entrada porque la ruta
 * coincidía con la última herramienta guardada.
 */
class ToolVisitTest {

    /** Reproduce la lógica del NavGraph sobre una secuencia de rutas. */
    private fun countedKeys(vararg routes: String): List<String> {
        var current: String? = null
        val counted = mutableListOf<String>()
        for (route in routes) {
            val key = ToolRoutes.metricsKey(route)
            ToolRoutes.visitToCount(current, key)?.let { counted += it }
            current = key
        }
        return counted
    }

    @Test
    fun `entrar a una herramienta cuenta una vez`() {
        assertEquals(listOf("water"), countedKeys(Screen.Categories.route, Screen.Water.route))
    }

    @Test
    fun `moverse entre subpantallas de la misma herramienta no vuelve a contar`() {
        assertEquals(
            listOf("pomodoro"),
            countedKeys(
                Screen.Categories.route,
                Screen.PomodoroList.route,
                Screen.PomodoroDetail("t-1").route,
                Screen.PomodoroList.route
            )
        )
        assertEquals(
            listOf("water"),
            countedKeys(Screen.Water.route, Screen.WaterStats.route, Screen.Water.route)
        )
    }

    @Test
    fun `volver a entrar despues de pasar por categorias cuenta otra vez`() {
        assertEquals(
            listOf("water", "water"),
            countedKeys(
                Screen.Categories.route,
                Screen.Water.route,
                Screen.Categories.route,
                Screen.Water.route
            )
        )
    }

    @Test
    fun `pasar de una herramienta a otra cuenta las dos`() {
        assertEquals(
            listOf("water", "pomodoro"),
            countedKeys(Screen.Water.route, Screen.PomodoroList.route)
        )
    }

    @Test
    fun `las pantallas que no son herramientas no cuentan`() {
        assertEquals(emptyList<String>(), countedKeys(Screen.Categories.route, Screen.DevMetrics.route))
    }
}
