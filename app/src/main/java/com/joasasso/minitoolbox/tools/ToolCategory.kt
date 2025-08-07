// app/src/main/java/com/example/minitoolbox/tools/ToolCategory.kt
package com.joasasso.minitoolbox.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SportsEsports
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

    object Organizacion :
        ToolCategory(R.string.category_organizacion, Icons.Outlined.CheckCircle, Icons.Filled.CheckCircle)

    object Informacion :
        ToolCategory(R.string.category_informacion, Icons.Outlined.Info, Icons.Filled.Info)

    object Entretenimiento : ToolCategory(R.string.category_entretenimiento, Icons.Outlined.SportsEsports,Icons.Filled.SportsEsports)

    object Favoritos : ToolCategory(
        R.string.category_favoritos, Icons.Outlined.StarOutline,Icons.Filled.Star)

    companion object {
        val all: List<ToolCategory>
            get() = listOf(
                Herramientas,
                Organizacion,
                Informacion,
                Entretenimiento,
                Favoritos
            )
    }
}