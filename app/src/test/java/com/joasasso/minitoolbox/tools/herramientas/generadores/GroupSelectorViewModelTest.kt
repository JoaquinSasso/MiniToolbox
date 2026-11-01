package com.joasasso.minitoolbox.tools.herramientas.generadores

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GroupSelectorViewModelTest {

    private lateinit var viewModel: GroupSelectorViewModel

    @Before
    fun setUp() {
        viewModel = GroupSelectorViewModel()
    }

    @Test
    fun `initial state has default values`() {
        val state = viewModel.uiState.value
        assertEquals(PartitionMode.BY_TEAM_COUNT, state.mode)
        assertEquals(2, state.targetValue)
        assertFalse(state.isFrozen)
        assertTrue(state.frozenAssignments.isEmpty())
        assertTrue(state.frozenPositions.isEmpty())
        assertTrue(state.teamSizes.isEmpty())
        assertFalse(state.showInfoDialog)
    }

    @Test
    fun `setMode updates mode and resets frozen state`() {
        // Freeze state first with dummy data
        val dummyPositions = mapOf(
            1L to TouchPoint(10f, 10f),
            2L to TouchPoint(20f, 20f)
        )
        viewModel.completeSelection(dummyPositions)
        assertTrue(viewModel.uiState.value.isFrozen)

        // Change mode
        viewModel.setMode(PartitionMode.BY_TEAM_SIZE)
        val state = viewModel.uiState.value
        assertEquals(PartitionMode.BY_TEAM_SIZE, state.mode)
        assertFalse(state.isFrozen)
        assertTrue(state.frozenAssignments.isEmpty())
    }

    @Test
    fun `setTargetValue updates target clamped to 2 to 5 and resets frozen state`() {
        viewModel.setTargetValue(4)
        assertEquals(4, viewModel.uiState.value.targetValue)

        // Clamp upper bound
        viewModel.setTargetValue(10)
        assertEquals(5, viewModel.uiState.value.targetValue)

        // Clamp lower bound
        viewModel.setTargetValue(1)
        assertEquals(2, viewModel.uiState.value.targetValue)
    }

    @Test
    fun `completeSelection returns false when participants are insufficient`() {
        viewModel.setMode(PartitionMode.BY_TEAM_COUNT)
        viewModel.setTargetValue(3)

        val positions = mapOf(
            1L to TouchPoint(10f, 10f),
            2L to TouchPoint(20f, 20f)
        ) // only 2, need 3

        val completed = viewModel.completeSelection(positions)
        assertFalse(completed)
        assertFalse(viewModel.uiState.value.isFrozen)
    }

    @Test
    fun `completeSelection succeeds with uneven participants and freezes state`() {
        viewModel.setMode(PartitionMode.BY_TEAM_SIZE)
        viewModel.setTargetValue(3)

        // 8 fingers, size 3 -> 2 teams of 3 and 1 of 2
        val positions = (1L..8L).associateWith { TouchPoint(it.toFloat(), it.toFloat()) }

        val completed = viewModel.completeSelection(positions, shuffler = { it })
        assertTrue(completed)

        val state = viewModel.uiState.value
        assertTrue(state.isFrozen)
        assertEquals(listOf(3, 3, 2), state.teamSizes)
        assertEquals(8, state.frozenAssignments.size)
        assertEquals(8, state.frozenPositions.size)

        // Verify team assignments
        assertEquals(0, state.frozenAssignments[1L])
        assertEquals(0, state.frozenAssignments[2L])
        assertEquals(0, state.frozenAssignments[3L])
        assertEquals(1, state.frozenAssignments[4L])
        assertEquals(1, state.frozenAssignments[5L])
        assertEquals(1, state.frozenAssignments[6L])
        assertEquals(2, state.frozenAssignments[7L])
        assertEquals(2, state.frozenAssignments[8L])
    }

    @Test
    fun `reset clears frozen state`() {
        val positions = (1L..4L).associateWith { TouchPoint(it.toFloat(), it.toFloat()) }
        viewModel.completeSelection(positions)
        assertTrue(viewModel.uiState.value.isFrozen)

        viewModel.reset()
        val state = viewModel.uiState.value
        assertFalse(state.isFrozen)
        assertTrue(state.frozenAssignments.isEmpty())
        assertTrue(state.frozenPositions.isEmpty())
        assertTrue(state.teamSizes.isEmpty())
    }

    @Test
    fun `setShowInfoDialog updates dialog state`() {
        viewModel.setShowInfoDialog(true)
        assertTrue(viewModel.uiState.value.showInfoDialog)

        viewModel.setShowInfoDialog(false)
        assertFalse(viewModel.uiState.value.showInfoDialog)
    }
}
