package com.joasasso.minitoolbox.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.tools.Tool
import com.joasasso.minitoolbox.ui.theme.rememberCategoryColors
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Hoja para ordenar y quitar favoritos. El orden importa fuera de la app: el widget de
 * favoritos muestra solo las primeras que entran según su tamaño (`FavoriteToolsWidget`).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditFavoritesSheet(
    favoriteRoutes: List<String>,
    catalogTools: List<Tool>,
    onCommitOrder: (List<String>) -> Unit,
    onRemove: (route: String) -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val toolsByRoute = remember(catalogTools) { catalogTools.associateBy { it.screen.route } }

    // Copia local para que el arrastre sea fluido; se persiste al soltar.
    val orderedRoutes = remember(favoriteRoutes, toolsByRoute) {
        favoriteRoutes.filter { it in toolsByRoute }.toMutableStateList()
    }
    // Las rutas que ya no resuelven (herramienta quitada) se conservan al final.
    val commitOrder = {
        onCommitOrder(orderedRoutes.toList() + favoriteRoutes.filterNot { it in orderedRoutes })
    }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState = listState) { from, to ->
        // Los índices incluyen el encabezado; se mueve por clave para no depender de eso.
        val fromIndex = orderedRoutes.indexOf(from.key)
        val toIndex = orderedRoutes.indexOf(to.key)
        if (fromIndex >= 0 && toIndex >= 0) {
            orderedRoutes.add(toIndex, orderedRoutes.removeAt(fromIndex))
            haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
    }

    val moveUp = stringResource(R.string.catalog_move_up)
    val moveDown = stringResource(R.string.catalog_move_down)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        )
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item(key = "edit_favorites_header") {
                Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 16.dp)) {
                    Text(
                        text = stringResource(R.string.catalog_edit_favorites_title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = stringResource(
                            if (orderedRoutes.isEmpty()) R.string.categories_empty_favorites_subtitle
                            else R.string.catalog_edit_favorites_subtitle
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            itemsIndexed(items = orderedRoutes, key = { _, route -> route }) { index, route ->
                val tool = toolsByRoute.getValue(route)
                ReorderableItem(reorderState, key = route) { isDragging ->
                    val handleModifier = Modifier.draggableHandle(
                        onDragStarted = {
                            haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                        },
                        onDragStopped = {
                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            commitOrder()
                        }
                    )

                    ListItem(
                        modifier = Modifier
                            // Alternativa al arrastre para TalkBack y Switch Access.
                            .semantics {
                                customActions = buildList {
                                    if (index > 0) {
                                        add(CustomAccessibilityAction(moveUp) {
                                            orderedRoutes.add(index - 1, orderedRoutes.removeAt(index))
                                            commitOrder()
                                            true
                                        })
                                    }
                                    if (index < orderedRoutes.lastIndex) {
                                        add(CustomAccessibilityAction(moveDown) {
                                            orderedRoutes.add(index + 1, orderedRoutes.removeAt(index))
                                            commitOrder()
                                            true
                                        })
                                    }
                                }
                            },
                        shapes = ListItemDefaults.segmentedShapes(
                            index = index,
                            count = orderedRoutes.size
                        ),
                        colors = ListItemDefaults.colors(
                            containerColor = if (isDragging) {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            }
                        ),
                        leadingContent = { CategoryShapeIcon(tool = tool, size = 40.dp) },
                        supportingContent = {
                            Text(
                                text = stringResource(tool.category.titleRes),
                                color = rememberCategoryColors(tool.category).accent
                            )
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onRemove(route) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = stringResource(
                                            R.string.categories_favorite_remove_cd
                                        ),
                                        tint = rememberCategoryColors(tool.category).accent
                                    )
                                }
                                Box(
                                    modifier = handleModifier.size(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DragHandle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(tool.name),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
