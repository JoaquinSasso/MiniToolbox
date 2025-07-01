// app/src/main/java/com/example/minitoolbox/tools/ToolCategory.kt
package com.example.minitoolbox.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.minitoolbox.R

/**
 * Categorías que aparecen en la barra inferior.
 */
sealed class ToolCategory(
    val titleRes: Int,
    val icon: ImageVector
) {
    object Herramientas :
        ToolCategory(R.string.category_herramientas, Icons.Filled.Build)

    object Organizacion :
        ToolCategory(R.string.category_organizacion, Icons.Filled.CheckCircle)

    object Informacion :
        ToolCategory(R.string.category_informacion, Icons.Filled.Info)

    object Entretenimiento : ToolCategory(
        R.string.category_entretenimiento,
        Icons.Filled.SportsEsports
    )

    companion object {
        val all: List<ToolCategory>
            get() = listOf(
                Herramientas,
                Organizacion,
                Informacion,
                Entretenimiento
            )
    }
}