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
    val selectedCategory: ToolCategory = ToolCategory.Instrumentos,
    val isSearchActive: Boolean = false,
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

    private val _selectedCategory = MutableStateFlow<ToolCategory>(ToolCategory.Instrumentos)
    private val _isSearchActive = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<CategoriesUiState> = combine(
        _selectedCategory,
        _isSearchActive,
        _searchQuery,
        repository.favoritesFlow
    ) { category, searchActive, query, favorites ->
        CategoriesUiState(
            selectedCategory = category,
            isSearchActive = searchActive,
            searchQuery = query,
            favoriteRoutes = favorites,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoriesUiState(isLoading = true)
    )

    fun selectCategory(category: ToolCategory) {
        _selectedCategory.value = category
        _isSearchActive.value = false
        _searchQuery.value = ""
    }

    fun openSearch() {
        _isSearchActive.value = true
    }

    fun closeSearch() {
        _isSearchActive.value = false
        _searchQuery.value = ""
    }

    fun onSearchQueryChange(query: String) {
        _isSearchActive.value = true
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
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
