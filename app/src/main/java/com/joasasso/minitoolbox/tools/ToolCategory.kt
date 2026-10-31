package com.joasasso.minitoolbox.tools

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.ui.graphics.vector.ImageVector
import com.joasasso.minitoolbox.R

/**
 * Categorías del catálogo, nombradas por lo que el usuario quiere hacer.
 *
 * Favoritos no es una categoría: es una sección propia de la pantalla. La categoría no forma
 * parte de las métricas (la clave es `Tool.metricsKey`), así que reorganizarla no parte series.
 */
sealed class ToolCategory(
    @StringRes val titleRes: Int,
    val outlinedIcon: ImageVector,
    val filledIcon: ImageVector
) {
    data object Medir : ToolCategory(
        titleRes = R.string.category_measure,
        outlinedIcon = Icons.Outlined.Straighten,
        filledIcon = Icons.Filled.Straighten
    )

    data object Esenciales : ToolCategory(
        titleRes = R.string.category_essentials,
        outlinedIcon = Icons.Outlined.Lightbulb,
        filledIcon = Icons.Filled.Lightbulb
    )

    data object Organizar : ToolCategory(
        titleRes = R.string.category_organize,
        outlinedIcon = Icons.AutoMirrored.Outlined.EventNote,
        filledIcon = Icons.AutoMirrored.Filled.EventNote
    )

    data object Generar : ToolCategory(
        titleRes = R.string.category_generate,
        outlinedIcon = Icons.Outlined.AutoAwesome,
        filledIcon = Icons.Filled.AutoAwesome
    )

    data object Jugar : ToolCategory(
        titleRes = R.string.category_play,
        outlinedIcon = Icons.Outlined.SportsEsports,
        filledIcon = Icons.Filled.SportsEsports
    )

    companion object {
        /** Orden de presentación en el catálogo y en los filtros. */
        val all: List<ToolCategory> = listOf(
            Medir,
            Esenciales,
            Organizar,
            Generar,
            Jugar
        )
    }
}
