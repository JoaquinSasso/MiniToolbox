package com.joasasso.minitoolbox.ui

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.joasasso.minitoolbox.CategoriesScreen
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.TestApplication
import com.joasasso.minitoolbox.data.FavoritesRepository
import com.joasasso.minitoolbox.nav.Screen
import com.joasasso.minitoolbox.tools.ToolRegistry
import com.joasasso.minitoolbox.viewmodel.CategoryViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Arranque en frío: DataStore todavía no leyó los favoritos cuando se dibuja el primer frame.
 * La pantalla tiene que quedar arriba de todo (buscador y favoritos visibles) cuando llegan.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class CategoriesScreenColdStartTest {

    @get:Rule
    val compose = createComposeRule()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private var vm: CategoryViewModel? = null

    /** Repositorio cuyos favoritos llegan cuando el test lo decide, como un DataStore lento. */
    private class DelayedFavoritesRepository : FavoritesRepository {
        val favorites = MutableStateFlow<List<String>?>(null)
        override val favoritesFlow: Flow<List<String>> = favorites.filterNotNull()
        override suspend fun toggleFavorite(route: String) = Unit
        override suspend fun setFavoritesOrder(newOrder: List<String>) = Unit
    }

    @After
    fun tearDown() {
        vm?.viewModelScope?.cancel()
    }

    @Test
    fun favoritosLleganDespuesDelPrimerFrame_buscadorYFavoritosQuedanVisibles() {
        val repository = DelayedFavoritesRepository()
        val model = CategoryViewModel(app, repository).also { vm = it }

        compose.setContent {
            MaterialTheme {
                CategoriesScreen(
                    tools = ToolRegistry.tools,
                    onToolClick = {},
                    vm = model,
                    onNavigateToPro = {},
                    onNavigateToAbout = {}
                )
            }
        }
        compose.waitForIdle()

        repository.favorites.value = listOf(Screen.Compass.route, Screen.Ruler.route)
        compose.waitForIdle()

        compose.onNodeWithText(app.getString(R.string.categories_search_hint)).assertIsDisplayed()
        compose.onNodeWithText(app.getString(R.string.catalog_favorites_title)).assertIsDisplayed()
    }
}
