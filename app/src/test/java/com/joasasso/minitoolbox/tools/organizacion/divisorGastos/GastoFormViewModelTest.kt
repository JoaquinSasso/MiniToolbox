package com.joasasso.minitoolbox.tools.organizacion.divisorGastos

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.joasasso.minitoolbox.R
import com.joasasso.minitoolbox.TestApplication
import com.joasasso.minitoolbox.data.Gasto
import com.joasasso.minitoolbox.data.Reunion
import com.joasasso.minitoolbox.data.ReunionesRepository
import com.joasasso.minitoolbox.data.reunionesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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
class GastoFormViewModelTest {

    private lateinit var app: Application
    private val testDispatcher = UnconfinedTestDispatcher()
    private var vm: GastoFormViewModel? = null

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
    fun modoAgregar_validaDescripcionObligatoria() = runTest {
        val reunion = Reunion("r1", "Reunion", 1000L, listOf("Nico", "Joaco"), emptyList())
        ReunionesRepository.agregarReunion(app, reunion)

        val model = GastoFormViewModel(app, SavedStateHandle(mapOf("reunionId" to "r1")))
        vm = model
        model.uiState.first { !it.isLoading }

        val eventDeferred = async(UnconfinedTestDispatcher()) {
            model.events.first()
        }

        model.guardarGasto()

        val event = eventDeferred.await()
        assertTrue(event is GastoFormEvent.ShowError)
        assertEquals(R.string.expense_name_required, (event as GastoFormEvent.ShowError).messageResId)
    }

    @Test
    fun modoAgregar_validaMontoMayorACero() = runTest {
        val reunion = Reunion("r1", "Reunion", 1000L, listOf("Nico", "Joaco"), emptyList())
        ReunionesRepository.agregarReunion(app, reunion)

        val model = GastoFormViewModel(app, SavedStateHandle(mapOf("reunionId" to "r1")))
        vm = model
        model.uiState.first { !it.isLoading }

        val eventDeferred = async(UnconfinedTestDispatcher()) {
            model.events.first()
        }

        model.onDescripcionChange("Bebidas")
        model.guardarGasto()

        val event = eventDeferred.await()
        assertTrue(event is GastoFormEvent.ShowError)
        assertEquals(R.string.expense_amount_required, (event as GastoFormEvent.ShowError).messageResId)
    }

    @Test
    fun modoAgregar_validaConsumidoresObligatorios() = runTest {
        val reunion = Reunion("r1", "Reunion", 1000L, listOf("Nico", "Joaco"), emptyList())
        ReunionesRepository.agregarReunion(app, reunion)

        val model = GastoFormViewModel(app, SavedStateHandle(mapOf("reunionId" to "r1")))
        vm = model
        model.uiState.first { !it.isLoading }

        val eventDeferred = async(UnconfinedTestDispatcher()) {
            model.events.first()
        }

        model.onDescripcionChange("Bebidas")
        model.onAporteChange("Nico", "100")
        // Desactivar todos los consumidores
        model.onConsumidorToggle("Nico", false)
        model.onConsumidorToggle("Joaco", false)

        model.guardarGasto()

        val event = eventDeferred.await()
        assertTrue(event is GastoFormEvent.ShowError)
        assertEquals(R.string.expense_consumers_required, (event as GastoFormEvent.ShowError).messageResId)
    }

    @Test
    fun modoAgregar_guardaGastoCorrectamenteYEmiteSaveSuccess() = runTest {
        val reunion = Reunion("r1", "Reunion", 1000L, listOf("Nico", "Joaco"), emptyList())
        ReunionesRepository.agregarReunion(app, reunion)

        val model = GastoFormViewModel(app, SavedStateHandle(mapOf("reunionId" to "r1")))
        vm = model
        model.uiState.first { !it.isLoading }

        val eventDeferred = async(UnconfinedTestDispatcher()) {
            model.events.first()
        }

        model.onDescripcionChange("Supermercado")
        model.onAporteChange("Nico", "25.50")
        model.guardarGasto()

        val event = eventDeferred.await()
        assertTrue(event is GastoFormEvent.SaveSuccess)

        val rActualizada = ReunionesRepository.flujoReuniones(app).first {
            it.firstOrNull { r -> r.id == "r1" }?.gastos?.isNotEmpty() == true
        }.first { it.id == "r1" }

        assertEquals(1, rActualizada.gastos.size)
        val gasto = rActualizada.gastos[0]
        assertEquals("Supermercado", gasto.descripcion)
        assertEquals(2550L, gasto.totalEnCentavos())
        assertEquals(2, gasto.consumidoPor.size)
    }

    @Test
    fun modoEdicion_cargaGastoExistenteYActualizaExitosamente() = runTest {
        val gastoOriginal = Gasto(
            id = "g1",
            descripcion = "Snacks",
            consumidoPor = mapOf("Nico" to 1, "Joaco" to 0),
            aportesCentavos = mapOf("Nico" to 1500L)
        )
        val reunion = Reunion("r1", "Reunion", 1000L, listOf("Nico", "Joaco"), listOf(gastoOriginal))
        ReunionesRepository.agregarReunion(app, reunion)

        val model = GastoFormViewModel(app, SavedStateHandle(mapOf("reunionId" to "r1", "gastoId" to "g1")))
        vm = model
        val loadedState = model.uiState.first { !it.isLoading && it.descripcion.isNotEmpty() }

        assertTrue(loadedState.isEditMode)
        assertEquals("Snacks", loadedState.descripcion)
        assertEquals("15", loadedState.aportes["Nico"])
        assertEquals(1, loadedState.consumidores["Nico"])
        assertEquals(0, loadedState.consumidores["Joaco"])

        val eventDeferred = async(UnconfinedTestDispatcher()) {
            model.events.first()
        }

        model.onDescripcionChange("Snacks y Golosinas")
        model.onAporteChange("Nico", "20")
        model.onConsumidorToggle("Joaco", true)
        model.guardarGasto()

        val event = eventDeferred.await()
        assertTrue(event is GastoFormEvent.SaveSuccess)

        val rActualizada = ReunionesRepository.flujoReuniones(app).first {
            it.firstOrNull { r -> r.id == "r1" }?.gastos?.firstOrNull()?.descripcion == "Snacks y Golosinas"
        }.first { it.id == "r1" }

        assertEquals(1, rActualizada.gastos.size)
        val gEditado = rActualizada.gastos[0]
        assertEquals("g1", gEditado.id)
        assertEquals("Snacks y Golosinas", gEditado.descripcion)
        assertEquals(2000L, gEditado.totalEnCentavos())
        assertEquals(1, gEditado.consumidoPor["Joaco"])
    }

    @Test
    fun viewModel_tieneConstructorParaSavedStateViewModelFactory() {
        val ctor = GastoFormViewModel::class.java.getConstructor(
            Application::class.java,
            SavedStateHandle::class.java
        )
        assertNotNull(ctor)
        val instance = ctor.newInstance(app, SavedStateHandle(mapOf("reunionId" to "123")))
        assertNotNull(instance)
    }
}
