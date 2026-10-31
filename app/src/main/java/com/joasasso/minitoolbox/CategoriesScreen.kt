package com.joasasso.minitoolbox

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joasasso.minitoolbox.tools.Tool
import com.joasasso.minitoolbox.tools.ToolCategory
import com.joasasso.minitoolbox.ui.components.ProPassBadge
import com.joasasso.minitoolbox.ui.components.ProToolPaywallDialog
import com.joasasso.minitoolbox.ui.components.ToolCard
import com.joasasso.minitoolbox.ui.components.groupDecorBySubcategory
import com.joasasso.minitoolbox.ui.theme.swatchForCategory
import com.joasasso.minitoolbox.utils.ads.RewardedManager
import com.joasasso.minitoolbox.utils.buildSearchIndexForAllTools
import com.joasasso.minitoolbox.utils.isCatalogTool
import com.joasasso.minitoolbox.utils.matchesQuery
import com.joasasso.minitoolbox.utils.pro.CreditAccessManager
import com.joasasso.minitoolbox.utils.pro.LocalProState
import com.joasasso.minitoolbox.viewmodel.CategoryViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
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
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val activity = context as? Activity
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val screenWidthDp = configuration.screenWidthDp
    val useNavigationRail = screenWidthDp >= 600
    val horizontalMargin: Dp = if (screenWidthDp >= 600) 24.dp else 16.dp

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

    val catalogTools = remember(tools) { tools.filter(::isCatalogTool) }

    val categoryTools = remember(uiState.selectedCategory, catalogTools) {
        catalogTools.filter { it.category == uiState.selectedCategory }
    }

    val locale = configuration.locales.get(0) ?: Locale.getDefault()
    val searchIndex = remember(catalogTools, locale, context) {
        buildSearchIndexForAllTools(context, catalogTools)
    }

    var debouncedQuery by remember { mutableStateOf("") }
    LaunchedEffect(uiState.searchQuery) {
        snapshotFlow { uiState.searchQuery.trim() }
            .distinctUntilChanged()
            .debounce(80)
            .collectLatest { debouncedQuery = it }
    }

    val searchResults: List<Tool> = remember(debouncedQuery, searchIndex, locale, catalogTools) {
        if (debouncedQuery.isBlank()) {
            emptyList()
        } else {
            val matchingRoutes = searchIndex.asSequence()
                .filter { matchesQuery(it, debouncedQuery, locale) }
                .map { it.route }
                .toSet()
            catalogTools.filter { it.screen.route in matchingRoutes }
        }
    }

    val listStateKey = remember(uiState.selectedCategory) {
        "cat_${uiState.selectedCategory.titleRes}"
    }
    val listState = rememberSaveable(listStateKey, saver = LazyListState.Saver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }
    val searchListState = rememberSaveable("search_list", saver = LazyListState.Saver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }

    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0

    BackHandler(enabled = isKeyboardOpen) {
        focusManager.clearFocus()
    }

    BackHandler(enabled = !isKeyboardOpen && uiState.isSearchActive) {
        vm.closeSearch()
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CatalogTopAppBar(
                selectedCategory = uiState.selectedCategory,
                isSearchActive = uiState.isSearchActive,
                searchQuery = uiState.searchQuery,
                isPro = proState.isPro,
                scrollBehavior = scrollBehavior,
                onOpenSearch = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.openSearch()
                },
                onCloseSearch = {
                    focusManager.clearFocus()
                    vm.closeSearch()
                },
                onQueryChange = vm::onSearchQueryChange,
                onClearQuery = vm::clearSearch,
                onSearchSubmit = { focusManager.clearFocus() },
                onNavigateToPro = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigateToPro()
                },
                onNavigateToAbout = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigateToAbout()
                }
            )
        },
        bottomBar = {
            if (!useNavigationRail && !uiState.isSearchActive) {
                CategoryBottomBar(
                    selectedCategory = uiState.selectedCategory,
                    onSelectCategory = { category ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.selectCategory(category)
                    }
                )
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (useNavigationRail && !uiState.isSearchActive) {
                CategorySideRail(
                    selectedCategory = uiState.selectedCategory,
                    onSelectCategory = { category ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.selectCategory(category)
                    }
                )
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 760.dp)
                ) {
                    if (uiState.isSearchActive) {
                        SearchResultsList(
                            query = debouncedQuery,
                            results = searchResults,
                            catalogTools = catalogTools,
                            favoriteRoutes = uiState.favoriteRoutes,
                            listState = searchListState,
                            horizontalMargin = horizontalMargin,
                            onToolClick = handleToolClick,
                            onToggleFavorite = { tool ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.toggleFavorite(tool.screen.route)
                            },
                            onClearSearch = vm::clearSearch
                        )
                    } else if (uiState.selectedCategory == ToolCategory.Favoritos) {
                        ReorderableFavoritesContent(
                            favoriteRoutes = uiState.favoriteRoutes,
                            catalogTools = catalogTools,
                            listState = listState,
                            horizontalMargin = horizontalMargin,
                            onToolClick = handleToolClick,
                            onToggleFavorite = { route ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.toggleFavorite(route)
                            },
                            onCommitOrder = vm::updateFavoritesOrder
                        )
                    } else {
                        SingleCategoryList(
                            categoryTools = categoryTools,
                            favoriteRoutes = uiState.favoriteRoutes,
                            listState = listState,
                            horizontalMargin = horizontalMargin,
                            onToolClick = handleToolClick,
                            onToggleFavorite = { tool ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.toggleFavorite(tool.screen.route)
                            }
                        )
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
                            CreditAccessManager.startTimedPassForAd(activity)
                            Toast
                                .makeText(activity, R.string.pro_unlocked_toast, Toast.LENGTH_SHORT)
                                .show()
                            selectedProTool?.let { onToolClick(it) }
                        },
                        onUnavailable = {
                            val used = CreditAccessManager.consumeGrace(activity)
                            if (used) {
                                Toast
                                    .makeText(activity, R.string.free_pass_used_toast, Toast.LENGTH_SHORT)
                                    .show()
                                selectedProTool?.let { onToolClick(it) }
                            } else {
                                Toast
                                    .makeText(activity, R.string.paywall_no_ad_try_later, Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogTopAppBar(
    selectedCategory: ToolCategory,
    isSearchActive: Boolean,
    searchQuery: String,
    isPro: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSearchSubmit: () -> Unit,
    onNavigateToPro: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    TopAppBar(
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        navigationIcon = {
            if (isSearchActive) {
                IconButton(onClick = onCloseSearch) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.close)
                    )
                }
            }
        },
        title = {
            if (isSearchActive) {
                TextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.categories_search_hint),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { onSearchSubmit() }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
            } else {
                Text(
                    text = stringResource(selectedCategory.titleRes),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        actions = {
            if (isSearchActive) {
                AnimatedVisibility(
                    visible = searchQuery.isNotBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(onClick = onClearQuery) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.categories_search_clear_cd)
                        )
                    }
                }
            } else {
                IconButton(onClick = onOpenSearch) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = stringResource(R.string.categories_search_hint)
                    )
                }
                ProPassBadge(
                    modifier = Modifier.padding(end = 2.dp),
                    onClick = onNavigateToPro
                )
                if (!isPro) {
                    IconButton(onClick = onNavigateToPro) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = stringResource(R.string.pro_screen_title),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                IconButton(onClick = onNavigateToAbout) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(R.string.information)
                    )
                }
            }
        }
    )
}

@Composable
private fun CategoryBottomBar(
    selectedCategory: ToolCategory,
    onSelectCategory: (ToolCategory) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        ToolCategory.all.forEach { category ->
            val isSelected = category == selectedCategory
            val swatch = swatchForCategory(category)
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectCategory(category) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) category.filledIcon else category.outlinedIcon,
                        contentDescription = stringResource(category.titleRes)
                    )
                },
                label = {
                    Text(
                        text = stringResource(category.titleRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = swatch.onContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = swatch.container,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun CategorySideRail(
    selectedCategory: ToolCategory,
    onSelectCategory: (ToolCategory) -> Unit
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Spacer(Modifier.height(8.dp))
        ToolCategory.all.forEach { category ->
            val isSelected = category == selectedCategory
            val swatch = swatchForCategory(category)
            NavigationRailItem(
                selected = isSelected,
                onClick = { onSelectCategory(category) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) category.filledIcon else category.outlinedIcon,
                        contentDescription = stringResource(category.titleRes)
                    )
                },
                label = {
                    Text(
                        text = stringResource(category.titleRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = swatch.onContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = swatch.container,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun SingleCategoryList(
    categoryTools: List<Tool>,
    favoriteRoutes: List<String>,
    listState: LazyListState,
    horizontalMargin: Dp,
    onToolClick: (Tool) -> Unit,
    onToggleFavorite: (Tool) -> Unit
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = horizontalMargin,
            end = horizontalMargin,
            top = 6.dp,
            bottom = 24.dp
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(
            items = categoryTools,
            key = { _, tool -> "cat_item_${tool.screen.route}" }
        ) { index, tool ->
            val decor = groupDecorBySubcategory(index, categoryTools)
            ToolCard(
                tool = tool,
                isFavorito = favoriteRoutes.contains(tool.screen.route),
                shape = decor.shape,
                topPadding = decor.topPad,
                bottomPadding = decor.bottomPad,
                onToolClick = onToolClick,
                onToggleFavorito = { onToggleFavorite(tool) }
            )
        }
    }
}

@Composable
private fun SearchResultsList(
    query: String,
    results: List<Tool>,
    catalogTools: List<Tool>,
    favoriteRoutes: List<String>,
    listState: LazyListState,
    horizontalMargin: Dp,
    onToolClick: (Tool) -> Unit,
    onToggleFavorite: (Tool) -> Unit,
    onClearSearch: () -> Unit
) {
    val displayedTools = if (query.isBlank()) catalogTools else results

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = horizontalMargin,
            end = horizontalMargin,
            top = 6.dp,
            bottom = 24.dp
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        if (displayedTools.isEmpty()) {
            item(key = "search_empty_state") {
                EmptyCatalogStateCard(
                    icon = Icons.Outlined.SearchOff,
                    title = stringResource(R.string.no_tools_found),
                    subtitle = stringResource(R.string.categories_empty_search_subtitle),
                    actionLabel = stringResource(R.string.categories_clear_filters),
                    onAction = onClearSearch
                )
            }
        } else {
            itemsIndexed(
                items = displayedTools,
                key = { _, tool -> "search_item_${tool.screen.route}" }
            ) { index, tool ->
                val decor = groupDecorBySubcategory(index, displayedTools)
                ToolCard(
                    tool = tool,
                    isFavorito = favoriteRoutes.contains(tool.screen.route),
                    shape = decor.shape,
                    topPadding = decor.topPad,
                    bottomPadding = decor.bottomPad,
                    onToolClick = onToolClick,
                    onToggleFavorito = { onToggleFavorite(tool) }
                )
            }
        }
    }
}

@Composable
private fun ReorderableFavoritesContent(
    favoriteRoutes: List<String>,
    catalogTools: List<Tool>,
    listState: LazyListState,
    horizontalMargin: Dp,
    onToolClick: (Tool) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onCommitOrder: (List<String>) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val favRoutes = remember(favoriteRoutes) { favoriteRoutes.toMutableStateList() }
    val reorderState = rememberReorderableLazyListState(
        lazyListState = listState
    ) { from, to ->
        val fromIdx = from.index.coerceIn(0, (favRoutes.size - 1).coerceAtLeast(0))
        val toIdx = to.index.coerceIn(0, (favRoutes.size - 1).coerceAtLeast(0))
        if (fromIdx in favRoutes.indices && toIdx in favRoutes.indices) {
            favRoutes.move(fromIdx, toIdx)
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = horizontalMargin,
            end = horizontalMargin,
            top = 6.dp,
            bottom = 24.dp
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        if (favRoutes.isEmpty()) {
            item(key = "fav_empty_state") {
                EmptyCatalogStateCard(
                    icon = Icons.Outlined.StarOutline,
                    title = stringResource(R.string.categories_empty_favorites_title),
                    subtitle = stringResource(R.string.categories_empty_favorites_subtitle)
                )
            }
        } else {
            itemsIndexed(
                items = favRoutes,
                key = { _, route -> route }
            ) { index, route ->
                val tool = catalogTools.find { it.screen.route == route } ?: return@itemsIndexed
                val toolsForDecor = favRoutes.mapNotNull { r ->
                    catalogTools.find { it.screen.route == r }
                }
                val decorIndex = toolsForDecor.indexOfFirst { it.screen.route == route }
                val decor = if (decorIndex >= 0) {
                    groupDecorBySubcategory(decorIndex, toolsForDecor)
                } else {
                    groupDecorBySubcategory(index, toolsForDecor)
                }

                ReorderableItem(reorderState, key = route) { isDragging ->
                    val elev by animateDpAsState(
                        targetValue = if (isDragging) 6.dp else 0.dp,
                        label = "fav-drag-elev"
                    )

                    val cardLongPressDragMod = with(this) {
                        Modifier.longPressDraggableHandle(
                            onDragStarted = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragStopped = {
                                onCommitOrder(favRoutes.toList())
                            }
                        )
                    }

                    val handleImmediateDragMod = with(this) {
                        Modifier.draggableHandle(
                            onDragStarted = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragStopped = {
                                onCommitOrder(favRoutes.toList())
                            }
                        )
                    }

                    Box(modifier = cardLongPressDragMod) {
                        ToolCard(
                            tool = tool,
                            isFavorito = true,
                            shape = decor.shape,
                            topPadding = decor.topPad,
                            bottomPadding = decor.bottomPad,
                            onToolClick = onToolClick,
                            onToggleFavorito = {
                                favRoutes.remove(tool.screen.route)
                                onToggleFavorite(tool.screen.route)
                            },
                            overrideElevation = elev,
                            isDragging = isDragging,
                            showDragHandle = true,
                            dragHandleModifier = handleImmediateDragMod
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCatalogStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(16.dp))
                FilledTonalButton(onClick = onAction) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}

private fun <T> MutableList<T>.move(from: Int, to: Int) {
    if (from == to) return
    val element = removeAt(from)
    add(to, element)
}
