package com.joasasso.minitoolbox.tools.herramientas.generadores

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Coordenadas de posición para congelar el resultado en pantalla cuando se levantan los dedos.
 */
data class TouchPoint(
    val x: Float,
    val y: Float
)

data class GroupSelectorUiState(
    val mode: PartitionMode = PartitionMode.BY_TEAM_COUNT,
    val targetValue: Int = 2,
    val isFrozen: Boolean = false,
    val frozenAssignments: Map<Long, Int> = emptyMap(),
    val frozenPositions: Map<Long, TouchPoint> = emptyMap(),
    val teamSizes: List<Int> = emptyList(),
    val showInfoDialog: Boolean = false
)

class GroupSelectorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GroupSelectorUiState())
    val uiState: StateFlow<GroupSelectorUiState> = _uiState.asStateFlow()

    fun setMode(newMode: PartitionMode) {
        _uiState.update { current ->
            if (current.mode == newMode) current
            else current.copy(
                mode = newMode,
                isFrozen = false,
                frozenAssignments = emptyMap(),
                frozenPositions = emptyMap(),
                teamSizes = emptyList()
            )
        }
    }

    fun setTargetValue(value: Int) {
        val clamped = value.coerceIn(2, 5)
        _uiState.update { current ->
            if (current.targetValue == clamped) current
            else current.copy(
                targetValue = clamped,
                isFrozen = false,
                frozenAssignments = emptyMap(),
                frozenPositions = emptyMap(),
                teamSizes = emptyList()
            )
        }
    }

    fun completeSelection(
        positions: Map<Long, TouchPoint>,
        shuffler: (List<Long>) -> List<Long> = { it.shuffled() }
    ): Boolean {
        val currentState = _uiState.value
        val pointerIds = positions.keys.toList()

        val partitionResult = GroupPartitionEngine.partition(
            items = pointerIds,
            mode = currentState.mode,
            targetValue = currentState.targetValue,
            shuffler = shuffler
        )

        return when (partitionResult) {
            is PartitionResult.Success -> {
                _uiState.update { current ->
                    current.copy(
                        isFrozen = true,
                        frozenAssignments = partitionResult.assignments,
                        frozenPositions = positions,
                        teamSizes = partitionResult.teamSizes
                    )
                }
                true
            }
            is PartitionResult.InsufficientParticipants -> false
        }
    }

    fun reset() {
        _uiState.update { current ->
            current.copy(
                isFrozen = false,
                frozenAssignments = emptyMap(),
                frozenPositions = emptyMap(),
                teamSizes = emptyList()
            )
        }
    }

    fun setShowInfoDialog(show: Boolean) {
        _uiState.update { it.copy(showInfoDialog = show) }
    }
}
