package com.joasasso.minitoolbox.tools.organizacion.divisorGastos

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.joasasso.minitoolbox.TestApplication
import com.joasasso.minitoolbox.data.Gasto
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class ReunionDetailViewModelTest {

    private lateinit var app: Application
    private val testDispatcher = StandardTestDispatcher()
    private var vm: ReunionDetailViewModel? = null

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
    fun uiState_emiteDetalleYDeudasCalculadasReactivamente() = runTest {
        val gasto = Gasto(
            id = "g1",
            descripcion = "Asado",
            consumidoPor = mapOf("Nico" to 1, "Joaco" to 1),
            aportesCentavos = mapOf("Nico" to 10000L)
        )
        val reunion = Reunion(
            id = "r1",
            nombre = "Juntada",
            fecha = 1000L,
            integrantes = listOf("Nico", "Joaco"),
            gastos = listOf(gasto)
        )
        ReunionesRepository.agregarReunion(app, reunion)

        val savedState = SavedStateHandle(mapOf("reunionId" to "r1"))
        val model = ReunionDetailViewModel(app, savedState)
        vm = model

        val state = model.uiState.first { it.reunion != null }
        assertNotNull(state.reunion)
        assertEquals("Juntada", state.reunion?.nombre)
        assertEquals(1, state.deudas.size)
        assertTrue(state.deudas[0].contains("Joaco") && state.deudas[0].contains("Nico"))
        assertTrue(state.textoCompartir.contains("Juntada"))
    }

    @Test
    fun eliminarGasto_actualizaUiStateYRecalculaDeudas() = runTest {
        val gasto = Gasto(
            id = "g1",
            descripcion = "Comida",
            consumidoPor = mapOf("Nico" to 1, "Joaco" to 1),
            aportesCentavos = mapOf("Nico" to 4000L)
        )
        val reunion = Reunion(
            id = "r1",
            nombre = "Almuerzo",
            fecha = 1000L,
            integrantes = listOf("Nico", "Joaco"),
            gastos = listOf(gasto)
        )
        ReunionesRepository.agregarReunion(app, reunion)

        val savedState = SavedStateHandle(mapOf("reunionId" to "r1"))
        val model = ReunionDetailViewModel(app, savedState)
        vm = model

        model.uiState.first { it.reunion != null && it.deudas.isNotEmpty() }

        model.eliminarGasto("g1")

        val stateActualizado = model.uiState.first { it.reunion != null && it.reunion!!.gastos.isEmpty() }
        assertEquals(0, stateActualizado.reunion?.gastos?.size)
        assertTrue(stateActualizado.deudas.isEmpty())
    }

    @Test
    fun actualizarIntegrante_renombraEnUiState() = runTest {
        val reunion = Reunion(
            id = "r1",
            nombre = "Reunion",
            fecha = 1000L,
            integrantes = listOf("Nico"),
            gastos = emptyList()
        )
        ReunionesRepository.agregarReunion(app, reunion)

        val savedState = SavedStateHandle(mapOf("reunionId" to "r1"))
        val model = ReunionDetailViewModel(app, savedState)
        vm = model

        model.uiState.first { it.reunion != null }

        model.actualizarIntegrante("Nico", "Nicolas")

        val stateActualizado = model.uiState.first { it.reunion?.integrantes?.contains("Nicolas") == true }
        assertTrue(stateActualizado.reunion!!.integrantes.contains("Nicolas"))
        assertFalse(stateActualizado.reunion!!.integrantes.contains("Nico"))
    }

    @Test
    fun agregarYEliminarIntegrante_modificaIntegrantesEnUiState() = runTest {
        val reunion = Reunion(
            id = "r1",
            nombre = "Reunion",
            fecha = 1000L,
            integrantes = listOf("Nico"),
            gastos = emptyList()
        )
        ReunionesRepository.agregarReunion(app, reunion)

        val savedState = SavedStateHandle(mapOf("reunionId" to "r1"))
        val model = ReunionDetailViewModel(app, savedState)
        vm = model

        model.uiState.first { it.reunion != null }

        model.agregarIntegrante("Pedro")
        val stateConPedro = model.uiState.first { it.reunion?.integrantes?.contains("Pedro") == true }
        assertEquals(listOf("Nico", "Pedro"), stateConPedro.reunion!!.integrantes)

        model.eliminarIntegrante("Pedro")
        val stateSinPedro = model.uiState.first { it.reunion?.integrantes?.contains("Pedro") == false }
        assertEquals(listOf("Nico"), stateSinPedro.reunion!!.integrantes)
    }

    @Test
    fun viewModel_tieneConstructorParaSavedStateViewModelFactory() {
        val ctor = ReunionDetailViewModel::class.java.getConstructor(
            Application::class.java,
            SavedStateHandle::class.java
        )
        assertNotNull(ctor)
        val instance = ctor.newInstance(app, SavedStateHandle(mapOf("reunionId" to "123")))
        assertNotNull(instance)
    }
}
