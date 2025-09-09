package com.joasasso.minitoolbox

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.protobuf.LazyStringArrayList.emptyList
import com.joasasso.minitoolbox.data.flujoToolsFavoritas
import com.joasasso.minitoolbox.data.setFavoritesOrder
import com.joasasso.minitoolbox.data.toogleFavorite
import com.joasasso.minitoolbox.tools.Tool
import com.joasasso.minitoolbox.tools.ToolCategory
import com.joasasso.minitoolbox.tools.ToolRegistry
import com.joasasso.minitoolbox.ui.theme.CategoryIcon
import com.joasasso.minitoolbox.ui.theme.swatchForCategory
import com.joasasso.minitoolbox.ui.theme.swatchForSubcategory
import com.joasasso.minitoolbox.viewmodel.CategoryViewModel
import com.joasasso.minitoolbox.widgets.actualizarWidgetFavoritos
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    tools: List<Tool>,
    onToolClick: (Tool) -> Unit,
    vm: CategoryViewModel = viewModel()
) {
    val selectedCategory by vm.selectedCategory
    val haptic = LocalHapticFeedback.current
    val categories = ToolCategory.all

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val orderedRoutes by remember(context) { context.flujoToolsFavoritas() }.collectAsState(initial = emptyList())

    // Mantiene el orden guardado para favoritos
    val favoritesTools: List<Tool> =
        orderedRoutes.mapNotNull { route -> tools.find { it.screen.route == route } }

    val displayList = if (selectedCategory == ToolCategory.Favoritos) {
        favoritesTools                    // ← respeta el orden guardado
    } else {
        tools.filter { it.category == selectedCategory }.sortedBy { it.subCategory }
    }

    // Estado de scroll por categoría
    val listState = rememberSaveable(selectedCategory, saver = LazyListState.Saver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(selectedCategory.titleRes)) },
                actions = {
                        IconButton(onClick = {
                            onToolClick(ToolRegistry.tools.last())
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }) {
                            Icon(Icons.Filled.Info, contentDescription = "Información")
                        }
                },
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                categories.forEach { category ->
                    val isSelected = category == selectedCategory
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.selectedCategory.value = category
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) category.filledIcon else category.outlinedIcon,
                                contentDescription = stringResource(category.titleRes),
                                modifier = Modifier.size(25.dp)
                            )
                        },
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        key(selectedCategory) {
            // --- FAVORITOS: Drag & Drop con guardado automático ---
            if (selectedCategory == ToolCategory.Favoritos) {
                // Copia local reordenable (rutas)
                val favRoutes = remember(orderedRoutes) { orderedRoutes.toMutableStateList() }

                // Estado de DnD (Calvin-LL), mueve en RAM durante el drag
                val reorderState = rememberReorderableLazyListState(
                    lazyListState = listState
                ) { from, to ->
                    favRoutes.move(from.index, to.index)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = innerPadding.calculateTopPadding() + 12.dp,
                            bottom = innerPadding.calculateBottomPadding() + 4.dp
                        )
                ){
                    if (selectedCategory == ToolCategory.Favoritos) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.favorites_reorder_info),fontSize = 13.sp)
                        }
                    }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(favRoutes, key = { _, route -> route }) { index, route ->
                        val tool = tools.find { it.screen.route == route } ?: return@itemsIndexed
                        // Lista de Tool según el orden actual para las decoraciones
                        val toolsForDecor =
                            favRoutes.mapNotNull { r -> tools.find { it.screen.route == r } }
                        val decor = groupDecorBySubcategory(index, toolsForDecor)

                        ReorderableItem(reorderState, key = route) { isDragging ->
                            val elev = animateDpAsState(
                                targetValue = if (isDragging) 10.dp else 6.dp,
                                label = "drag-elev"
                            ).value

                            // Long-press en toda la card para iniciar arrastre; persiste al soltar
                            val dragMod = with(this) {
                                Modifier.longPressDraggableHandle(
                                    onDragStarted = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragStopped = {
                                        scope.launch {
                                            context.setFavoritesOrder(favRoutes.toList())
                                            actualizarWidgetFavoritos(context)
                                        }
                                    }
                                )
                            }

                            Box(modifier = dragMod) {
                                ToolCard(
                                    tool = tool,
                                    isFavorito = true,
                                    shape = decor.shape,
                                    topPadding = decor.topPad,
                                    bottomPadding = decor.bottomPad,
                                    onToolClick = onToolClick,
                                    onToggleFavorito = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        scope.launch {
                                            context.toogleFavorite(tool.screen.route)
                                            // reflejo inmediato en UI
                                            favRoutes.remove(tool.screen.route)
                                            actualizarWidgetFavoritos(context)
                                        }
                                    },
                                    overrideElevation = elev
                                )
                            }
                        }
                    }

                    if (favRoutes.isEmpty()) {
                        item {
                            Spacer(Modifier.height(4.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Error,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        text = stringResource(R.string.no_tools_found),
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            } else {
                // --- RESTO DE CATEGORÍAS (sin DnD) ---
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = innerPadding.calculateTopPadding() + 16.dp,
                        bottom = innerPadding.calculateBottomPadding() + 16.dp
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(displayList, key = { _, tool -> tool.screen.route }) { index, tool ->
                        val isFavorito = orderedRoutes.contains(tool.screen.route)
                        val decor = groupDecorBySubcategory(index, displayList)

                        ToolCard(
                            tool = tool,
                            isFavorito = isFavorito,
                            shape = decor.shape,
                            topPadding = decor.topPad,
                            bottomPadding = decor.bottomPad,
                            onToolClick = onToolClick,
                            onToggleFavorito = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    context.toogleFavorite(tool.screen.route)
                                    actualizarWidgetFavoritos(context)
                                }
                            }
                        )
                    }

                    if (displayList.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Error,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        text = stringResource(R.string.no_tools_found),
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Datos de decoración por grupo (shape + paddings verticales). */
private data class GroupDecor(
    val shape: Shape,
    val topPad: Dp,
    val bottomPad: Dp
)

private fun groupDecorBySubcategory(index: Int, list: List<Tool>): GroupDecor {
    val cur = list[index]
    val prevSame = index > 0 && list[index - 1].subCategory == cur.subCategory
    val nextSame = index < list.lastIndex && list[index + 1].subCategory == cur.subCategory

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
    val haptic = LocalHapticFeedback.current

    val swatch = swatchForSubcategory(tool.subCategory) ?: swatchForCategory(tool.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = bottomPadding)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToolClick(tool)
            },
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
                    style = MaterialTheme.typography.bodyLarge
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

/* ---------- Utils ---------- */
private fun <T> MutableList<T>.move(from: Int, to: Int) {
    if (from == to) return
    val element = removeAt(from)
    add(to, element)
}
