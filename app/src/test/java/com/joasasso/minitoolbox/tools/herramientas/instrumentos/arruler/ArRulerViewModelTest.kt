package com.joasasso.minitoolbox.tools.herramientas.instrumentos.arruler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ArRulerViewModelTest {

    private lateinit var viewModel: ArRulerViewModel

    @Before
    fun setUp() {
        viewModel = ArRulerViewModel()
    }

    @Test
    fun initialState_esCorrecto() {
        val state = viewModel.uiState.value
        assertEquals(Units.METRIC, state.unitSystem)
        assertEquals(MeasureMode.SEGMENT, state.mode)
        assertTrue(state.measurements.isEmpty())
        assertTrue(state.draftLocals.isEmpty())
        assertFalse(state.canUndo)
        assertFalse(state.canFinish)
    }

    @Test
    fun toggleUnits_alternaEntreMetricoEImperial() {
        viewModel.toggleUnits()
        assertEquals(Units.IMPERIAL, viewModel.uiState.value.unitSystem)

        viewModel.toggleUnits()
        assertEquals(Units.METRIC, viewModel.uiState.value.unitSystem)
    }

    @Test
    fun changeMode_cambiaModoYCancelaBorrador() {
        viewModel.addPoint(floatArrayOf(0f, 0f, 0f))
        assertEquals(1, viewModel.uiState.value.draftLocals.size)

        viewModel.changeMode(MeasureMode.POLYLINE)
        val state = viewModel.uiState.value
        assertEquals(MeasureMode.POLYLINE, state.mode)
        assertTrue(state.draftLocals.isEmpty())
        assertFalse(state.canFinish)
    }

    @Test
    fun modoSegmento_autoConfirmaAlSegundoPunto() {
        val p1 = floatArrayOf(0f, 0f, 0f)
        val p2 = floatArrayOf(1f, 0f, 0f)

        val id1 = viewModel.addPoint(p1)
        assertNull(id1)
        val stateMid = viewModel.uiState.value
        assertEquals(1, stateMid.draftLocals.size)
        assertTrue(stateMid.canUndo)
        assertFalse(stateMid.canFinish)

        val id2 = viewModel.addPoint(p2)
        assertNotNull(id2)
        assertEquals(1, id2)

        val stateFinal = viewModel.uiState.value
        assertTrue(stateFinal.draftLocals.isEmpty())
        assertEquals(1, stateFinal.measurements.size)

        val measurement = stateFinal.measurements.first()
        assertEquals(1, measurement.id)
        assertEquals(1, measurement.segments.size)
        assertEquals(1.0f, measurement.segments.first(), 1e-4f)
        assertEquals(1.0f, measurement.total, 1e-4f)
        assertTrue(stateFinal.canUndo)
        assertFalse(stateFinal.canFinish)
    }

    @Test
    fun modoPolilinea_acumulaPuntosYConfirmaConCommitDraft() {
        viewModel.changeMode(MeasureMode.POLYLINE)

        val p1 = floatArrayOf(0f, 0f, 0f)
        val p2 = floatArrayOf(1f, 0f, 0f)
        val p3 = floatArrayOf(1f, 1f, 0f)

        assertNull(viewModel.addPoint(p1))
        assertFalse(viewModel.uiState.value.canFinish)

        assertNull(viewModel.addPoint(p2))
        assertTrue(viewModel.uiState.value.canFinish)

        assertNull(viewModel.addPoint(p3))
        assertEquals(3, viewModel.uiState.value.draftLocals.size)
        assertTrue(viewModel.uiState.value.canFinish)

        val committedId = viewModel.commitDraft()
        assertNotNull(committedId)
        assertEquals(1, committedId)

        val state = viewModel.uiState.value
        assertTrue(state.draftLocals.isEmpty())
        assertEquals(1, state.measurements.size)

        val measurement = state.measurements.first()
        assertEquals(2, measurement.segments.size)
        assertEquals(1.0f, measurement.segments[0], 1e-4f)
        assertEquals(1.0f, measurement.segments[1], 1e-4f)
        assertEquals(2.0f, measurement.total, 1e-4f)
        assertFalse(state.canFinish)
    }

    @Test
    fun commitDraft_conMenosDeDosPuntos_retornaNull() {
        viewModel.changeMode(MeasureMode.POLYLINE)
        assertNull(viewModel.commitDraft())

        viewModel.addPoint(floatArrayOf(0f, 0f, 0f))
        assertNull(viewModel.commitDraft())
    }

    @Test
    fun undo_deshacePuntosDeBorradorLuegoMedicionesConfirmadas() {
        viewModel.changeMode(MeasureMode.POLYLINE)
        viewModel.addPoint(floatArrayOf(0f, 0f, 0f))
        viewModel.addPoint(floatArrayOf(1f, 0f, 0f))
        viewModel.addPoint(floatArrayOf(1f, 1f, 0f))
        assertEquals(3, viewModel.uiState.value.draftLocals.size)

        // 1er undo: de 3 a 2 puntos en borrador
        assertTrue(viewModel.undo())
        assertEquals(2, viewModel.uiState.value.draftLocals.size)
        assertTrue(viewModel.uiState.value.canFinish)

        // 2do undo: de 2 a 1 punto
        assertTrue(viewModel.undo())
        assertEquals(1, viewModel.uiState.value.draftLocals.size)
        assertFalse(viewModel.uiState.value.canFinish)

        // 3er undo: borra borrador completo
        assertTrue(viewModel.undo())
        assertTrue(viewModel.uiState.value.draftLocals.isEmpty())
        assertFalse(viewModel.uiState.value.canUndo)

        // Ahora confirmamos una medición y probamos undo sobre la medición terminada
        viewModel.changeMode(MeasureMode.SEGMENT)
        viewModel.addPoint(floatArrayOf(0f, 0f, 0f))
        viewModel.addPoint(floatArrayOf(2f, 0f, 0f))
        assertEquals(1, viewModel.uiState.value.measurements.size)

        assertTrue(viewModel.undo())
        assertTrue(viewModel.uiState.value.measurements.isEmpty())
        assertFalse(viewModel.uiState.value.canUndo)

        // Undo sobre estado vacío retorna false
        assertFalse(viewModel.undo())
    }

    @Test
    fun remove_eliminaMedicionEspecificaPorId() {
        viewModel.addPoint(floatArrayOf(0f, 0f, 0f))
        val id1 = viewModel.addPoint(floatArrayOf(1f, 0f, 0f))!!

        viewModel.addPoint(floatArrayOf(0f, 0f, 0f))
        val id2 = viewModel.addPoint(floatArrayOf(2f, 0f, 0f))!!

        assertEquals(2, viewModel.uiState.value.measurements.size)

        viewModel.remove(id1)
        val remaining = viewModel.uiState.value.measurements
        assertEquals(1, remaining.size)
        assertEquals(id2, remaining.first().id)
    }

    @Test
    fun clearAll_vaciaTodoElEstado() {
        viewModel.addPoint(floatArrayOf(0f, 0f, 0f))
        viewModel.addPoint(floatArrayOf(1f, 0f, 0f))
        viewModel.changeMode(MeasureMode.POLYLINE)
        viewModel.addPoint(floatArrayOf(3f, 3f, 3f))

        viewModel.clearAll()
        val state = viewModel.uiState.value
        assertTrue(state.measurements.isEmpty())
        assertTrue(state.draftLocals.isEmpty())
        assertFalse(state.canUndo)
        assertFalse(state.canFinish)
    }

    @Test
    fun format_utilizaElSistemaDeUnidadesActual() {
        val metricFormatted = viewModel.format(0.20f)
        assertTrue(metricFormatted.contains("cm"))

        viewModel.toggleUnits()
        val imperialFormatted = viewModel.format(0.20f)
        assertTrue(imperialFormatted.contains("\""))
    }
}
