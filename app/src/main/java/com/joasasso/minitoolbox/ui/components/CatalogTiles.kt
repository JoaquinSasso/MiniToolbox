package com.joasasso.minitoolbox.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.tools.Tool
import com.joasasso.minitoolbox.tools.ToolCategory
import com.joasasso.minitoolbox.ui.theme.polygon
import com.joasasso.minitoolbox.ui.theme.rememberCategoryColors

/**
 * Ícono de la herramienta sobre la forma expresiva de su categoría.
 * Si la herramienta no tiene ícono propio, se usa el de la categoría.
 */
@Composable
fun CategoryShapeIcon(
    tool: Tool,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val resId = tool.svgResId
    if (resId != null) {
        CategoryShapeContainer(tool.category, size, modifier) { tint, iconModifier ->
            Icon(
                painter = painterResource(resId),
                contentDescription = null,
                tint = tint,
                modifier = iconModifier
            )
        }
    } else {
        CategoryGlyph(category = tool.category, modifier = modifier, size = size)
    }
}

/** El ícono propio de la categoría sobre su forma, para encabezados y filtros. */
@Composable
fun CategoryGlyph(
    category: ToolCategory,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    CategoryShapeContainer(category, size, modifier) { tint, iconModifier ->
        Icon(
            imageVector = category.filledIcon,
            contentDescription = null,
            tint = tint,
            modifier = iconModifier
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CategoryShapeContainer(
    category: ToolCategory,
    size: Dp,
    modifier: Modifier,
    icon: @Composable (tint: Color, iconModifier: Modifier) -> Unit
) {
    val colors = rememberCategoryColors(category)
    val shape = category.polygon().toShape()
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(colors.container),
        contentAlignment = Alignment.Center
    ) {
        icon(colors.onContainer, Modifier.size(size * 0.5f))
    }
}

/** Tarjeta de la grilla del catálogo. */
@Composable
fun ToolTile(
    tool: Tool,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 172.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 172.dp)
                .padding(start = 16.dp, end = 4.dp, top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                CategoryShapeIcon(tool = tool)
                Spacer(Modifier.weight(1f))
                FavoriteToggle(
                    category = tool.category,
                    isFavorite = isFavorite,
                    onToggle = onToggleFavorite
                )
            }

            Column(
                modifier = Modifier.padding(top = 12.dp, end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(tool.name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (tool.isPro) {
                        ProBadgePill()
                    }
                }
                tool.summary?.let { summaryRes ->
                    Text(
                        text = stringResource(summaryRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Atajo de la fila de favoritos. El arrastre lo agrega quien lo contiene, para que esta pieza
 * no dependa de la librería de reordenamiento.
 */
@Composable
fun FavoriteTile(
    tool: Tool,
    isDragging: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "favorite-drag-scale"
    )
    val rotation by animateFloatAsState(
        targetValue = if (isDragging) 12f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "favorite-drag-rotation"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .width(96.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = MaterialTheme.shapes.large,
        color = if (isDragging) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CategoryShapeIcon(
                tool = tool,
                size = 64.dp,
                modifier = Modifier.graphicsLayer { rotationZ = rotation }
            )
            Text(
                text = stringResource(tool.name),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/** Fila de resultado de búsqueda, dentro de una lista segmentada. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ToolSearchResult(
    tool: Tool,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        onClick = onClick,
        modifier = modifier,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        leadingContent = { CategoryShapeIcon(tool = tool, size = 40.dp) },
        supportingContent = tool.summary?.let { summaryRes ->
            {
                Text(
                    text = stringResource(summaryRes),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        trailingContent = {
            Text(
                text = stringResource(tool.category.titleRes),
                style = MaterialTheme.typography.labelMedium,
                color = rememberCategoryColors(tool.category).accent
            )
        }
    ) {
        Text(
            text = stringResource(tool.name),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FavoriteToggle(
    category: ToolCategory,
    isFavorite: Boolean,
    onToggle: () -> Unit
) {
    val accent = rememberCategoryColors(category).accent
    val scale by animateFloatAsState(
        targetValue = if (isFavorite) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "favorite-toggle-scale"
    )
    IconToggleButton(
        checked = isFavorite,
        onCheckedChange = { onToggle() },
        colors = IconButtonDefaults.iconToggleButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            checkedContentColor = accent
        )
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
            contentDescription = stringResource(
                if (isFavorite) R.string.categories_favorite_remove_cd
                else R.string.categories_favorite_add_cd
            ),
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        )
    }
}

@Composable
fun ProBadgePill(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Text(
            text = stringResource(R.string.categories_pro_badge),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/** Estado vacío del catálogo (favoritos sin elementos, búsqueda sin resultados). */
@Composable
fun CatalogEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (actionLabel != null && onAction != null) {
                    FilledTonalButton(
                        onClick = onAction,
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text(text = actionLabel)
                    }
                }
            }
        }
    }
}
