package com.joasasso.minitoolbox.tools.herramientas.generadores

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupPartitionEngineTest {

    @Test
    fun `calculateTeamSizes - insufficient participants returns emptyList`() {
        // Less than K in BY_TEAM_COUNT
        assertEquals(emptyList<Int>(), GroupPartitionEngine.calculateTeamSizes(2, PartitionMode.BY_TEAM_COUNT, 3))
        // Less than S in BY_TEAM_SIZE
        assertEquals(emptyList<Int>(), GroupPartitionEngine.calculateTeamSizes(2, PartitionMode.BY_TEAM_SIZE, 3))
        // Non-positive target
        assertEquals(emptyList<Int>(), GroupPartitionEngine.calculateTeamSizes(8, PartitionMode.BY_TEAM_COUNT, 0))
    }

    @Test
    fun `calculateTeamSizes - BY_TEAM_COUNT divides evenly`() {
        val sizes = GroupPartitionEngine.calculateTeamSizes(6, PartitionMode.BY_TEAM_COUNT, 2)
        assertEquals(listOf(3, 3), sizes)
    }

    @Test
    fun `calculateTeamSizes - BY_TEAM_COUNT divides unevenly with max difference of 1`() {
        // 8 people in 3 teams -> 3, 3, 2
        val sizes8in3 = GroupPartitionEngine.calculateTeamSizes(8, PartitionMode.BY_TEAM_COUNT, 3)
        assertEquals(listOf(3, 3, 2), sizes8in3)

        // 7 people in 2 teams -> 4, 3
        val sizes7in2 = GroupPartitionEngine.calculateTeamSizes(7, PartitionMode.BY_TEAM_COUNT, 2)
        assertEquals(listOf(4, 3), sizes7in2)

        // 10 people in 3 teams -> 4, 3, 3
        val sizes10in3 = GroupPartitionEngine.calculateTeamSizes(10, PartitionMode.BY_TEAM_COUNT, 3)
        assertEquals(listOf(4, 3, 3), sizes10in3)
    }

    @Test
    fun `calculateTeamSizes - BY_TEAM_SIZE creates full groups plus remainder group`() {
        // 8 people, size 3 -> 2 teams of 3 and 1 of 2 (User's explicit example)
        val sizes8size3 = GroupPartitionEngine.calculateTeamSizes(8, PartitionMode.BY_TEAM_SIZE, 3)
        assertEquals(listOf(3, 3, 2), sizes8size3)

        // 9 people, size 3 -> 3 teams of 3
        val sizes9size3 = GroupPartitionEngine.calculateTeamSizes(9, PartitionMode.BY_TEAM_SIZE, 3)
        assertEquals(listOf(3, 3, 3), sizes9size3)

        // 7 people, size 3 -> 2 teams of 3 and 1 of 1
        val sizes7size3 = GroupPartitionEngine.calculateTeamSizes(7, PartitionMode.BY_TEAM_SIZE, 3)
        assertEquals(listOf(3, 3, 1), sizes7size3)

        // 5 people, size 3 -> 1 team of 3 and 1 of 2
        val sizes5size3 = GroupPartitionEngine.calculateTeamSizes(5, PartitionMode.BY_TEAM_SIZE, 3)
        assertEquals(listOf(3, 2), sizes5size3)
    }

    @Test
    fun `summarizeTeamSizes - groups correctly`() {
        val summary1 = GroupPartitionEngine.summarizeTeamSizes(listOf(3, 3, 2))
        assertEquals(
            listOf(
                TeamGroupSummary(teamCount = 2, memberCount = 3),
                TeamGroupSummary(teamCount = 1, memberCount = 2)
            ),
            summary1
        )

        val summary2 = GroupPartitionEngine.summarizeTeamSizes(listOf(4, 4))
        assertEquals(
            listOf(TeamGroupSummary(teamCount = 2, memberCount = 4)),
            summary2
        )

        val summaryEmpty = GroupPartitionEngine.summarizeTeamSizes(emptyList())
        assertEquals(emptyList<TeamGroupSummary>(), summaryEmpty)
    }

    @Test
    fun `partition - assigns all items deterministically with custom shuffler`() {
        val items = listOf("A", "B", "C", "D", "E", "F", "G", "H")
        // Shuffler that preserves order
        val result = GroupPartitionEngine.partition(
            items = items,
            mode = PartitionMode.BY_TEAM_COUNT,
            targetValue = 3,
            shuffler = { it }
        )

        assertTrue(result is PartitionResult.Success)
        val success = result as PartitionResult.Success
        assertEquals(listOf(3, 3, 2), success.teamSizes)
        assertEquals(8, success.assignments.size)

        // Team 0: A, B, C
        assertEquals(0, success.assignments["A"])
        assertEquals(0, success.assignments["B"])
        assertEquals(0, success.assignments["C"])
        // Team 1: D, E, F
        assertEquals(1, success.assignments["D"])
        assertEquals(1, success.assignments["E"])
        assertEquals(1, success.assignments["F"])
        // Team 2: G, H
        assertEquals(2, success.assignments["G"])
        assertEquals(2, success.assignments["H"])
    }

    @Test
    fun `partition - returns InsufficientParticipants when count is too low`() {
        val items = listOf("A", "B")
        val result = GroupPartitionEngine.partition(
            items = items,
            mode = PartitionMode.BY_TEAM_COUNT,
            targetValue = 3
        )

        assertTrue(result is PartitionResult.InsufficientParticipants)
        assertEquals(3, (result as PartitionResult.InsufficientParticipants).minRequired)
    }
}
