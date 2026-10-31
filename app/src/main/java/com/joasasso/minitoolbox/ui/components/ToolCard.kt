package com.joasasso.minitoolbox.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.tools.Tool
import com.joasasso.minitoolbox.ui.theme.CategoryIcon
import com.joasasso.minitoolbox.ui.theme.swatchForCategory
import com.joasasso.minitoolbox.ui.theme.swatchForSubcategory

@Composable
fun ToolCard(
    tool: Tool,
    isFavorito: Boolean,
    shape: Shape,
    topPadding: Dp,
    bottomPadding: Dp,
    onToolClick: (Tool) -> Unit,
    onToggleFavorito: () -> Unit,
    modifier: Modifier = Modifier,
    overrideElevation: Dp? = null,
    isDragging: Boolean = false,
    showSubcategoryBadge: Boolean = false,
    showDragHandle: Boolean = false,
    dragHandleModifier: Modifier = Modifier
) {
    val swatch = swatchForSubcategory(tool.subCategory) ?: swatchForCategory(tool.category)
    val containerColor = if (isDragging) {
        MaterialTheme.colorScheme.surfaceContainerHighest
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    Card(
        onClick = { onToolClick(tool) },
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = bottomPadding),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = overrideElevation ?: 0.dp
        ),
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showDragHandle) {
                Box(
                    modifier = dragHandleModifier
                        .padding(end = 6.dp)
                        .size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            tool.svgResId?.let { resId ->
                CategoryIcon(
                    painter = painterResource(resId),
                    contentDescription = null,
                    swatch = swatch,
                    size = 46.dp,
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(id = tool.name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (tool.isPro) {
                        ProBadgePill()
                    }
                }

                if (showSubcategoryBadge) {
                    Text(
                        text = stringResource(id = tool.subCategory),
                        style = MaterialTheme.typography.labelSmall,
                        color = swatch.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                tool.summary?.let { sumRes ->
                    Text(
                        text = stringResource(id = sumRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            FavoriteToggleButton(
                isFavorite = isFavorito,
                onToggle = onToggleFavorito
            )
        }
    }
}

@Composable
fun ToolBentoCard(
    tool: Tool,
    isFavorito: Boolean,
    onToolClick: (Tool) -> Unit,
    onToggleFavorito: () -> Unit,
    modifier: Modifier = Modifier
) {
    val swatch = swatchForSubcategory(tool.subCategory) ?: swatchForCategory(tool.category)

    Card(
        onClick = { onToolClick(tool) },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                tool.svgResId?.let { resId ->
                    CategoryIcon(
                        painter = painterResource(resId),
                        contentDescription = null,
                        swatch = swatch,
                        size = 44.dp,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (tool.isPro) {
                        ProBadgePill()
                    }
                    FavoriteToggleButton(
                        isFavorite = isFavorito,
                        onToggle = onToggleFavorito
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(id = tool.subCategory),
                style = MaterialTheme.typography.labelSmall,
                color = swatch.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = stringResource(id = tool.name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            tool.summary?.let { sumRes ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(id = sumRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun FavoriteQuickCard(
    tool: Tool,
    onToolClick: (Tool) -> Unit,
    modifier: Modifier = Modifier
) {
    val swatch = swatchForSubcategory(tool.subCategory) ?: swatchForCategory(tool.category)

    Surface(
        onClick = { onToolClick(tool) },
        modifier = modifier.width(156.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tool.svgResId?.let { resId ->
                    CategoryIcon(
                        painter = painterResource(resId),
                        contentDescription = null,
                        swatch = swatch,
                        size = 36.dp,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = stringResource(id = tool.name),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = stringResource(id = tool.subCategory),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FavoriteToggleButton(
    isFavorite: Boolean,
    onToggle: () -> Unit
) {
    val cd = stringResource(
        if (isFavorite) R.string.categories_favorite_remove_cd
        else R.string.categories_favorite_add_cd
    )
    IconButton(onClick = onToggle) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    if (isFavorite) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = cd,
                tint = if (isFavorite) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
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

/** Datos de decoración por grupo expresivo MD3 (shape + paddings verticales). */
data class GroupDecor(
    val shape: Shape,
    val topPad: Dp,
    val bottomPad: Dp
)

fun groupDecorBySubcategory(index: Int, list: List<Tool>): GroupDecor {
    val outerCorner = 20.dp
    val innerCorner = 4.dp
    if (list.isEmpty()) {
        return GroupDecor(RoundedCornerShape(outerCorner), 4.dp, 4.dp)
    }

    val safeIndex = index.coerceIn(0, list.lastIndex)
    val cur = list.getOrNull(safeIndex)
        ?: return GroupDecor(RoundedCornerShape(outerCorner), 4.dp, 4.dp)

    val prevSame = safeIndex > 0 && list[safeIndex - 1].subCategory == cur.subCategory
    val nextSame = safeIndex < list.lastIndex && list[safeIndex + 1].subCategory == cur.subCategory

    return when {
        !prevSame && !nextSame -> GroupDecor(
            shape = RoundedCornerShape(outerCorner),
            topPad = 4.dp,
            bottomPad = 4.dp
        )

        !prevSame && nextSame -> GroupDecor(
            shape = RoundedCornerShape(
                topStart = outerCorner,
                topEnd = outerCorner,
                bottomStart = innerCorner,
                bottomEnd = innerCorner
            ),
            topPad = 4.dp,
            bottomPad = 1.dp
        )

        prevSame && nextSame -> GroupDecor(
            shape = RoundedCornerShape(innerCorner),
            topPad = 1.dp,
            bottomPad = 1.dp
        )

        else -> GroupDecor(
            shape = RoundedCornerShape(
                topStart = innerCorner,
                topEnd = innerCorner,
                bottomStart = outerCorner,
                bottomEnd = outerCorner
            ),
            topPad = 1.dp,
            bottomPad = 4.dp
        )
    }
}
