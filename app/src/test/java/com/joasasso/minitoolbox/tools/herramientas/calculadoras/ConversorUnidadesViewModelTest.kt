package com.joasasso.minitoolbox.tools.herramientas.calculadoras

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversorUnidadesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private var vm: ConversorUnidadesViewModel? = null

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        vm = ConversorUnidadesViewModel()
    }

    @After
    fun tearDown() {
        vm?.viewModelScope?.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_esCorrecto() {
        val state = vm!!.uiState.value
        assertEquals(UnitCategory.Length, state.category)
        assertEquals("", state.input)
        assertEquals("m", state.fromUnit.id)
        assertEquals("cm", state.toUnit.id)
        assertNull(state.formattedResult)
        assertNull(state.rawResult)
        assertFalse(state.showInfoDialog)
        // Con entrada vacía, allConversions provee referencias para 1 unidad base
        assertEquals(UnitConverterEngine.lengthUnits.size, state.allConversions.size)
        val cmItem = state.allConversions.first { it.unit.id == "cm" }
        assertEquals(100.0, cmItem.rawValue, 1e-6)
    }

    @Test
    fun onInputChanged_calculaResultadoCorrecto() {
        vm!!.onInputChanged("5")
        val state = vm!!.uiState.value
        assertEquals("5", state.input)
        assertEquals(500.0, state.rawResult!!, 1e-6)
        assertEquals("500", state.formattedResult)

        val mmItem = state.allConversions.first { it.unit.id == "mm" }
        assertEquals(5000.0, mmItem.rawValue, 1e-6)
    }

    @Test
    fun onInputChanged_sanitizaCaracteresYMultiplesSeparadores() {
        vm!!.onInputChanged("12..3,,4a")
        val state = vm!!.uiState.value
        // Solo debe admitir el primer separador decimal
        assertEquals("12.34", state.input)
    }

    @Test
    fun onCategorySelected_actualizaCategoriaYUnidades() {
        vm!!.onInputChanged("10")
        vm!!.onCategorySelected(UnitCategory.Weight)

        val state = vm!!.uiState.value
        assertEquals(UnitCategory.Weight, state.category)
        assertEquals("kg", state.fromUnit.id)
        assertEquals("g", state.toUnit.id)
        assertEquals(10000.0, state.rawResult!!, 1e-6)
        assertEquals("10,000", state.formattedResult)
    }

    @Test
    fun onSwapUnits_invierteOrigenYDestino() {
        vm!!.onInputChanged("100")
        // Inicial: 100 m = 10,000 cm
        assertEquals(10000.0, vm!!.uiState.value.rawResult!!, 1e-6)

        vm!!.onSwapUnits()
        val state = vm!!.uiState.value
        assertEquals("cm", state.fromUnit.id)
        assertEquals("m", state.toUnit.id)
        // 100 cm = 1 m
        assertEquals(1.0, state.rawResult!!, 1e-6)
        assertEquals("1", state.formattedResult)
    }

    @Test
    fun onClearInput_reseteaResultadoYMantieneReferencia() {
        vm!!.onInputChanged("50")
        assertNotNull(vm!!.uiState.value.rawResult)

        vm!!.onClearInput()
        val state = vm!!.uiState.value
        assertEquals("", state.input)
        assertNull(state.rawResult)
        assertNull(state.formattedResult)
        assertTrue(state.allConversions.isNotEmpty())
    }

    @Test
    fun onQuickPresetSelected_asignaValorYCalcula() {
        vm!!.onQuickPresetSelected("25")
        val state = vm!!.uiState.value
        assertEquals("25", state.input)
        assertEquals(2500.0, state.rawResult!!, 1e-6)
    }

    @Test
    fun onUnitSelectedFromList_actualizaUnidadDestino() {
        vm!!.onInputChanged("1")
        val mm = UnitConverterEngine.lengthUnits.first { it.id == "mm" }
        vm!!.onUnitSelectedFromList(mm)

        val state = vm!!.uiState.value
        assertEquals("mm", state.toUnit.id)
        assertEquals(1000.0, state.rawResult!!, 1e-6)
    }

    @Test
    fun onShowInfoDialog_actualizaEstado() {
        vm!!.onShowInfoDialog(true)
        assertTrue(vm!!.uiState.value.showInfoDialog)

        vm!!.onShowInfoDialog(false)
        assertFalse(vm!!.uiState.value.showInfoDialog)
    }

    @Test
    fun onCopyResult_emiteEventoDePortapapeles() = runTest {
        vm!!.onInputChanged("2.5")
        vm!!.onCopyResult()

        val event = vm!!.events.first()
        assertTrue(event is ConversorUnidadesEvent.CopyToClipboard)
        assertEquals("250", (event as ConversorUnidadesEvent.CopyToClipboard).text)
    }
}
