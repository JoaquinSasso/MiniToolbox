package com.joasasso.minitoolbox

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.joasasso.minitoolbox.ui.components.FavoriteQuickCard
import com.joasasso.minitoolbox.ui.components.ProPassBadge
import com.joasasso.minitoolbox.ui.components.ProToolPaywallDialog
import com.joasasso.minitoolbox.ui.components.ToolBentoCard
import com.joasasso.minitoolbox.ui.components.ToolCard
import com.joasasso.minitoolbox.ui.components.groupDecorBySubcategory
import com.joasasso.minitoolbox.ui.theme.swatchForCategory
import com.joasasso.minitoolbox.utils.ads.RewardedManager
import com.joasasso.minitoolbox.utils.buildSearchIndexForAllTools
import com.joasasso.minitoolbox.utils.isCatalogTool
import com.joasasso.minitoolbox.utils.matchesQuery
import com.joasasso.minitoolbox.utils.pro.CreditAccessManager
import com.joasasso.minitoolbox.utils.pro.LocalProState
import com.joasasso.minitoolbox.viewmodel.CatalogLayoutMode
import com.joasasso.minitoolbox.viewmodel.CategoryFilter
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
    val horizontalMargin: Dp = if (screenWidthDp >= 600) 24.dp else 16.dp
    val gridColumns: Int = when {
        screenWidthDp >= 840 -> 3
        screenWidthDp >= 600 -> 2
        uiState.layoutMode == CatalogLayoutMode.GRID -> 2
        else -> 1
    }

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

    val favoritesTools: List<Tool> = remember(uiState.favoriteRoutes, catalogTools) {
        uiState.favoriteRoutes.mapNotNull { route ->
            catalogTools.find { it.screen.route == route }
        }
    }

    val locale = configuration.locales.get(0) ?: Locale.getDefault()
    val searchIndex = remember(catalogTools, locale, context) {
        buildSearchIndexForAllTools(context, catalogTools)
    }

    var debouncedQuery by remember { mutableStateOf("") }
    LaunchedEffect(uiState.searchQuery) {
        snapshotFlow { uiState.searchQuery.trim() }
            .distinctUntilChanged()
            .debounce(100)
            .collectLatest { debouncedQuery = it }
    }

    LaunchedEffect(Unit) {
        vm.clearSearch()
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

    val visibleCategories: List<ToolCategory> = remember(uiState.selectedFilter) {
        when (val filter = uiState.selectedFilter) {
            CategoryFilter.All -> ToolCategory.domainCategories
            CategoryFilter.Favorites -> emptyList()
            is CategoryFilter.ByCategory -> listOf(filter.category)
        }
    }

    val listStateKey = remember(uiState.selectedFilter) { filterKey(uiState.selectedFilter) }
    val listState = rememberSaveable(listStateKey, saver = LazyListState.Saver) {
        LazyListState(firstVisibleItemIndex = 0, firstVisibleItemScrollOffset = 0)
    }

    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0

    BackHandler(enabled = isKeyboardOpen) {
        focusManager.clearFocus()
    }

    BackHandler(enabled = !isKeyboardOpen && uiState.searchQuery.isNotBlank()) {
        vm.clearSearch()
    }

    BackHandler(
        enabled = !isKeyboardOpen &&
            uiState.searchQuery.isBlank() &&
            uiState.selectedFilter != CategoryFilter.All
    ) {
        vm.selectFilter(CategoryFilter.All)
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.categories_app_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Text(
                                text = catalogTools.size.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    ProPassBadge(
                        modifier = Modifier.padding(end = 6.dp),
                        onClick = onNavigateToPro
                    )
                    if (!proState.isPro) {
                        Surface(
                            onClick = onNavigateToPro,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = stringResource(R.string.pro_screen_title),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.categories_pro_badge),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateToAbout()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.information)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1040.dp)
            ) {
                // --- Barra de búsqueda MD3 Pill + Toggle de vista ---
                SearchAndViewModeHeader(
                    query = uiState.searchQuery,
                    onQueryChange = vm::onSearchQueryChange,
                    onClearQuery = vm::clearSearch,
                    onSearchSubmit = { focusManager.clearFocus() },
                    layoutMode = uiState.layoutMode,
                    onToggleLayoutMode = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.toggleLayoutMode()
                    },
                    showLayoutToggle = screenWidthDp < 600 && uiState.selectedFilter != CategoryFilter.Favorites,
                    horizontalMargin = horizontalMargin
                )

                // --- Fila horizontal de FilterChips MD3 ---
                CategoryFilterRow(
                    selectedFilter = uiState.selectedFilter,
                    favoritesCount = favoritesTools.size,
                    onSelectFilter = { filter ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.selectFilter(filter)
                    },
                    horizontalMargin = horizontalMargin
                )

                // --- Contenido principal ---
                if (debouncedQuery.isNotBlank()) {
                    SearchResultsContent(
                        results = searchResults,
                        favoriteRoutes = uiState.favoriteRoutes,
                        gridColumns = gridColumns,
                        listState = listState,
                        horizontalMargin = horizontalMargin,
                        bottomPadding = innerPadding.calculateBottomPadding() + 24.dp,
                        onToolClick = handleToolClick,
                        onToggleFavorite = { tool ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.toggleFavorite(tool.screen.route)
                        },
                        onClearSearch = vm::clearSearch
                    )
                } else if (uiState.selectedFilter == CategoryFilter.Favorites) {
                    ReorderableFavoritesContent(
                        favoriteRoutes = uiState.favoriteRoutes,
                        catalogTools = catalogTools,
                        listState = listState,
                        horizontalMargin = horizontalMargin,
                        bottomPadding = innerPadding.calculateBottomPadding() + 24.dp,
                        onToolClick = handleToolClick,
                        onToggleFavorite = { route ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.toggleFavorite(route)
                        },
                        onCommitOrder = vm::updateFavoritesOrder
                    )
                } else {
                    CategorizedCatalogContent(
                        visibleCategories = visibleCategories,
                        catalogTools = catalogTools,
                        favoritesTools = favoritesTools,
                        favoriteRoutes = uiState.favoriteRoutes,
                        showQuickFavoritesShelf = uiState.selectedFilter == CategoryFilter.All && favoritesTools.isNotEmpty(),
                        gridColumns = gridColumns,
                        listState = listState,
                        horizontalMargin = horizontalMargin,
                        bottomPadding = innerPadding.calculateBottomPadding() + 24.dp,
                        onToolClick = handleToolClick,
                        onToggleFavorite = { tool ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.toggleFavorite(tool.screen.route)
                        },
                        onOpenFavoritesReorder = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.selectFilter(CategoryFilter.Favorites)
                        }
                    )
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

@Composable
private fun SearchAndViewModeHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSearchSubmit: () -> Unit,
    layoutMode: CatalogLayoutMode,
    onToggleLayoutMode: () -> Unit,
    showLayoutToggle: Boolean,
    horizontalMargin: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalMargin, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    text = stringResource(R.string.categories_search_hint),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = query.isNotBlank(),
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
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = { onSearchSubmit() }
            ),
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )

        if (showLayoutToggle) {
            val isGrid = layoutMode == CatalogLayoutMode.GRID
            Surface(
                onClick = onToggleLayoutMode,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isGrid) Icons.Outlined.ViewAgenda else Icons.Outlined.GridView,
                        contentDescription = stringResource(
                            if (isGrid) R.string.categories_view_list_cd
                            else R.string.categories_view_grid_cd
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selectedFilter: CategoryFilter,
    favoritesCount: Int,
    onSelectFilter: (CategoryFilter) -> Unit,
    horizontalMargin: Dp
) {
    val filters = remember {
        buildList {
            add(CategoryFilter.All)
            add(CategoryFilter.Favorites)
            ToolCategory.domainCategories.forEach { add(CategoryFilter.ByCategory(it)) }
        }
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = horizontalMargin, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(
            items = filters,
            key = { filter -> "filter_chip_${filterKey(filter)}" }
        ) { filter ->
            val isSelected = filter == selectedFilter
            when (filter) {
                CategoryFilter.All -> {
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectFilter(filter) },
                        label = {
                            Text(
                                text = stringResource(R.string.category_all),
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isSelected) Icons.Filled.Apps else Icons.Outlined.Apps,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        shape = CircleShape
                    )
                }

                CategoryFilter.Favorites -> {
                    val favSwatch = swatchForCategory(ToolCategory.Favoritos)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectFilter(filter) },
                        label = {
                            val baseLabel = stringResource(R.string.category_favoritos)
                            val labelText = if (favoritesCount > 0) "$baseLabel ($favoritesCount)" else baseLabel
                            Text(
                                text = labelText,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = favSwatch.container,
                            selectedLabelColor = favSwatch.onContainer,
                            selectedLeadingIconColor = favSwatch.onContainer
                        )
                    )
                }

                is CategoryFilter.ByCategory -> {
                    val category = filter.category
                    val catSwatch = swatchForCategory(category)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectFilter(filter) },
                        label = {
                            Text(
                                text = stringResource(category.titleRes),
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isSelected) category.filledIcon else category.outlinedIcon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = catSwatch.container,
                            selectedLabelColor = catSwatch.onContainer,
                            selectedLeadingIconColor = catSwatch.onContainer
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CategorizedCatalogContent(
    visibleCategories: List<ToolCategory>,
    catalogTools: List<Tool>,
    favoritesTools: List<Tool>,
    favoriteRoutes: List<String>,
    showQuickFavoritesShelf: Boolean,
    gridColumns: Int,
    listState: LazyListState,
    horizontalMargin: Dp,
    bottomPadding: Dp,
    onToolClick: (Tool) -> Unit,
    onToggleFavorite: (Tool) -> Unit,
    onOpenFavoritesReorder: () -> Unit
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = horizontalMargin,
            end = horizontalMargin,
            top = 8.dp,
            bottom = bottomPadding
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        if (showQuickFavoritesShelf) {
            item(key = "quick_favorites_shelf") {
                QuickFavoritesSection(
                    favoritesTools = favoritesTools,
                    onToolClick = onToolClick,
                    onOpenFavoritesReorder = onOpenFavoritesReorder
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        visibleCategories.forEach { category ->
            val categoryTools = catalogTools.filter { it.category == category }
            if (categoryTools.isNotEmpty()) {
                item(key = "cat_header_${category.titleRes}") {
                    CategorySectionHeader(
                        category = category,
                        toolCount = categoryTools.size
                    )
                }

                if (gridColumns > 1) {
                    val rows = categoryTools.chunked(gridColumns)
                    itemsIndexed(
                        items = rows,
                        key = { rowIndex, rowTools ->
                            "cat_grid_${category.titleRes}_row_${rowIndex}_${rowTools.first().screen.route}"
                        }
                    ) { _, rowTools ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowTools.forEach { tool ->
                                ToolBentoCard(
                                    tool = tool,
                                    isFavorito = favoriteRoutes.contains(tool.screen.route),
                                    onToolClick = onToolClick,
                                    onToggleFavorito = { onToggleFavorite(tool) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(gridColumns - rowTools.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    val groupedBySubcategory = categoryTools.groupBy { it.subCategory }
                    groupedBySubcategory.entries.forEach { (subCatRes, subTools) ->
                        item(key = "subcat_header_${category.titleRes}_$subCatRes") {
                            SubcategoryGroupHeader(subCategoryRes = subCatRes)
                        }
                        itemsIndexed(
                            items = subTools,
                            key = { _, tool -> "cat_list_${category.titleRes}_${tool.screen.route}" }
                        ) { index, tool ->
                            val decor = groupDecorBySubcategory(index, subTools)
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

                item(key = "cat_spacer_${category.titleRes}") {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickFavoritesSection(
    favoritesTools: List<Tool>,
    onToolClick: (Tool) -> Unit,
    onOpenFavoritesReorder: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val favSwatch = swatchForCategory(ToolCategory.Favoritos)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(favSwatch.container),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = favSwatch.onContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.categories_quick_favorites_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            TextButton(onClick = onOpenFavoritesReorder) {
                Text(
                    text = stringResource(R.string.categories_manage_favorites),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                items = favoritesTools,
                key = { tool -> "quick_fav_${tool.screen.route}" }
            ) { tool ->
                FavoriteQuickCard(
                    tool = tool,
                    onToolClick = onToolClick
                )
            }
        }
    }
}

@Composable
private fun CategorySectionHeader(
    category: ToolCategory,
    toolCount: Int
) {
    val swatch = swatchForCategory(category)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(swatch.container),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.filledIcon,
                    contentDescription = null,
                    tint = swatch.onContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = stringResource(category.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Text(
                text = toolCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun SubcategoryGroupHeader(subCategoryRes: Int) {
    Text(
        text = stringResource(subCategoryRes),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun SearchResultsContent(
    results: List<Tool>,
    favoriteRoutes: List<String>,
    gridColumns: Int,
    listState: LazyListState,
    horizontalMargin: Dp,
    bottomPadding: Dp,
    onToolClick: (Tool) -> Unit,
    onToggleFavorite: (Tool) -> Unit,
    onClearSearch: () -> Unit
) {
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = horizontalMargin,
            end = horizontalMargin,
            top = 8.dp,
            bottom = bottomPadding
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        if (results.isEmpty()) {
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
            item(key = "search_results_header") {
                Text(
                    text = stringResource(R.string.categories_search_results_count, results.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                )
            }

            if (gridColumns > 1) {
                val rows = results.chunked(gridColumns)
                itemsIndexed(
                    items = rows,
                    key = { rowIndex, rowTools ->
                        "search_grid_row_${rowIndex}_${rowTools.first().screen.route}"
                    }
                ) { _, rowTools ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowTools.forEach { tool ->
                            ToolBentoCard(
                                tool = tool,
                                isFavorito = favoriteRoutes.contains(tool.screen.route),
                                onToolClick = onToolClick,
                                onToggleFavorito = { onToggleFavorite(tool) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(gridColumns - rowTools.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = results,
                    key = { _, tool -> "search_item_${tool.screen.route}" }
                ) { index, tool ->
                    val decor = groupDecorBySubcategory(index, results)
                    ToolCard(
                        tool = tool,
                        isFavorito = favoriteRoutes.contains(tool.screen.route),
                        shape = decor.shape,
                        topPadding = decor.topPad,
                        bottomPadding = decor.bottomPad,
                        onToolClick = onToolClick,
                        onToggleFavorito = { onToggleFavorite(tool) },
                        showSubcategoryBadge = true
                    )
                }
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
    bottomPadding: Dp,
    onToolClick: (Tool) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onCommitOrder: (List<String>) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val favRoutes = remember(favoriteRoutes) { favoriteRoutes.toMutableStateList() }
    val reorderState = rememberReorderableLazyListState(
        lazyListState = listState
    ) { from, to ->
        // Ajustar por el header item en índice 0
        val fromIdx = (from.index - 1).coerceIn(0, (favRoutes.size - 1).coerceAtLeast(0))
        val toIdx = (to.index - 1).coerceIn(0, (favRoutes.size - 1).coerceAtLeast(0))
        if (fromIdx in favRoutes.indices && toIdx in favRoutes.indices) {
            favRoutes.move(fromIdx, toIdx)
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            start = horizontalMargin,
            end = horizontalMargin,
            top = 8.dp,
            bottom = bottomPadding
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
            item(key = "fav_reorder_header") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragIndicator,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.favorites_reorder_info),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

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

                    val dragMod = with(this) {
                        Modifier.longPressDraggableHandle(
                            onDragStarted = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragStopped = {
                                onCommitOrder(favRoutes.toList())
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
                                favRoutes.remove(tool.screen.route)
                                onToggleFavorite(tool.screen.route)
                            },
                            overrideElevation = elev,
                            isDragging = isDragging,
                            showSubcategoryBadge = true,
                            showDragHandle = true
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

private fun filterKey(filter: CategoryFilter): String = when (filter) {
    CategoryFilter.All -> "all"
    CategoryFilter.Favorites -> "favorites"
    is CategoryFilter.ByCategory -> "cat_${filter.category.titleRes}"
}

private fun <T> MutableList<T>.move(from: Int, to: Int) {
    if (from == to) return
    val element = removeAt(from)
    add(to, element)
}
