package com.joasasso.minitoolbox.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joasasso.minitoolbox.data.DefaultFavoritesRepository
import com.joasasso.minitoolbox.data.FavoritesRepository
import com.joasasso.minitoolbox.tools.ToolCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Filtro activo en la pantalla principal del catálogo.
 */
sealed interface CategoryFilter {
    data object All : CategoryFilter
    data object Favorites : CategoryFilter
    data class ByCategory(val category: ToolCategory) : CategoryFilter
}

/**
 * Modo de visualización del catálogo en pantallas compactas.
 */
enum class CatalogLayoutMode {
    LIST,
    GRID
}

data class CategoriesUiState(
    val selectedFilter: CategoryFilter = CategoryFilter.All,
    val searchQuery: String = "",
    val layoutMode: CatalogLayoutMode = CatalogLayoutMode.LIST,
    val favoriteRoutes: List<String> = emptyList(),
    val isLoading: Boolean = false
)

class CategoryViewModel(
    application: Application,
    private val repository: FavoritesRepository
) : AndroidViewModel(application) {

    @Suppress("unused")
    constructor(application: Application) : this(
        application = application,
        repository = DefaultFavoritesRepository(application)
    )

    private val _selectedFilter = MutableStateFlow<CategoryFilter>(CategoryFilter.All)
    private val _searchQuery = MutableStateFlow("")
    private val _layoutMode = MutableStateFlow(CatalogLayoutMode.LIST)

    val uiState: StateFlow<CategoriesUiState> = combine(
        _selectedFilter,
        _searchQuery,
        _layoutMode,
        repository.favoritesFlow
    ) { filter, query, layout, favorites ->
        CategoriesUiState(
            selectedFilter = filter,
            searchQuery = query,
            layoutMode = layout,
            favoriteRoutes = favorites,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoriesUiState(isLoading = true)
    )

    fun selectFilter(filter: CategoryFilter) {
        _selectedFilter.value = filter
        _searchQuery.value = ""
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun toggleLayoutMode() {
        _layoutMode.value = when (_layoutMode.value) {
            CatalogLayoutMode.LIST -> CatalogLayoutMode.GRID
            CatalogLayoutMode.GRID -> CatalogLayoutMode.LIST
        }
    }

    fun toggleFavorite(route: String) {
        viewModelScope.launch {
            repository.toggleFavorite(route)
        }
    }

    fun updateFavoritesOrder(newOrder: List<String>) {
        viewModelScope.launch {
            repository.setFavoritesOrder(newOrder)
        }
    }
}
