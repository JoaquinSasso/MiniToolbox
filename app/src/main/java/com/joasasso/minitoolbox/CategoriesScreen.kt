package com.joasasso.minitoolbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.protobuf.LazyStringArrayList.emptyList
import com.joasasso.minitoolbox.data.flujoToolsFavoritas
import com.joasasso.minitoolbox.data.setFavoritesOrder
import com.joasasso.minitoolbox.data.toogleFavorite
import com.joasasso.minitoolbox.tools.Tool
import com.joasasso.minitoolbox.tools.ToolCategory
import com.joasasso.minitoolbox.ui.theme.CategoryIcon
import com.joasasso.minitoolbox.ui.theme.swatchForCategory
import com.joasasso.minitoolbox.ui.theme.swatchForSubcategory
import com.joasasso.minitoolbox.viewmodel.CategoryViewModel
import com.joasasso.minitoolbox.widgets.actualizarWidgetFavoritos
import kotlinx.coroutines.launch

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
    var showReorder by remember { mutableStateOf(false) }


    // Filtrado por categoría seleccionada
    val filteredTools = if (selectedCategory == ToolCategory.Favoritos) {
        tools.filter { orderedRoutes.contains(it.screen.route) }
    } else {
        tools.filter { it.category == selectedCategory }
    }

    // Agrupación:
    val favoritesTools: List<Tool> =
        orderedRoutes.mapNotNull { route -> tools.find { it.screen.route == route } }

    val displayList = if (selectedCategory == ToolCategory.Favoritos) {
        favoritesTools                    // ← respeta el orden guardado
    } else {
        tools.filter { it.category == selectedCategory }.sortedBy { it.subCategory }
    }

    // Estado de scroll NUEVO por categoría (sin frame intermedio con offset viejo)
    val listState = rememberSaveable(selectedCategory, saver = LazyListState.Saver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }




    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(selectedCategory.titleRes)) },
                actions = {
                    if (selectedCategory == ToolCategory.Favoritos && orderedRoutes.size > 1) {
                        IconButton(onClick = { showReorder = true }) {
                            Icon(
                                imageVector = Icons.Default.Reorder, // si no lo tenés, usa MoreVert/Sort
                                contentDescription = "Reordenar favoritos"
                            )
                        }
                    }
                }
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
        key(selectedCategory) { // fuerza recomposición aislada por categoría
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
                        // color por subcategoría
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
    if (showReorder) {
        ReorderFavoritesSheet(
            routes = orderedRoutes,
            allTools = tools,
            onDismiss = { showReorder = false },
            onSave = { newRoutes ->
                scope.launch {
                    context.setFavoritesOrder(newRoutes)
                    actualizarWidgetFavoritos(context)
                    showReorder = false
                }
            }
        )
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
    onToggleFavorito: () -> Unit
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
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReorderFavoritesSheet(
    routes: List<String>,
    allTools: List<Tool>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val byRoute = remember(allTools) { allTools.associateBy { it.screen.route } }
    val items = remember(routes) { routes.toMutableStateList() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Text("Ordenar favoritos", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                itemsIndexed(items, key = { _, r -> r }) { index, route ->
                    val tool = byRoute[route] ?: return@itemsIndexed
                    ReorderRow(
                        index = index,
                        tool = tool,
                        onMoveUp = {
                            if (index > 0) {
                                val tmp = items.removeAt(index)
                                items.add(index - 1, tmp)
                            }
                        },
                        onMoveDown = {
                            if (index < items.lastIndex) {
                                val tmp = items.removeAt(index)
                                items.add(index + 1, tmp)
                            }
                        },
                        onRemove = {
                            items.removeAt(index)
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onSave(items.toList()) },
                    enabled = items.isNotEmpty()
                ) { Text(stringResource(R.string.save)) }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ReorderRow(
    index: Int,
    tool: Tool,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mini icono expresivo (mismo estilo de lista)
            tool.svgResId?.let { resId ->
                val swatch = swatchForSubcategory(tool.subCategory) ?: swatchForCategory(tool.category)
                CategoryIcon(painter = painterResource(resId), contentDescription = null, swatch = swatch, size = 32.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text = stringResource(tool.name), style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                tool.summary?.let { sum ->
                    Text(
                        text = stringResource(sum),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            IconButton(onClick = onMoveUp, enabled = index > 0) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir")
            }
            IconButton(onClick = onMoveDown, enabled = true) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bajar")
            }
        }
    }
}


