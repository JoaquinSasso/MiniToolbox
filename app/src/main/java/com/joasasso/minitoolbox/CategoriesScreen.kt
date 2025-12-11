package com.joasasso.minitoolbox

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.protobuf.LazyStringArrayList.emptyList
import com.joasasso.minitoolbox.data.flujoToolsFavoritas
import com.joasasso.minitoolbox.data.setFavoritesOrder
import com.joasasso.minitoolbox.data.toogleFavorite
import com.joasasso.minitoolbox.tools.Tool
import com.joasasso.minitoolbox.tools.ToolCategory
import com.joasasso.minitoolbox.ui.components.ProPassBadge
import com.joasasso.minitoolbox.ui.components.ProToolPaywallDialog
import com.joasasso.minitoolbox.ui.components.ToolCard
import com.joasasso.minitoolbox.ui.components.groupDecorBySubcategory
import com.joasasso.minitoolbox.utils.ads.RewardedManager
import com.joasasso.minitoolbox.utils.buildSearchIndexForAllTools
import com.joasasso.minitoolbox.utils.matchesQuery
import com.joasasso.minitoolbox.utils.pro.CreditAccessManager
import com.joasasso.minitoolbox.utils.pro.LocalProState
import com.joasasso.minitoolbox.viewmodel.CategoryViewModel
import com.joasasso.minitoolbox.widgets.actualizarWidgetFavoritos
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun CategoriesScreen(
    tools: List<Tool>,
    onToolClick: (Tool) -> Unit,
    vm: CategoryViewModel = viewModel(),
    onNavigateToPro: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val selectedCategory by vm.selectedCategory
    val haptic = LocalHapticFeedback.current
    val categories = ToolCategory.all

    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val proState = LocalProState.current
    var showPaywallDialog by remember { mutableStateOf(false) }
    var selectedProTool by remember { mutableStateOf<Tool?>(null) }

    val handleToolClick: (Tool) -> Unit = { tool ->
        if (tool.isPro && !proState.isPro && !CreditAccessManager.hasActivePass(context)) {
            selectedProTool = tool
            showPaywallDialog = true
        } else {
            onToolClick(tool)
        }
    }

    val orderedRoutes by remember(context) { context.flujoToolsFavoritas() }.collectAsState(initial = emptyList())

    // Mantiene el orden guardado para favoritos
    val favoritesTools: List<Tool> =
        orderedRoutes.mapNotNull { route -> tools.find { it.screen.route == route } }

    val baseListForCategory = if (selectedCategory == ToolCategory.Favoritos) {
        favoritesTools                    // ← respeta el orden guardado
    } else {
        tools.filter { it.category == selectedCategory }.sortedBy { it.subCategory }.reversed()
    }

    // Estado de scroll por categoría (persistente)
    val listState = rememberSaveable(selectedCategory, saver = LazyListState.Saver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }

    // ===== Buscador global =====
    var query by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current

    val locales = LocalConfiguration.current.locales
    val locale = locales.get(0) ?: Locale.getDefault()

    // Índice global de tools (título+resumen localizados)
    val searchIndex by remember(tools, locale, context) {
        mutableStateOf(buildSearchIndexForAllTools(context, tools))
    }

    // Debounce para fluidez
    var debouncedQuery by remember { mutableStateOf("") }
    LaunchedEffect(query) {
        snapshotFlow { query.trim() }
            .distinctUntilChanged()
            .debounce(120)
            .collectLatest { debouncedQuery = it }
    }

    // Si hay query → resultados globales; si no → lista normal por categoría
    val effectiveList: List<Tool> = remember(debouncedQuery, searchIndex, locale, selectedCategory, baseListForCategory, tools) {
        if (debouncedQuery.isBlank()) {
            baseListForCategory
        } else {
            val matchingRoutes = searchIndex.asSequence()
                .filter { matchesQuery(it, debouncedQuery, locale) }
                .map { it.route }
                .toSet()
            tools.filter { it.screen.route in matchingRoutes }
        }
    }
    // Limpia query al cambiar de categoría
    LaunchedEffect(selectedCategory) {
        query = ""
    }

    // Limpia query al volver a la screen desde una tool
    LaunchedEffect(Unit) {
        query = ""
    }

    // Detecta si el teclado (IME) está visible.
    // En Compose recientes también podés usar: val isKeyboardOpen = WindowInsets.isImeVisible
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0

    // 1) Primer back: si el teclado está abierto, lo cierra (consume el back)
    BackHandler(enabled = isKeyboardOpen) {
        focusManager.clearFocus() // cierra el teclado
    }

    // 2) Segundo back: si no hay teclado pero hay texto, limpia la búsqueda (consume el back)
    BackHandler(enabled = !isKeyboardOpen && query.isNotBlank()) {
        query = ""
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(selectedCategory.titleRes)) },
                actions = {
                    ProPassBadge(
                        modifier = Modifier.padding(end = 6.dp),
                        onClick = { onNavigateToPro() }
                    )
                    if (!proState.isPro) {
                        IconButton(onClick = onNavigateToPro) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = stringResource(R.string.pro_screen_title),
                                tint = Color(0xFFFFD700)
                            )
                        }
                    }
                    IconButton(onClick = {
                        onNavigateToAbout()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }) {
                        Icon(Icons.Filled.Info, contentDescription = stringResource(R.string.information))
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // --- Cuadro de búsqueda global (siempre visible arriba) ---
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = innerPadding.calculateTopPadding() + 12.dp
                        ),
                    placeholder = { Text(text = stringResource(R.string.categories_search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.categories_search_clear_cd))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { focusManager.clearFocus()} //Ocultar el teclado al buscar
                    ),
                    shape = RoundedCornerShape(24.dp)
                )

                // --- Cuerpo: si hay búsqueda → lista global simple; si no, UI original por categoría ---
                if (debouncedQuery.isNotBlank()) {
                    // Lista global (sin DnD, incluso en Favoritos)
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = innerPadding.calculateBottomPadding() + 16.dp
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(effectiveList, key = { _, tool -> tool.screen.route }) { index, tool ->
                            val isFavorito = orderedRoutes.contains(tool.screen.route)
                            val decor = groupDecorBySubcategory(index, effectiveList)
                            ToolCard(
                                tool = tool,
                                isFavorito = isFavorito,
                                shape = decor.shape,
                                topPadding = decor.topPad,
                                bottomPadding = decor.bottomPad,
                                onToolClick = { handleToolClick(tool) },
                                onToggleFavorito = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    scope.launch {
                                        context.toogleFavorite(tool.screen.route)
                                        actualizarWidgetFavoritos(context)
                                    }
                                }
                            )
                        }
                        if (effectiveList.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                } else {
                    // ========= UI original por categoría =========
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
                                    top = 12.dp,
                                    bottom = innerPadding.calculateBottomPadding() + 4.dp
                                )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.favorites_reorder_info), fontSize = 13.sp)
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(favRoutes, key = { _, route -> route }) { index, route ->
                                    val tool = tools.find { it.screen.route == route } ?: return@itemsIndexed
                                    val toolsForDecor = favRoutes.mapNotNull { r -> tools.find { it.screen.route == r } }
                                    val decorIndex = toolsForDecor.indexOfFirst { it.screen.route == route }
                                    val decor = if (decorIndex >= 0) {
                                        groupDecorBySubcategory(decorIndex, toolsForDecor)
                                    } else {
                                        groupDecorBySubcategory(index, toolsForDecor)
                                    }

                                    ReorderableItem(reorderState, key = route) { isDragging ->
                                        val elev = animateDpAsState(
                                            targetValue = if (isDragging) 10.dp else 6.dp,
                                            label = "drag-elev"
                                        ).value

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
                                                onToolClick = { handleToolClick(tool) },
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
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = innerPadding.calculateBottomPadding() + 16.dp
                            ),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(baseListForCategory, key = { _, tool -> tool.screen.route }) { index, tool ->
                                val isFavorito = orderedRoutes.contains(tool.screen.route)
                                val decor = groupDecorBySubcategory(index, baseListForCategory)

                                ToolCard(
                                    tool = tool,
                                    isFavorito = isFavorito,
                                    shape = decor.shape,
                                    topPadding = decor.topPad,
                                    bottomPadding = decor.bottomPad,
                                    onToolClick = { handleToolClick(tool) },
                                    onToggleFavorito = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        scope.launch {
                                            context.toogleFavorite(tool.screen.route)
                                            actualizarWidgetFavoritos(context)
                                        }
                                    }
                                )
                            }

                            if (baseListForCategory.isEmpty()) {
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
    }
    if (showPaywallDialog) {
        ProToolPaywallDialog(
            onDismiss = { showPaywallDialog = false },
            onGoToPro = {
                showPaywallDialog = false
                onNavigateToPro()
            },
            onWatchAd = {
                showPaywallDialog = false
                if (activity != null) {
                    RewardedManager.show(
                        activity = activity,
                        onReward = {
                            // Activa el pase de 10 minutos
                            CreditAccessManager.startTimedPassForAd(activity)

                            Toast
                                .makeText(activity, R.string.pro_unlocked_toast, android.widget.Toast.LENGTH_SHORT)
                                .show()

                            selectedProTool?.let { onToolClick(it) }
                        },
                        onUnavailable = {
                            // No-fill: deja pasar, muestra Toast y no suma tiempo
                            val used = CreditAccessManager.consumeGrace(activity)
                            if (used) {
                                Toast
                                    .makeText(activity, R.string.free_pass_used_toast, android.widget.Toast.LENGTH_SHORT)
                                    .show()
                                selectedProTool?.let { onToolClick(it) }
                            } else {
                                android.widget.Toast
                                    .makeText(activity, R.string.paywall_no_ad_try_later, android.widget.Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    )
                }
            }
        )
    }
}

/* ---------- Utils ---------- */
private fun <T> MutableList<T>.move(from: Int, to: Int) {
    if (from == to) return
    val element = removeAt(from)
    add(to, element)
}
