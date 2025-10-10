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
    val isPro: Boolean = false
)
