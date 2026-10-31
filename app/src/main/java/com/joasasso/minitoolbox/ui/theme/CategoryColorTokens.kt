package com.joasasso.minitoolbox.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.graphics.shapes.RoundedPolygon
import com.google.android.material.color.MaterialColors
import com.joasasso.minitoolbox.tools.ToolCategory

/**
 * Roles de color de una categoría, con la misma estructura que MD3 usa para colores propios
 * (accent / onAccent / container / onContainer).
 */
@Immutable
data class CategoryColors(
    val accent: Color,
    val onAccent: Color,
    val container: Color,
    val onContainer: Color
)

/**
 * Semilla de cada categoría. No se usa tal cual: se armoniza con el `primary` del tema activo
 * (incluido el color dinámico de Android 12+) y de ahí se derivan los tonos, así que el
 * contraste de cada par lo garantiza el algoritmo de MDC y no un hex elegido a mano.
 */
private val CategorySeeds: Map<ToolCategory, Color> = mapOf(
    ToolCategory.Medir to Color(0xFF00897B),
    ToolCategory.Esenciales to Color(0xFFF2A900),
    ToolCategory.Organizar to Color(0xFF3D6BE0),
    ToolCategory.Generar to Color(0xFF8B55D9),
    ToolCategory.Jugar to Color(0xFFE2506F)
)

@Composable
fun rememberCategoryColors(category: ToolCategory): CategoryColors {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.surface.luminance() > 0.5f
    val primary = scheme.primary
    return remember(category, primary, isLight) {
        val seed = CategorySeeds.getValue(category).toArgb()
        val harmonized = MaterialColors.harmonize(seed, primary.toArgb())
        val roles = MaterialColors.getColorRoles(harmonized, isLight)
        CategoryColors(
            accent = Color(roles.accent),
            onAccent = Color(roles.onAccent),
            container = Color(roles.accentContainer),
            onContainer = Color(roles.onAccentContainer)
        )
    }
}

/**
 * Forma expresiva que identifica a la categoría junto con su color, para que la distinción
 * no dependa solo del tono (daltonismo, modo de alto contraste).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun ToolCategory.polygon(): RoundedPolygon = when (this) {
    ToolCategory.Medir -> MaterialShapes.Cookie4Sided
    ToolCategory.Esenciales -> MaterialShapes.Sunny
    ToolCategory.Organizar -> MaterialShapes.Clover4Leaf
    ToolCategory.Generar -> MaterialShapes.SoftBurst
    ToolCategory.Jugar -> MaterialShapes.Puffy
}
