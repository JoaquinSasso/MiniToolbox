package com.joasasso.minitoolbox.tools.herramientas.calculadoras

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class ConversorUnidadesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private var vm: ConversorUnidadesViewModel? = null
    private val es = Locale.forLanguageTag("es-AR")

    private val viewModel get() = vm!!
    private val state get() = viewModel.uiState.value

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        vm = ConversorUnidadesViewModel(localeProvider = { es })
    }

    @After
    fun tearDown() {
        vm?.viewModelScope?.cancel()
        Dispatchers.resetMain()
    }

    private fun unit(id: String) = UnitConverterEngine.unit(id)
    private fun row(id: String) = state.results.first { it.unit.id == id }

    @Test
    fun estadoInicial_longitudEnCentimetrosSinResultados() {
        assertEquals(UnitCategory.Length, state.category)
        assertEquals("cm", state.sourceUnit.id)
        assertEquals("", state.input)
        assertTrue(state.results.isEmpty())
        assertNull(state.error)
        assertFalse(state.showInfoDialog)
    }

    // -------------------------------------------------------------------------
    // Tareas de referencia: cada test reproduce las interacciones contadas en el reporte
    // -------------------------------------------------------------------------

    @Test
    fun tarea1_pieYPulgadasACentimetros() {
        viewModel.onInputChanged("5'11")
        assertEquals("ft_in", state.sourceUnit.id)
        assertEquals("cm", state.results.first().unit.id)
        assertEquals("180,34", state.results.first().display)
    }

    @Test
    fun tarea2_centimetrosAPiesYPulgadas() {
        viewModel.onInputChanged("180")
        assertEquals("ft_in", state.results.first().unit.id)
        assertEquals("5'10,9\"", state.results.first().display)
    }

    @Test
    fun tarea3_kilosALibras() {
        viewModel.onCategorySelected(UnitCategory.Weight)
        viewModel.onInputChanged("70")
        assertEquals("lb", state.results.first().unit.id)
        assertEquals("154,324", state.results.first().display)
    }

    @Test
    fun tarea4_celsiusAFahrenheit() {
        viewModel.onCategorySelected(UnitCategory.Temperature)
        viewModel.onInputChanged("30")
        assertEquals("f", state.results.first().unit.id)
        assertEquals("86", state.results.first().display)
    }

    @Test
    fun tarea5_horasAMinutos() {
        viewModel.onCategorySelected(UnitCategory.Time)
        viewModel.onInputChanged("2,5")
        assertEquals("min", state.results.first().unit.id)
        assertEquals("150", state.results.first().display)
    }

    // -------------------------------------------------------------------------
    // Entrada
    // -------------------------------------------------------------------------

    @Test
    fun resultados_noIncluyenLaUnidadDeOrigen() {
        viewModel.onInputChanged("180")
        assertTrue(state.results.none { it.unit.id == "cm" })
        assertEquals(UnitConverterEngine.lengthUnits.size - 1, state.results.size)
    }

    @Test
    fun unidadEscrita_cambiaOrigenYCategoria() {
        viewModel.onInputChanged("70 kg")
        assertEquals(UnitCategory.Weight, state.category)
        assertEquals("kg", state.sourceUnit.id)
        assertEquals("lb", state.results.first().unit.id)
    }

    @Test
    fun copyTextNoLlevaSeparadorDeMiles() {
        viewModel.onInputChanged("1")
        viewModel.onSourceUnitSelected(unit("mi"))
        assertEquals("1.609,34", row("m").display)
        assertEquals("1609,34", row("m").copyText)
    }

    @Test
    fun entradaDemasiadoLarga_seRecortaYAvisaALaUi() {
        val revision = state.inputRevision
        viewModel.onInputChanged("1".repeat(60))
        assertEquals(40, state.input.length)
        assertEquals(revision + 1, state.inputRevision)
    }

    @Test
    fun escribir_noSubeLaRevision() {
        val revision = state.inputRevision
        viewModel.onInputChanged("1")
        viewModel.onInputChanged("18")
        assertEquals(revision, state.inputRevision)
    }

    @Test
    fun borrar_limpiaResultadosYErrores() {
        viewModel.onInputChanged("5'13")
        viewModel.onClearInput()
        assertEquals("", state.input)
        assertTrue(state.results.isEmpty())
        assertNull(state.error)
    }

    // -------------------------------------------------------------------------
    // Errores
    // -------------------------------------------------------------------------

    @Test
    fun entradaInvalida_conservaResultadosAnterioresComoDesactualizados() {
        viewModel.onInputChanged("5'1")
        val previous = state.results
        viewModel.onInputChanged("5'13")
        assertEquals(InputErrorKind.InchesOutOfRange, state.error!!.kind)
        assertEquals("6'1\"", state.error!!.suggestionText)
        assertEquals(previous, state.results)
        assertTrue(state.resultsAreStale)
    }

    @Test
    fun entradaInvalida_sinResultadosPrevios_noMarcaDesactualizado() {
        viewModel.onInputChanged("hola")
        assertEquals(InputErrorKind.InvalidFormat, state.error!!.kind)
        assertFalse(state.resultsAreStale)
    }

    @Test
    fun aceptarSugerencia_aplicaLaMedidaNormalizada() {
        viewModel.onInputChanged("5'13")
        val revision = state.inputRevision
        viewModel.onSuggestionAccepted()
        assertEquals("6'1\"", state.input)
        assertNull(state.error)
        assertEquals("ft_in", state.sourceUnit.id)
        assertEquals("185,42", row("cm").display)
        assertEquals(revision + 1, state.inputRevision)
    }

    @Test
    fun aceptarSugerencia_sinSugerencia_noHaceNada() {
        viewModel.onInputChanged("hola")
        viewModel.onSuggestionAccepted()
        assertEquals("hola", state.input)
    }

    @Test
    fun unidadDesconocida_informaLaPalabra() {
        viewModel.onInputChanged("5 xyz")
        assertEquals(InputErrorKind.UnknownUnit, state.error!!.kind)
        assertEquals("xyz", state.error!!.detail)
    }

    // -------------------------------------------------------------------------
    // Categoría y unidad de origen
    // -------------------------------------------------------------------------

    @Test
    fun cambiarCategoria_conservaUnNumeroSolo() {
        viewModel.onInputChanged("70")
        viewModel.onCategorySelected(UnitCategory.Weight)
        assertEquals("70", state.input)
        assertEquals("kg", state.sourceUnit.id)
        assertEquals("154,324", row("lb").display)
    }

    @Test
    fun cambiarCategoria_borraTextoConUnidadPropia() {
        viewModel.onInputChanged("180 cm")
        viewModel.onCategorySelected(UnitCategory.Weight)
        assertEquals("", state.input)
        assertEquals(UnitCategory.Weight, state.category)
        assertTrue(state.results.isEmpty())
    }

    @Test
    fun cambiarCategoria_borraTextoInvalido() {
        viewModel.onInputChanged("5'13")
        viewModel.onCategorySelected(UnitCategory.Time)
        assertEquals("", state.input)
        assertNull(state.error)
    }

    @Test
    fun cambiarCategoria_aLaMisma_noHaceNada() {
        viewModel.onInputChanged("180 cm")
        viewModel.onCategorySelected(UnitCategory.Length)
        assertEquals("180 cm", state.input)
    }

    @Test
    fun elegirUnidadDeOrigen_reinterpretaElNumero() {
        viewModel.onInputChanged("71")
        viewModel.onSourceUnitSelected(unit("in"))
        assertEquals("71", state.input)
        assertEquals("180,34", row("cm").display)
    }

    @Test
    fun elegirUnidadDeOrigen_conUnidadEscrita_borraElTexto() {
        viewModel.onInputChanged("5'11")
        viewModel.onSourceUnitSelected(unit("m"))
        assertEquals("", state.input)
        assertEquals("m", state.sourceUnit.id)
    }

    @Test
    fun unidadDeOrigenCompuesta_aceptaDosNumeros() {
        viewModel.onSourceUnitSelected(unit("ft_in"))
        viewModel.onInputChanged("5 11")
        assertEquals("180,34", row("cm").display)
    }

    // -------------------------------------------------------------------------
    // Tocar una fila
    // -------------------------------------------------------------------------

    @Test
    fun tocarFila_laVuelveOrigenConElValorConvertido() {
        viewModel.onInputChanged("180")
        viewModel.onResultSelected(unit("in"))
        assertEquals("in", state.sourceUnit.id)
        assertEquals("70,8661", state.input)
        assertEquals("cm", state.results.first().unit.id)
        assertEquals("180", row("cm").display)
    }

    @Test
    fun tocarFilaCompuesta_escribeLaMedidaCompuesta() {
        viewModel.onInputChanged("180")
        viewModel.onResultSelected(unit("ft_in"))
        assertEquals("5'10,9\"", state.input)
        assertEquals("ft_in", state.sourceUnit.id)
    }

    @Test
    fun tocarFila_conResultadosDesactualizados_noHaceNada() {
        viewModel.onInputChanged("180")
        viewModel.onInputChanged("180 xyz")
        viewModel.onResultSelected(unit("in"))
        assertEquals("180 xyz", state.input)
        assertEquals("cm", state.sourceUnit.id)
    }

    // -------------------------------------------------------------------------
    // Copiar y diálogo
    // -------------------------------------------------------------------------

    @Test
    fun copiar_emiteTextoYMensaje() = runTest(testDispatcher) {
        viewModel.onInputChanged("180")
        viewModel.onCopyResult(unit("ft_in"))
        val events = viewModel.events.take(2).toList()
        assertEquals(ConversorUnidadesEvent.CopyToClipboard("5'10,9\""), events[0])
        assertEquals(ConversorUnidadesEvent.ShowCopiedMessage, events[1])
    }

    @Test
    fun copiar_unidadSinResultado_noEmite() = runTest(testDispatcher) {
        viewModel.onCopyResult(unit("in"))
        viewModel.onInputChanged("1")
        viewModel.onCopyResult(unit("m"))
        val first = viewModel.events.first()
        assertEquals(ConversorUnidadesEvent.CopyToClipboard("0,01"), first)
    }

    @Test
    fun dialogoDeInfo_seAbreYCierra() {
        viewModel.onShowInfoDialog(true)
        assertTrue(state.showInfoDialog)
        viewModel.onShowInfoDialog(false)
        assertFalse(state.showInfoDialog)
    }
}
