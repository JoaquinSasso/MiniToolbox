package com.joasasso.minitoolbox.metrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `widget_shortcuts` se contaba para cualquier deep link, incluidas las notificaciones y
 * los widgets de agua. Ahora solo cuenta el widget de favoritos.
 */
class MetricsSourceTest {

    @Test
    fun `el widget de favoritos cuenta widget_shortcuts`() {
        assertEquals(
            "widget_shortcuts",
            MetricsSource.deepLinkWidgetKey(MetricsSource.WIDGET, MetricsSource.FAVORITES_WIDGET_KEY)
        )
    }

    @Test
    fun `una notificacion no cuenta como widget`() {
        assertNull(
            MetricsSource.deepLinkWidgetKey(MetricsSource.NOTIFICATION, null)
        )
    }

    @Test
    fun `los widgets de agua abren con origen widget pero no cuentan como favoritos`() {
        // AguaWidget y AguaMiniWidget no ponen EXTRA_START_WIDGET.
        assertNull(MetricsSource.deepLinkWidgetKey(MetricsSource.WIDGET, null))
    }

    @Test
    fun `un deep link sin origen no cuenta`() {
        assertNull(MetricsSource.deepLinkWidgetKey(null, null))
    }

    @Test
    fun `la marca de favoritos sin origen widget no cuenta`() {
        // Un Intent armado a mano con el extra pero sin origen no se atribuye al widget.
        assertNull(
            MetricsSource.deepLinkWidgetKey(MetricsSource.NOTIFICATION, MetricsSource.FAVORITES_WIDGET_KEY)
        )
    }

    @Test
    fun `la clave del widget de favoritos no cambia`() {
        // Es contrato con el backend y con la serie histórica (DECISIONS.md §4).
        assertEquals("widget_shortcuts", MetricsSource.FAVORITES_WIDGET_KEY)
    }
}
