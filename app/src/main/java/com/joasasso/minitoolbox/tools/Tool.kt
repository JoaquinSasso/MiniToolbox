// app/src/main/java/com/example/minitoolbox/tools/Tool.kt
package com.joasasso.minitoolbox.tools

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.joasasso.minitoolbox.nav.Screen


/**
 * Representa una mini-herramienta de la app,
 * con su nombre, pantalla de destino, categoría e icono.
 */
data class Tool(
    val name: Int,
    val screen: Screen,
    val category: ToolCategory,
    val subCategory: Int,
    @StringRes val summary: Int? = null,
    val icon: ImageVector? = null,
    @DrawableRes val svgResId: Int? = null,
    val isPro: Boolean = false,
    /**
     * Identificador estable para métricas.
     *
     * Por defecto coincide con la ruta de navegación, que es lo que se venía usando
     * y mantiene la continuidad histórica de los datos. Pero es un campo aparte a
     * propósito: si alguna vez cambia la ruta por razones de UX, hay que fijar acá
     * el valor anterior para no romper las series temporales. Debe cumplir siempre
     * el contrato de MetricsContract.KEY_RE.
     */
    val metricsKey: String = screen.route
)