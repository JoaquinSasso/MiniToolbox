package com.joasasso.minitoolbox.tools.organizacion.divisorGastos

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.joasasso.minitoolbox.TestApplication
import com.joasasso.minitoolbox.data.Reunion
import com.joasasso.minitoolbox.data.ReunionesRepository
import com.joasasso.minitoolbox.data.reunionesDataStore
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class ReunionesViewModelTest {

    private lateinit var app: Application
    private val testDispatcher = StandardTestDispatcher()
    private var vm: ReunionesViewModel? = null

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        app.reunionesDataStore.edit { it.clear() }
    }

    @After
    fun tearDown() {
        vm?.viewModelScope?.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_emiteReunionesOrdenadasPorFechaDescendente() = runTest {
        val r1 = Reunion(id = "1", nombre = "Vieja", fecha = 100L, integrantes = emptyList(), gastos = emptyList())
        val r2 = Reunion(id = "2", nombre = "Nueva", fecha = 200L, integrantes = emptyList(), gastos = emptyList())
        ReunionesRepository.agregarReunion(app, r1)
        ReunionesRepository.agregarReunion(app, r2)

        val model = ReunionesViewModel(app)
        vm = model
        val state = model.uiState.first { it.reuniones.isNotEmpty() }

        assertEquals(2, state.reuniones.size)
        assertEquals("2", state.reuniones[0].id)
        assertEquals("1", state.reuniones[1].id)
        assertFalse(state.isLoading)
    }

    @Test
    fun eliminarReunion_remueveReunionDeUiState() = runTest {
        val r1 = Reunion(id = "1", nombre = "A Borrar", fecha = 100L, integrantes = emptyList(), gastos = emptyList())
        ReunionesRepository.agregarReunion(app, r1)

        val model = ReunionesViewModel(app)
        vm = model
        model.uiState.first { it.reuniones.isNotEmpty() }

        model.eliminarReunion("1")

        val stateAfter = model.uiState.first { it.reuniones.isEmpty() }
        assertEquals(0, stateAfter.reuniones.size)
    }

    @Test
    fun viewModel_puedeSerInstanciadoPorAndroidViewModelFactory() {
        val factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        val created = factory.create(ReunionesViewModel::class.java)
        org.junit.Assert.assertNotNull(created)
    }
}
