package com.joasasso.minitoolbox.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joasasso.minitoolbox.data.DefaultFavoritesRepository
import com.joasasso.minitoolbox.data.FavoritesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoriesUiState(
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

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<CategoriesUiState> = combine(
        _searchQuery,
        repository.favoritesFlow
    ) { query, favorites ->
        CategoriesUiState(
            searchQuery = query,
            favoriteRoutes = favorites,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoriesUiState(isLoading = true)
    )

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
