package com.joasasso.minitoolbox.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenSerializationTest {

    @Test
    fun `roundtrip serialization for data objects`() {
        val testScreens = listOf(
            Screen.Categories,
            Screen.Water,
            Screen.WaterStats,
            Screen.PomodoroList,
            Screen.BasicPhrases,
            Screen.Meetings,
            Screen.MeetingCreate,
            Screen.BubbleLevel,
            Screen.ArRuler,
            Screen.Pro,
            Screen.DevMetrics
        )

        for (screen in testScreens) {
            val json = Screen.toJson(screen)
            assertNotNull(json)
            val deserialized = Screen.fromJson(json)
            assertEquals("Failed roundtrip for ${screen::class.simpleName}", screen, deserialized)
        }
    }

    @Test
    fun `roundtrip serialization for data classes with arguments`() {
        val pomodoro = Screen.PomodoroDetail("abc-123-xyz")
        val meetingDetail = Screen.MeetingDetail("meeting-456")
        val expenseAdd = Screen.ExpenseAdd("meeting-456")
        val expenseEdit = Screen.ExpenseEdit("meeting-456", "expense-789")

        assertEquals(pomodoro, Screen.fromJson(Screen.toJson(pomodoro)))
        assertEquals(meetingDetail, Screen.fromJson(Screen.toJson(meetingDetail)))
        assertEquals(expenseAdd, Screen.fromJson(Screen.toJson(expenseAdd)))
        assertEquals(expenseEdit, Screen.fromJson(Screen.toJson(expenseEdit)))
    }

    @Test
    fun `fromJson returns null for invalid or null inputs`() {
        assertNull(Screen.fromJson(null))
        assertNull(Screen.fromJson(""))
        assertNull(Screen.fromJson("   "))
        assertNull(Screen.fromJson("{ \"type\": \"unknown_screen_type_xyz\" }"))
        assertNull(Screen.fromJson("not a json"))
    }

    @Test
    fun `fromRouteString handles legacy route strings and aliases`() {
        assertEquals(Screen.Categories, Screen.fromRouteString("categories"))
        assertEquals(Screen.Water, Screen.fromRouteString("water"))
        assertEquals(Screen.WaterStats, Screen.fromRouteString("water_stats"))
        assertEquals(Screen.PomodoroList, Screen.fromRouteString("pomodoro_list"))
        assertEquals(Screen.PomodoroList, Screen.fromRouteString("pomodoro"))
        assertEquals(Screen.BasicPhrases, Screen.fromRouteString("basic_phrases"))
        assertEquals(Screen.BasicPhrases, Screen.fromRouteString("quotes"))
        assertEquals(Screen.PomodoroDetail("uuid-1"), Screen.fromRouteString("pomodoro/detail/uuid-1"))
        assertEquals(Screen.PomodoroDetail("uuid-2"), Screen.fromRouteString("pomodoro_detail/uuid-2"))
        assertEquals(Screen.MeetingDetail("m-123"), Screen.fromRouteString("meeting_detail/m-123"))
        assertEquals(Screen.ExpenseAdd("m-123"), Screen.fromRouteString("expense_add/m-123"))
        assertEquals(Screen.ExpenseEdit("m-123", "e-456"), Screen.fromRouteString("expense_edit/m-123/e-456"))
    }

    @Test
    fun `fromRouteString ignores query strings and fragments`() {
        assertEquals(Screen.Water, Screen.fromRouteString("water?from=widget"))
        assertEquals(Screen.Water, Screen.fromRouteString("water#section"))
        assertEquals(Screen.PomodoroDetail("uuid-1"), Screen.fromRouteString("pomodoro/detail/uuid-1?source=notification"))
    }

    @Test
    fun `isValidRoute validates known routes`() {
        assertTrue(Screen.isValidRoute("water"))
        assertTrue(Screen.isValidRoute("pomodoro/detail/123"))
        assertTrue(Screen.isValidRoute("quotes"))
        assertTrue(Screen.isValidRoute("basic_phrases"))
        assertFalse(Screen.isValidRoute("invalid_route_xyz"))
        assertFalse(Screen.isValidRoute(null))
        assertFalse(Screen.isValidRoute(""))
    }
}
