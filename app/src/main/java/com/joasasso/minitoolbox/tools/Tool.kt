// app/src/main/java/com/example/minitoolbox/tools/Tool.kt
package com.joasasso.minitoolbox.tools

import androidx.compose.ui.graphics.vector.ImageVector
import com.joasasso.minitoolbox.nav.Screen


/**
 * Representa una mini-herramienta de la app,
 * con su nombre, pantalla de destino, categoría e icono.
 */
data class Tool(
    val name: String,
    val screen: Screen,
    val category: ToolCategory,
    val subCategory: String,
    val icon: ImageVector
)
