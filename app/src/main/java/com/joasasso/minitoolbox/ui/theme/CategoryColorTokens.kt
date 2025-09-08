package com.joasasso.minitoolbox.ui.theme

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.tools.ToolCategory
import kotlin.math.max
import kotlin.math.min

data class CategorySwatch(val vibrant: Color, val onVibrant: Color)

private fun darken(color: Color, factor: Float): Color {
    val f = 1f - factor
    return Color(
        red = max(0f, min(1f, color.red * f)),
        green = max(0f, min(1f, color.green * f)),
        blue = max(0f, min(1f, color.blue * f)),
        alpha = color.alpha
    )
}

fun swatchFromBase(base: Color) = CategorySwatch(base, darken(base, 0.5f))

// Paleta base por categoría (ajustá tonos a tu gusto)
val CategoryPalette: Map<ToolCategory, CategorySwatch> = mapOf(
    ToolCategory.Herramientas to swatchFromBase(Color(0xFF7AD1FF)),
    ToolCategory.Organizacion to swatchFromBase(Color(0xFFFFD580)),
    ToolCategory.Informacion  to swatchFromBase(Color(0xFFA6E3B1)),
    ToolCategory.Entretenimiento to swatchFromBase(Color(0xFFBFAAFF)),
    ToolCategory.Favoritos    to swatchFromBase(Color(0xFFFF9F9F)),
)

val SubcategoryPalette: Map<Int, CategorySwatch> = mapOf(
    R.string.subcategory_generator   to swatchFromBase(Color(0xFF5CC8FF)), // Sky
    R.string.subcategory_random      to swatchFromBase(Color(0xFFFFB155)), // Amber dulce
    R.string.subcategory_calculator  to swatchFromBase(Color(0xFF6FD3A6)), // Verde menta
    R.string.subcategory_instrument  to swatchFromBase(Color(0xFF888888)), // Violeta lavanda
    R.string.subcategory_dates       to swatchFromBase(Color(0xFFFF8A80)), // Coral suave
    R.string.subcategory_general     to swatchFromBase(Color(0xFF4CCDC4)), // Turquesa
    R.string.subcategory_habits      to swatchFromBase(Color(0xFF8EA6FF)), // Azul violeta
    R.string.subcategory_others      to swatchFromBase(Color(0xFFB0EEFF)), // Azul hielo
    R.string.subcategory_minigames   to swatchFromBase(Color(0xFFDDE25C)), // Lima cálida
    R.string.subcategory_scoreboards to swatchFromBase(Color(0xFFFF88C2))  // Rosa fresa
)

fun swatchForCategory(category: ToolCategory): CategorySwatch =
    CategoryPalette[category] ?: swatchFromBase(Color(0xFF9E9E9E))

fun swatchForSubcategory(@StringRes subCategory: Int?): CategorySwatch? =
    subCategory?.let { SubcategoryPalette[it] }

/** Círculo vibrante + icono en el mismo matiz más oscuro */
@Composable
fun CategoryIcon(
    painter: Painter,
    contentDescription: String?,
    swatch: CategorySwatch,
    size: Dp = 40.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(swatch.vibrant, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = swatch.onVibrant,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}