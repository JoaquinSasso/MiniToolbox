// app/src/main/java/com/example/minitoolbox/tools/ToolCategory.kt
package com.joasasso.minitoolbox.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.ui.graphics.vector.ImageVector
import com.joasasso.minitoolbox.R

/**
 * Categorías que aparecen en la barra inferior.
 */
sealed class ToolCategory(
    val titleRes: Int,
    val outlinedIcon: ImageVector,
    val filledIcon: ImageVector
) {
    object Herramientas :
        ToolCategory(R.string.category_herramientas, Icons.Outlined.Build, Icons.Filled.Build)

    object Favoritos : ToolCategory(
        R.string.category_favoritos, Icons.Outlined.StarOutline, Icons.Filled.Star
    )

    companion object {
        val all: List<ToolCategory>
            get() = listOf(
                Herramientas,
                Favoritos
            )
    }
}
