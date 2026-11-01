package com.joasasso.minitoolbox

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joasasso.minitoolbox.tools.Tool
import com.joasasso.minitoolbox.tools.ToolCategory
import com.joasasso.minitoolbox.ui.components.CatalogEmptyState
import com.joasasso.minitoolbox.ui.components.CategoryGlyph
import com.joasasso.minitoolbox.ui.components.EditFavoritesSheet
import com.joasasso.minitoolbox.ui.components.FavoriteTile
import com.joasasso.minitoolbox.ui.components.ProPassBadge
import com.joasasso.minitoolbox.ui.components.ProToolPaywallDialog
import com.joasasso.minitoolbox.ui.components.ToolSearchResult
import com.joasasso.minitoolbox.ui.components.ToolTile
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
import kotlinx.coroutines.launch
import java.util.Locale

/** Ancho máximo del contenido en ventanas grandes (MD3: 840–1040dp). */
private val MaxContentWidth = 1040.dp

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
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()

    val windowWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    val horizontalMargin: Dp = if (windowWidth >= 600.dp) 24.dp else 16.dp

    val proState = LocalProState.current
    var showPaywallDialog by remember { mutableStateOf(false) }
    var selectedProTool by remember { mutableStateOf<Tool?>(null) }
    var showEditFavorites by rememberSaveable { mutableStateOf(false) }

    val handleToolClick: (Tool) -> Unit = { tool ->
        if (tool.isPro && !proState.isPro && !CreditAccessManager.hasActivePass(context)) {
            selectedProTool = tool
            showPaywallDialog = true
        } else {
            onToolClick(tool)
        }
    }

    val catalogTools = remember(tools) { tools.filter(::isCatalogTool) }

    // Búsqueda: el campo de texto es la fuente y el ViewModel guarda la consulta.
    // Al colapsar la barra se limpia, para que la próxima búsqueda arranque de cero.
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState(initialText = uiState.searchQuery)
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }.collectLatest(vm::onSearchQueryChange)
    }
    LaunchedEffect(searchBarState) {
        snapshotFlow { searchBarState.currentValue }
            .distinctUntilChanged()
            .collectLatest { if (it == SearchBarValue.Collapsed) textFieldState.clearText() }
    }

    val locale = configuration.locales.get(0) ?: Locale.getDefault()
    val searchIndex = remember(catalogTools, locale, context) {
        buildSearchIndexForAllTools(context, catalogTools)
    }

    var debouncedQuery by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
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

    val onToggleFavorite: (Tool) -> Unit = { tool ->
        val adding = tool.screen.route !in uiState.favoriteRoutes
        haptic.performHapticFeedback(
            if (adding) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
        )
        vm.toggleFavorite(tool.screen.route)
    }

    val searchScrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()

    val inputField: @Composable () -> Unit = {
        CatalogSearchField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onCollapse = { scope.launch { searchBarState.animateToCollapsed() } }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(searchScrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AppBarWithSearch(
                state = searchBarState,
                inputField = inputField,
                scrollBehavior = searchScrollBehavior,
                actions = {
                    ProPassBadge(
                        modifier = Modifier.padding(start = 4.dp),
                        onClick = onNavigateToPro
                    )
                    if (!proState.isPro) {
                        IconButton(onClick = onNavigateToPro) {
                            Icon(
                                imageVector = Icons.Filled.WorkspacePremium,
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
            )
            ExpandedFullScreenSearchBar(
                state = searchBarState,
                inputField = inputField
            ) {
                SearchResultsContent(
                    query = debouncedQuery,
                    results = searchResults,
                    catalogTools = catalogTools,
                    onResultClick = { tool ->
                        scope.launch { searchBarState.animateToCollapsed() }
                        handleToolClick(tool)
                    },
                    onClearQuery = { textFieldState.clearText() }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            CatalogGrid(
                catalogTools = catalogTools,
                favoriteRoutes = uiState.favoriteRoutes,
                isLoading = uiState.isLoading,
                contentPadding = PaddingValues(
                    start = horizontalMargin,
                    end = horizontalMargin,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp
                ),
                horizontalMargin = horizontalMargin,
                onToolClick = handleToolClick,
                onToggleFavorite = onToggleFavorite,
                onEditFavorites = { showEditFavorites = true }
            )
            // Con la barra de búsqueda oculta por el scroll, la grilla pasaría debajo de la
            // barra de estado: esta franja la protege.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(MaterialTheme.colorScheme.surface)
            )
        }
    }

    if (showEditFavorites) {
        EditFavoritesSheet(
            favoriteRoutes = uiState.favoriteRoutes,
            catalogTools = catalogTools,
            onCommitOrder = vm::updateFavoritesOrder,
            onRemove = { route ->
                haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                vm.toggleFavorite(route)
            },
            onDismiss = { showEditFavorites = false }
        )
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
private fun CatalogSearchField(
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    onCollapse: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val isExpanded = searchBarState.currentValue == SearchBarValue.Expanded

    SearchBarDefaults.InputField(
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        onSearch = { keyboardController?.hide() },
        placeholder = { Text(stringResource(R.string.categories_search_hint)) },
        leadingIcon = {
            if (isExpanded) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.close)
                    )
                }
            } else {
                Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
            }
        },
        trailingIcon = {
            if (isExpanded && textFieldState.text.isNotEmpty()) {
                IconButton(onClick = { textFieldState.clearText() }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.categories_search_clear_cd)
                    )
                }
            }
        }
    )
}

@Composable
private fun CatalogGrid(
    catalogTools: List<Tool>,
    favoriteRoutes: List<String>,
    isLoading: Boolean,
    contentPadding: PaddingValues,
    horizontalMargin: Dp,
    onToolClick: (Tool) -> Unit,
    onToggleFavorite: (Tool) -> Unit,
    onEditFavorites: () -> Unit
) {
    val gridState = rememberLazyGridState()
    val isFavorite: (Tool) -> Boolean = { it.screen.route in favoriteRoutes }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = MaxContentWidth),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // La sección está desde el primer frame, vacía mientras carga DataStore. Si se insertara
        // al terminar la carga, la grilla mantendría anclado el primer ítem visible y los
        // favoritos quedarían arriba, fuera de pantalla (arranque en frío).
        item(key = "favorites_section", span = { GridItemSpan(maxLineSpan) }) {
            if (!isLoading) {
                FavoritesSection(
                    favoriteRoutes = favoriteRoutes,
                    catalogTools = catalogTools,
                    horizontalMargin = horizontalMargin,
                    onToolClick = onToolClick,
                    onEditFavorites = onEditFavorites
                )
            }
        }

        ToolCategory.all.forEach { category ->
            val categoryTools = catalogTools.filter { it.category == category }
            if (categoryTools.isEmpty()) return@forEach
            item(
                key = "header_category_${category.titleRes}",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                CategorySectionHeader(category = category)
            }
            toolTiles(categoryTools, isFavorite, onToolClick, onToggleFavorite)
        }
    }
}

private fun LazyGridScope.toolTiles(
    tools: List<Tool>,
    isFavorite: (Tool) -> Boolean,
    onToolClick: (Tool) -> Unit,
    onToggleFavorite: (Tool) -> Unit
) {
    items(items = tools, key = { "tool_${it.screen.route}" }) { tool ->
        ToolTile(
            tool = tool,
            isFavorite = isFavorite(tool),
            onClick = { onToolClick(tool) },
            onToggleFavorite = { onToggleFavorite(tool) },
            modifier = Modifier.animateItem()
        )
    }
}

@Composable
private fun FavoritesSection(
    favoriteRoutes: List<String>,
    catalogTools: List<Tool>,
    horizontalMargin: Dp,
    onToolClick: (Tool) -> Unit,
    onEditFavorites: () -> Unit
) {
    val favoriteTools = remember(favoriteRoutes, catalogTools) {
        val toolsByRoute = catalogTools.associateBy { it.screen.route }
        favoriteRoutes.mapNotNull { toolsByRoute[it] }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle(
                title = stringResource(R.string.catalog_favorites_title),
                modifier = Modifier.weight(1f)
            )
            if (favoriteTools.isNotEmpty()) {
                TextButton(onClick = onEditFavorites) {
                    Text(stringResource(R.string.catalog_favorites_edit))
                }
            }
        }

        if (favoriteTools.isEmpty()) {
            CatalogEmptyState(
                icon = Icons.Outlined.StarOutline,
                title = stringResource(R.string.categories_empty_favorites_title),
                subtitle = stringResource(R.string.categories_empty_favorites_subtitle),
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            LazyRow(
                modifier = Modifier
                    .bleedHorizontally(horizontalMargin)
                    .padding(top = 4.dp),
                contentPadding = PaddingValues(horizontal = horizontalMargin - 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items = favoriteTools, key = { "favorite_${it.screen.route}" }) { tool ->
                    FavoriteTile(
                        tool = tool,
                        onClick = { onToolClick(tool) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .padding(top = 8.dp, bottom = 4.dp)
            .semantics { heading() }
    )
}

@Composable
private fun CategorySectionHeader(
    category: ToolCategory,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CategoryGlyph(category = category, size = 36.dp)
        Text(
            text = stringResource(category.titleRes),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
    }
}

@Composable
private fun SearchResultsContent(
    query: String,
    results: List<Tool>,
    catalogTools: List<Tool>,
    onResultClick: (Tool) -> Unit,
    onClearQuery: () -> Unit
) {
    val displayed = if (query.isBlank()) catalogTools else results

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (query.isBlank()) {
            item(key = "search_suggestions_title") {
                Text(
                    text = stringResource(R.string.catalog_search_suggestions),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 4.dp, bottom = 8.dp)
                        .semantics { heading() }
                )
            }
        }

        if (displayed.isEmpty()) {
            item(key = "search_empty_state") {
                CatalogEmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = stringResource(R.string.no_tools_found),
                    subtitle = stringResource(R.string.categories_empty_search_subtitle),
                    actionLabel = stringResource(R.string.categories_clear_filters),
                    onAction = onClearQuery
                )
            }
        } else {
            itemsIndexed(
                items = displayed,
                key = { _, tool -> "search_${tool.screen.route}" }
            ) { index, tool ->
                ToolSearchResult(
                    tool = tool,
                    index = index,
                    count = displayed.size,
                    onClick = { onResultClick(tool) }
                )
            }
        }
    }
}

/**
 * Extiende el elemento sobre los márgenes laterales del contenedor, para que una fila
 * horizontal llegue al borde de la pantalla mientras su contenido queda alineado a la grilla.
 */
private fun Modifier.bleedHorizontally(margin: Dp): Modifier = layout { measurable, constraints ->
    val extra = (margin * 2).roundToPx()
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.minWidth + extra,
            maxWidth = constraints.maxWidth + extra
        )
    )
    layout(placeable.width - extra, placeable.height) {
        placeable.place(-extra / 2, 0)
    }
}
