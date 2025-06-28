// app/src/main/java/com/example/minitoolbox/tools/ToolCategory.kt
package com.example.minitoolbox.tools

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.minitoolbox.R

/**
 * Categorías que aparecen en la barra inferior.
 */
sealed class ToolCategory(
    val key: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    object Herramientas :
        ToolCategory("herramientas", R.string.category_herramientas, Icons.Filled.Build)

    init {
        println(">>> Herramientas initialized")
    }

    object Organizacion :
        ToolCategory("organizacion", R.string.category_organizacion, Icons.Filled.CheckCircle)

    init {
        println(">>> Organizacion initialized")
    }

    object Informacion :
        ToolCategory("informacion", R.string.category_informacion, Icons.Filled.Info)

    object Entretenimiento : ToolCategory(
        "entretenimiento",
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