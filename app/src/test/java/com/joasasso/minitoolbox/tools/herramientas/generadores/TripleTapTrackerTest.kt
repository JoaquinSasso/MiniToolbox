package com.joasasso.minitoolbox.tools.herramientas.generadores

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TripleTapTrackerTest {

    private lateinit var tracker: TripleTapTracker

    @Before
    fun setUp() {
        tracker = TripleTapTracker(maxIntervalMs = 900L, minSeparationMs = 60L, requiredTaps = 3)
    }

    @Test
    fun `single and double taps return false`() {
        assertFalse(tracker.registerTap(100L))
        assertFalse(tracker.registerTap(300L))
    }

    @Test
    fun `three consecutive taps within interval return true`() {
        assertFalse(tracker.registerTap(100L))
        assertFalse(tracker.registerTap(300L))
        assertTrue(tracker.registerTap(500L))
    }

    @Test
    fun `taps spread beyond max interval expire and do not trigger`() {
        assertFalse(tracker.registerTap(100L))
        // Next tap is 1100ms later (> 900ms window)
        assertFalse(tracker.registerTap(1200L))
        // Third tap is 2300ms later
        assertFalse(tracker.registerTap(2300L))
    }

    @Test
    fun `simultaneous touches under min separation are ignored`() {
        assertFalse(tracker.registerTap(100L))
        // Simultaneous touch at 120ms (< 60ms separation)
        assertFalse(tracker.registerTap(120L))
        // Another simultaneous touch at 130ms
        assertFalse(tracker.registerTap(130L))

        // Legitimate second tap at 300ms
        assertFalse(tracker.registerTap(300L))
        // Legitimate third tap at 500ms
        assertTrue(tracker.registerTap(500L))
    }

    @Test
    fun `reset clears accumulated taps`() {
        assertFalse(tracker.registerTap(100L))
        assertFalse(tracker.registerTap(300L))
        tracker.reset()
        // Would be 3rd tap, but after reset it is 1st tap
        assertFalse(tracker.registerTap(500L))
    }

    @Test
    fun `after triggering triple tap, state resets and fourth tap does not trigger`() {
        assertFalse(tracker.registerTap(100L))
        assertFalse(tracker.registerTap(300L))
        assertTrue(tracker.registerTap(500L))

        // Fourth tap should not trigger
        assertFalse(tracker.registerTap(700L))
    }
}
