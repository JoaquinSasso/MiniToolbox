package com.joasasso.minitoolbox.tools

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.ui.graphics.vector.ImageVector
import com.joasasso.minitoolbox.R

/**
 * Categorías principales del catálogo de herramientas.
 */
sealed class ToolCategory(
    @StringRes val titleRes: Int,
    val outlinedIcon: ImageVector,
    val filledIcon: ImageVector
) {
    data object Instrumentos : ToolCategory(
        titleRes = R.string.category_instrumentos,
        outlinedIcon = Icons.Outlined.Straighten,
        filledIcon = Icons.Filled.Straighten
    )

    data object Cotidiano : ToolCategory(
        titleRes = R.string.category_cotidiano,
        outlinedIcon = Icons.Outlined.Schedule,
        filledIcon = Icons.Filled.Schedule
    )

    data object Generadores : ToolCategory(
        titleRes = R.string.category_generadores,
        outlinedIcon = Icons.Outlined.AutoAwesome,
        filledIcon = Icons.Filled.AutoAwesome
    )

    data object Juegos : ToolCategory(
        titleRes = R.string.category_juegos,
        outlinedIcon = Icons.Outlined.SportsEsports,
        filledIcon = Icons.Filled.SportsEsports
    )

    data object Favoritos : ToolCategory(
        titleRes = R.string.category_favoritos,
        outlinedIcon = Icons.Outlined.StarOutline,
        filledIcon = Icons.Filled.Star
    )

    companion object {
        /** Categorías de dominio en orden de presentación del catálogo. */
        val domainCategories: List<ToolCategory> = listOf(
            Instrumentos,
            Cotidiano,
            Generadores,
            Juegos
        )

        val all: List<ToolCategory> = listOf(
            Instrumentos,
            Cotidiano,
            Generadores,
            Juegos,
            Favoritos
        )
    }
}
