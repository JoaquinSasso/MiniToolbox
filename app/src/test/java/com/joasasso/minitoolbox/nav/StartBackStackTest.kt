package com.joasasso.minitoolbox.nav

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.test.core.app.ApplicationProvider
import com.joasasso.minitoolbox.TestApplication
import com.joasasso.minitoolbox.prepareStartBackStack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pila inicial del NavHost en un arranque desde widget, notificación o atajo: tiene que
 * quedar armada antes del primer frame, con el catálogo debajo para que "atrás" vuelva.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class StartBackStackTest {

    private lateinit var navController: NavHostController
    private lateinit var graph: NavGraph
    private val lifecycleOwner = object : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this).apply {
            currentState = Lifecycle.State.RESUMED
        }
        override val lifecycle: Lifecycle get() = registry
    }

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        navController = NavHostController(context).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
        }
        graph = navController.createGraph(startDestination = Screen.Categories) {
            composable<Screen.Categories> {}
            composable<Screen.Water> {}
        }
    }

    private fun prepare(initial: Screen?) = navController.prepareStartBackStack(
        lifecycleOwner = lifecycleOwner,
        viewModelStore = ViewModelStore(),
        graph = graph,
        initialDestination = initial
    )

    private fun stack() = navController.currentBackStack.value
        .map { it.destination }
        .filter { it !is NavGraph }

    @Test
    fun sinDestino_laPilaEsSoloElCatalogo() {
        prepare(null)

        val stack = stack()
        assertEquals(1, stack.size)
        assertTrue(stack.single().hasRoute(Screen.Categories::class))
    }

    @Test
    fun conDestino_laHerramientaQuedaArribaDelCatalogo() {
        prepare(Screen.Water)

        val stack = stack()
        assertEquals(2, stack.size)
        assertTrue(stack[0].hasRoute(Screen.Categories::class))
        assertTrue(stack[1].hasRoute(Screen.Water::class))
    }

    @Test
    fun destinoFueraDelGrafo_seQuedaEnElCatalogo() {
        prepare(Screen.Compass)

        val stack = stack()
        assertEquals(1, stack.size)
        assertTrue(stack.single().hasRoute(Screen.Categories::class))
    }

    @Test
    fun volverAFijarElMismoGrafo_noReiniciaLaPila() {
        prepare(Screen.Water)

        // Lo mismo que hace NavHost en su primera composición.
        navController.graph = graph

        assertTrue(stack().last().hasRoute(Screen.Water::class))
        assertEquals(2, stack().size)
    }
}
