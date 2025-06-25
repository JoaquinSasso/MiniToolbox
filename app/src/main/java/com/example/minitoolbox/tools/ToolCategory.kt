// app/src/main/java/com/example/minitoolbox/tools/ToolCategory.kt
package com.example.minitoolbox.tools

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Straighten
import com.example.minitoolbox.R

/**
 * Categorías que aparecen en la barra inferior.
 */
sealed class ToolCategory(
    val key: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    object Generadores  : ToolCategory("generadores",  R.string.category_generadores,  Icons.Filled.Build)
    object Calculadoras : ToolCategory("calculadoras", R.string.category_calculadoras, Icons.Filled.Calculate)
    object Juegos       : ToolCategory("juegos",       R.string.category_juegos,       Icons.Filled.SportsEsports)
    object Informacion : ToolCategory("informacion",  R.string.category_informacion, Icons.AutoMirrored.Filled.Article)
    object Recordatorios : ToolCategory("recordatorios",  R.string.category_recordatorios, Icons.Filled.Alarm)
    object Medicion : ToolCategory("medicion",  R.string.category_medicion, Icons.Filled.Straighten)

    companion object {
        /** Lista garantizada sin nulos */
        val all: List<ToolCategory> = listOf(
            Generadores,
            Calculadoras,
            Juegos,
            Informacion,
            Medicion
        )
    }
}
