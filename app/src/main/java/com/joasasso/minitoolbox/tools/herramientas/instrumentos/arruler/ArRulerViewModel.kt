package com.joasasso.minitoolbox.tools.herramientas.instrumentos.arruler

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel para la Regla AR.
 * Gestiona el estado de las mediciones, modo activo, unidades y acciones de deshacer/limpiar.
 * Desacoplado completamente de ARCore nativo para permitir ejecución en JVM.
 */
class ArRulerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ArRulerUiState())
    val uiState: StateFlow<ArRulerUiState> = _uiState.asStateFlow()

    private var counter = 0

    fun toggleUnits() {
        _uiState.update { current ->
            current.copy(unitSystem = current.unitSystem.toggle())
        }
    }

    fun changeMode(newMode: MeasureMode) {
        if (newMode == _uiState.value.mode) return
        _uiState.update { current ->
            current.copy(
                mode = newMode,
                draftLocals = emptyList(),
                canUndo = current.measurements.isNotEmpty(),
                canFinish = false
            )
        }
    }

    /**
     * Agrega un punto al borrador en construcción en el espacio local del ancla.
     * En modo [MeasureMode.SEGMENT], al colocar el 2do punto confirma automáticamente la medición.
     * Retorna el ID de la medición si fue confirmada, o null si el borrador sigue en curso.
     */
    fun addPoint(localPoint: FloatArray): Int? {
        val current = _uiState.value
        val newDraft = current.draftLocals + listOf(localPoint.clone())

        return if (current.mode == MeasureMode.SEGMENT && newDraft.size >= 2) {
            val segs = newDraft.zipWithNext { a, b -> dist3(a, b) }
            val newId = ++counter
            val record = MeasurementRecord(
                id = newId,
                locals = newDraft,
                segments = segs,
                total = segs.sum()
            )
            _uiState.update { state ->
                state.copy(
                    measurements = state.measurements + record,
                    draftLocals = emptyList(),
                    canUndo = true,
                    canFinish = false
                )
            }
            newId
        } else {
            _uiState.update { state ->
                state.copy(
                    draftLocals = newDraft,
                    canUndo = true,
                    canFinish = state.mode == MeasureMode.POLYLINE && newDraft.size >= 2
                )
            }
            null
        }
    }

    /**
     * Confirma el borrador actual como una nueva medición (útil para [MeasureMode.POLYLINE]).
     * Retorna el ID generado o null si no había suficientes puntos (< 2).
     */
    fun commitDraft(): Int? {
        val current = _uiState.value
        if (current.draftLocals.size < 2) return null

        val segs = current.draftLocals.zipWithNext { a, b -> dist3(a, b) }
        val newId = ++counter
        val record = MeasurementRecord(
            id = newId,
            locals = current.draftLocals,
            segments = segs,
            total = segs.sum()
        )
        _uiState.update { state ->
            state.copy(
                measurements = state.measurements + record,
                draftLocals = emptyList(),
                canUndo = true,
                canFinish = false
            )
        }
        return newId
    }

    fun cancelDraft() {
        _uiState.update { current ->
            current.copy(
                draftLocals = emptyList(),
                canUndo = current.measurements.isNotEmpty(),
                canFinish = false
            )
        }
    }

    fun undo(): Boolean {
        val current = _uiState.value
        if (current.draftLocals.isNotEmpty()) {
            if (current.draftLocals.size <= 1) {
                cancelDraft()
            } else {
                val newDraft = current.draftLocals.dropLast(1)
                _uiState.update { state ->
                    state.copy(
                        draftLocals = newDraft,
                        canUndo = true,
                        canFinish = state.mode == MeasureMode.POLYLINE && newDraft.size >= 2
                    )
                }
            }
            return true
        }

        if (current.measurements.isNotEmpty()) {
            val newMeasurements = current.measurements.dropLast(1)
            _uiState.update { state ->
                state.copy(
                    measurements = newMeasurements,
                    canUndo = newMeasurements.isNotEmpty()
                )
            }
            return true
        }

        return false
    }

    fun remove(id: Int) {
        _uiState.update { current ->
            val newMeasurements = current.measurements.filterNot { it.id == id }
            current.copy(
                measurements = newMeasurements,
                canUndo = newMeasurements.isNotEmpty() || current.draftLocals.isNotEmpty()
            )
        }
    }

    fun clearAll() {
        _uiState.update { current ->
            current.copy(
                measurements = emptyList(),
                draftLocals = emptyList(),
                canUndo = false,
                canFinish = false
            )
        }
    }

    fun format(meters: Float): String = UnitFormat.format(meters.toDouble(), _uiState.value.unitSystem)
}
