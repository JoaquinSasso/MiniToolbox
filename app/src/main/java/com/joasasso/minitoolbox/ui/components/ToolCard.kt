package com.joasasso.minitoolbox.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    overrideElevation: Dp? = null
) {

    val swatch = swatchForSubcategory(tool.subCategory) ?: swatchForCategory(tool.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = bottomPadding)
            .clickable { onToolClick(tool) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = overrideElevation ?: 6.dp),
        shape = shape
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono: círculo vibrante + pictograma más oscuro
            tool.svgResId?.let { resId ->
                CategoryIcon(
                    painter = painterResource(resId),
                    contentDescription = null,
                    swatch = swatch,
                    size = 40.dp
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = tool.name),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (tool.isPro) Color(0xFFC79300) else MaterialTheme.colorScheme.onSurface
                )
                tool.summary?.let { sumRes ->
                    Text(
                        text = stringResource(id = sumRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            IconButton(onClick = onToggleFavorito) {
                Icon(
                    imageVector = if (isFavorito) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorito) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

//Utilidades para agrupar las cards de las categorías
/** Datos de decoración por grupo (shape + paddings verticales). */
data class GroupDecor(
    val shape: Shape,
    val topPad: Dp,
    val bottomPad: Dp
)

fun groupDecorBySubcategory(index: Int, list: List<Tool>): GroupDecor {
    if (list.isEmpty()) {
        return GroupDecor(
            RoundedCornerShape(16.dp), 8.dp, 8.dp
        )
    }

    val safeIndex = index.coerceIn(0, list.lastIndex)
    val cur = list.getOrNull(safeIndex)
        ?: return GroupDecor(
            RoundedCornerShape(16.dp), 8.dp, 8.dp
        )
    val prevSame = safeIndex > 0 && list[safeIndex - 1].subCategory == cur.subCategory
    val nextSame = safeIndex < list.lastIndex && list[safeIndex + 1].subCategory == cur.subCategory

    return when {
        !prevSame && !nextSame -> GroupDecor(
            RoundedCornerShape(16.dp), 8.dp, 8.dp
        )
        !prevSame && nextSame -> GroupDecor(
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
            8.dp, 2.dp
        )
        prevSame && nextSame -> GroupDecor(
            RoundedCornerShape(4.dp),
            2.dp, 2.dp
        )
        else /* prevSame && !nextSame */ -> GroupDecor(
            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            2.dp, 8.dp
        )
    }
}