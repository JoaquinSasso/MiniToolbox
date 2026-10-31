package com.joasasso.minitoolbox.ui.theme

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.tools.ToolCategory

/**
 * Par tonal MD3 (container + onContainer + accent) adaptado al tema claro/oscuro.
 */
data class CategorySwatch(
    val container: Color,
    val onContainer: Color,
    val accent: Color = onContainer
) {
    val vibrant: Color get() = container
    val onVibrant: Color get() = onContainer
}

data class DualThemeSwatch(
    val light: CategorySwatch,
    val dark: CategorySwatch
) {
    fun resolve(isDark: Boolean): CategorySwatch = if (isDark) dark else light
}

// --- Paletas tonales MD3 (Tone 90 / Tone 20 en Light, Tone 26 / Tone 88 en Dark) ---

private val TealDualSwatch = DualThemeSwatch(
    light = CategorySwatch(
        container = Color(0xFFCCF3F6),
        onContainer = Color(0xFF004F54),
        accent = Color(0xFF006970)
    ),
    dark = CategorySwatch(
        container = Color(0xFF004F54),
        onContainer = Color(0xFF8AF2FA),
        accent = Color(0xFF4CD9E2)
    )
)

private val SkyCyanDualSwatch = DualThemeSwatch(
    light = CategorySwatch(
        container = Color(0xFFD0E8FF),
        onContainer = Color(0xFF004877),
        accent = Color(0xFF00629E)
    ),
    dark = CategorySwatch(
        container = Color(0xFF004877),
        onContainer = Color(0xFFCFE5FF),
        accent = Color(0xFF82CFFF)
    )
)

private val IndigoDualSwatch = DualThemeSwatch(
    light = CategorySwatch(
        container = Color(0xFFDDE1FF),
        onContainer = Color(0xFF14277E),
        accent = Color(0xFF3954B4)
    ),
    dark = CategorySwatch(
        container = Color(0xFF233992),
        onContainer = Color(0xFFDDE1FF),
        accent = Color(0xFFB7C4FF)
    )
)

private val EmeraldDualSwatch = DualThemeSwatch(
    light = CategorySwatch(
        container = Color(0xFFC8F5DC),
        onContainer = Color(0xFF005136),
        accent = Color(0xFF0B6B4A)
    ),
    dark = CategorySwatch(
        container = Color(0xFF005136),
        onContainer = Color(0xFFA6F2C6),
        accent = Color(0xFF6CD9A0)
    )
)

private val AmberDualSwatch = DualThemeSwatch(
    light = CategorySwatch(
        container = Color(0xFFFFE0B2),
        onContainer = Color(0xFF5C3800),
        accent = Color(0xFF8A5600)
    ),
    dark = CategorySwatch(
        container = Color(0xFF5E3A00),
        onContainer = Color(0xFFFFDEA8),
        accent = Color(0xFFFFB945)
    )
)

private val VioletDualSwatch = DualThemeSwatch(
    light = CategorySwatch(
        container = Color(0xFFEBDDFF),
        onContainer = Color(0xFF44227A),
        accent = Color(0xFF65439C)
    ),
    dark = CategorySwatch(
        container = Color(0xFF4C2B82),
        onContainer = Color(0xFFEBDDFF),
        accent = Color(0xFFD3BBFF)
    )
)

private val RoseDualSwatch = DualThemeSwatch(
    light = CategorySwatch(
        container = Color(0xFFFFD9E3),
        onContainer = Color(0xFF630A35),
        accent = Color(0xFF932757)
    ),
    dark = CategorySwatch(
        container = Color(0xFF671039),
        onContainer = Color(0xFFFFD9E3),
        accent = Color(0xFFFFB0C9)
    )
)

private val CoralDualSwatch = DualThemeSwatch(
    light = CategorySwatch(
        container = Color(0xFFFFDBD1),
        onContainer = Color(0xFF691D00),
        accent = Color(0xFF96330E)
    ),
    dark = CategorySwatch(
        container = Color(0xFF6E2103),
        onContainer = Color(0xFFFFDBD1),
        accent = Color(0xFFFFB5A0)
    )
)

private val GoldFavoritesDualSwatch = DualThemeSwatch(
    light = CategorySwatch(
        container = Color(0xFFFFE599),
        onContainer = Color(0xFF543D00),
        accent = Color(0xFF795900)
    ),
    dark = CategorySwatch(
        container = Color(0xFF574000),
        onContainer = Color(0xFFFFE086),
        accent = Color(0xFFF6C228)
    )
)

private val CategoryDualPalette: Map<ToolCategory, DualThemeSwatch> = mapOf(
    ToolCategory.Instrumentos to TealDualSwatch,
    ToolCategory.Cotidiano to IndigoDualSwatch,
    ToolCategory.Generadores to AmberDualSwatch,
    ToolCategory.Juegos to RoseDualSwatch,
    ToolCategory.Favoritos to GoldFavoritesDualSwatch
)

private val SubcategoryDualPalette: Map<Int, DualThemeSwatch> = mapOf(
    R.string.subcategory_measurement to TealDualSwatch,
    R.string.subcategory_sensors to SkyCyanDualSwatch,
    R.string.subcategory_instrument to TealDualSwatch,
    R.string.subcategory_habits to IndigoDualSwatch,
    R.string.subcategory_calculator to EmeraldDualSwatch,
    R.string.subcategory_generator to VioletDualSwatch,
    R.string.subcategory_random to AmberDualSwatch,
    R.string.subcategory_scoreboards to RoseDualSwatch,
    R.string.subcategory_minigames to CoralDualSwatch,
    R.string.subcategory_others to IndigoDualSwatch
)

@Composable
fun swatchForCategory(
    category: ToolCategory,
    isDark: Boolean = isSystemInDarkTheme()
): CategorySwatch =
    (CategoryDualPalette[category] ?: IndigoDualSwatch).resolve(isDark)

@Composable
fun swatchForSubcategory(
    @StringRes subCategory: Int?,
    isDark: Boolean = isSystemInDarkTheme()
): CategorySwatch? =
    subCategory?.let { SubcategoryDualPalette[it]?.resolve(isDark) }

/**
 * Contenedor expresivo MD3 (squircle redondeado con par tonal container / onContainer).
 */
@Composable
fun CategoryIcon(
    painter: Painter,
    contentDescription: String?,
    swatch: CategorySwatch,
    size: Dp = 44.dp,
    shape: Shape = RoundedCornerShape(14.dp)
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(swatch.container, shape)
            .border(
                width = 1.dp,
                color = swatch.accent.copy(alpha = 0.16f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = swatch.onContainer,
            modifier = Modifier.size(size * 0.54f)
        )
    }
}
