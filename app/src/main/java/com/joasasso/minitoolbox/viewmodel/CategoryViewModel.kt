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

data class CategoriesUiState(
    /** Filtro del catálogo; `null` muestra todas las categorías agrupadas. */
    val selectedCategory: ToolCategory? = null,
    val searchQuery: String = "",
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

    private val _selectedCategory = MutableStateFlow<ToolCategory?>(null)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<CategoriesUiState> = combine(
        _selectedCategory,
        _searchQuery,
        repository.favoritesFlow
    ) { category, query, favorites ->
        CategoriesUiState(
            selectedCategory = category,
            searchQuery = query,
            favoriteRoutes = favorites,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoriesUiState(isLoading = true)
    )

    /** Tocar el filtro activo lo desactiva y vuelve a "Todas". */
    fun selectCategory(category: ToolCategory?) {
        _selectedCategory.value = if (category == _selectedCategory.value) null else category
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
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
