package com.joasasso.minitoolbox.metrics

import com.joasasso.minitoolbox.nav.Screen
import com.joasasso.minitoolbox.tools.ToolRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Protege el contrato entre las herramientas de la app y las claves que viajan al backend.
 *
 * El incidente original ocurrió porque la clave de métrica era la ruta de navegación cruda:
 * un deep link con argumentos ("pomodoro/detail/<uuid>") producía una clave que el backend
 * rechazaba, y ese rechazo dejaba al dispositivo sin poder enviar métricas nunca más.
 */
class ToolMetricsKeysTest {

    /**
     * Conjunto congelado de claves de métrica en producción.
     *
     * [com.joasasso.minitoolbox.tools.Tool.metricsKey] toma por defecto el valor de
     * `screen.route`, así que renombrar una ruta cambiaría silenciosamente la clave y
     * partiría en dos la serie histórica de esa herramienta.
     *
     * Si este test falla, la decisión NO es actualizar la lista sin pensarlo. Casi siempre
     * lo correcto es fijar `metricsKey` explícitamente en esa entrada del registry con el
     * valor anterior. Sólo se actualiza esta lista al agregar una herramienta nueva.
     */
    private val expectedKeys = setOf(
        "bubble_level",
        "group_selector",
        "coin_flip",
        "decimal_binary",
        "truco_scoreboard",
        "age_calculator",
        "zodiac_sign",
        "percentage",
        "unit_converter",
        "password_generator",
        "pomodoro_list",
        "qr_generator",
        "ruler",
        "light_meter",
        "flashlight",
        "water",
        "countdown",
        "countries_info",
        "selector_wheel",
        "guess_flag",
        "meetings",
        "dice",
        "quick_calcs",
        "quotes",
        "multiverse_me",
        "guess_capital",
        "compass",
        "todo",
        "scoreboard",
        "magnifier",
        "ar_ruler",
        "about",
        "minesweeper"
    )

    @Test
    fun `todas las metricsKey cumplen el contrato del backend`() {
        val invalid = ToolRegistry.tools
            .map { it.metricsKey }
            .filterNot { MetricsContract.isValidKey(it) }

        assertTrue(
            "Claves que el backend rechazaría: $invalid",
            invalid.isEmpty()
        )
    }

    @Test
    fun `no hay metricsKey duplicadas`() {
        val keys = ToolRegistry.tools.map { it.metricsKey }
        val duplicated = keys.groupingBy { it }.eachCount().filter { it.value > 1 }.keys

        assertTrue("Claves duplicadas: $duplicated", duplicated.isEmpty())
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `el conjunto de claves no cambio sin decision explicita`() {
        val actual = ToolRegistry.tools.map { it.metricsKey }.toSet()

        val added = actual - expectedKeys
        val removed = expectedKeys - actual

        assertTrue(
            "Claves nuevas: $added. Si es una herramienta nueva, agregala a expectedKeys. " +
                    "Claves perdidas: $removed. Si cambió una ruta, fijá metricsKey en el registry " +
                    "con el valor anterior para no romper la serie histórica.",
            added.isEmpty() && removed.isEmpty()
        )
    }

    // -----------------------------------------------------------------
    // Resolución de rutas
    // -----------------------------------------------------------------

    @Test
    fun `una ruta de herramienta resuelve a su clave`() {
        assertEquals("water", ToolRoutes.metricsKey(Screen.Water.route))
        assertEquals("ar_ruler", ToolRoutes.metricsKey(Screen.ArRuler.route))
    }

    @Test
    fun `una ruta con argumentos no genera metrica`() {
        val route = Screen.PomodoroDetail.createRoute("2f7a1b3c-4d5e-6f70-8a9b-0c1d2e3f4a5b")

        assertNull(
            "Una ruta con UUID no debe producir clave de métrica",
            ToolRoutes.metricsKey(route)
        )
    }

    @Test
    fun `una pantalla que no es herramienta no genera metrica`() {
        assertNull(ToolRoutes.metricsKey(Screen.Pro.route))
        assertNull(ToolRoutes.metricsKey(Screen.Categories.route))
    }

    @Test
    fun `las rutas invalidas no rompen el resolver`() {
        assertNull(ToolRoutes.metricsKey(null))
        assertNull(ToolRoutes.metricsKey(""))
        assertNull(ToolRoutes.metricsKey("   "))
        assertNull(ToolRoutes.metricsKey("ruta_que_no_existe"))
    }

    @Test
    fun `el query string no impide resolver la herramienta`() {
        assertEquals("water", ToolRoutes.metricsKey("water?from=widget"))
        assertEquals("water", ToolRoutes.metricsKey("water#seccion"))
    }

    /**
     * Todo `startRoute` que la app pone en un Intent debe resolver a una ruta real del
     * NavGraph. El deep link del recordatorio de agua usaba "agua", que no existe: la
     * navegación fallaba en silencio dentro de un try/catch y el usuario no llegaba
     * a ningún lado.
     */
    @Test
    fun `los startRoute de notificaciones y widgets son rutas validas`() {
        val startRoutes = listOf(
            Screen.Water.route,   // AguaNotification, AguaWidget, AguaMiniWidget
            Screen.Pro.route      // RedirectToPaywall
        )

        for (route in startRoutes) {
            assertTrue(
                "'$route' no es una ruta válida del NavGraph",
                Screen.isValidRoute(route)
            )
        }
    }

    @Test
    fun `las rutas de las herramientas del registry son validas`() {
        for (tool in ToolRegistry.tools) {
            assertTrue(
                "La ruta '${tool.screen.route}' no es válida en el NavGraph",
                Screen.isValidRoute(tool.screen.route)
            )
            assertNotNull(
                "La ruta '${tool.screen.route}' no resuelve a su propia herramienta",
                ToolRoutes.findTool(tool.screen.route)
            )
        }
    }
}