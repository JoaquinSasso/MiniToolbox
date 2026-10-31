package com.joasasso.minitoolbox.viewmodel

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.joasasso.minitoolbox.TestApplication
import com.joasasso.minitoolbox.data.DefaultFavoritesRepository
import com.joasasso.minitoolbox.data.favoritesDataStore
import com.joasasso.minitoolbox.nav.Screen
import com.joasasso.minitoolbox.tools.ToolCategory
import com.joasasso.minitoolbox.tools.ToolRegistry
import com.joasasso.minitoolbox.utils.buildSearchIndexForAllTools
import com.joasasso.minitoolbox.utils.isCatalogTool
import com.joasasso.minitoolbox.utils.matchesQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class CategoryViewModelTest {

    private lateinit var app: Application
    private val testDispatcher = StandardTestDispatcher()
    private var vm: CategoryViewModel? = null

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        app.favoritesDataStore.edit { it.clear() }
    }

    @After
    fun tearDown() {
        vm?.viewModelScope?.cancel()
        Dispatchers.resetMain()
    }

    private fun createViewModel(): CategoryViewModel {
        val repo = DefaultFavoritesRepository(app, updateWidgets = false)
        return CategoryViewModel(app, repo).also { vm = it }
    }

    @Test
    fun estadoInicial_emiteFiltroAllYListaFavoritosVacia() = runTest {
        val model = createViewModel()
        val state = model.uiState.first { !it.isLoading }

        assertEquals(CategoryFilter.All, state.selectedFilter)
        assertEquals("", state.searchQuery)
        assertEquals(CatalogLayoutMode.LIST, state.layoutMode)
        assertTrue(state.favoriteRoutes.isEmpty())
    }

    @Test
    fun selectFilter_cambiaFiltroYLimpiaBusquedaActiva() = runTest {
        val model = createViewModel()
        model.uiState.first { !it.isLoading }

        model.onSearchQueryChange("regla")
        assertEquals("regla", model.uiState.first { it.searchQuery == "regla" }.searchQuery)

        val targetFilter = CategoryFilter.ByCategory(ToolCategory.Instrumentos)
        model.selectFilter(targetFilter)

        val updated = model.uiState.first { it.selectedFilter == targetFilter }
        assertEquals(targetFilter, updated.selectedFilter)
        assertEquals("", updated.searchQuery)
    }

    @Test
    fun toggleLayoutMode_alternaEntreListYGrid() = runTest {
        val model = createViewModel()
        model.uiState.first { !it.isLoading }

        model.toggleLayoutMode()
        assertEquals(
            CatalogLayoutMode.GRID,
            model.uiState.first { it.layoutMode == CatalogLayoutMode.GRID }.layoutMode
        )

        model.toggleLayoutMode()
        assertEquals(
            CatalogLayoutMode.LIST,
            model.uiState.first { it.layoutMode == CatalogLayoutMode.LIST }.layoutMode
        )
    }

    @Test
    fun toggleFavorite_agregaYQuitaRutaEnDataStore() = runTest {
        val model = createViewModel()
        model.uiState.first { !it.isLoading }

        model.toggleFavorite(Screen.Compass.route)
        val afterAdd = model.uiState.first { it.favoriteRoutes.contains(Screen.Compass.route) }
        assertEquals(listOf(Screen.Compass.route), afterAdd.favoriteRoutes)

        model.toggleFavorite(Screen.Compass.route)
        val afterRemove = model.uiState.first { it.favoriteRoutes.isEmpty() }
        assertTrue(afterRemove.favoriteRoutes.isEmpty())
    }

    @Test
    fun updateFavoritesOrder_persisteNuevoOrdenExacto() = runTest {
        val model = createViewModel()
        model.uiState.first { !it.isLoading }

        val order = listOf(Screen.Water.route, Screen.Compass.route, Screen.Ruler.route)
        model.updateFavoritesOrder(order)

        val stateAfter = model.uiState.first { it.favoriteRoutes.size == 3 }
        assertEquals(order, stateAfter.favoriteRoutes)

        val reordered = listOf(Screen.Ruler.route, Screen.Water.route, Screen.Compass.route)
        model.updateFavoritesOrder(reordered)

        val stateReordered = model.uiState.first { it.favoriteRoutes.firstOrNull() == Screen.Ruler.route }
        assertEquals(reordered, stateReordered.favoriteRoutes)
    }

    @Test
    fun buildSearchIndex_excluyeAboutEIndexaSubcategorias() {
        val index = buildSearchIndexForAllTools(app, ToolRegistry.tools)

        assertFalse(index.any { it.route == Screen.About.route })
        assertEquals(ToolRegistry.tools.count(::isCatalogTool), index.size)

        val locale = Locale.getDefault()
        val matchedMeasurement = index.filter { matchesQuery(it, "measurement", locale) || matchesQuery(it, "medicion", locale) }
        assertTrue(matchedMeasurement.any { it.route == Screen.Ruler.route })
    }

    @Test
    fun viewModel_puedeSerInstanciadoPorAndroidViewModelFactory() {
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        val created = factory.create(CategoryViewModel::class.java).also { vm = it }
        assertNotNull(created)
    }
}
