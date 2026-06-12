package com.gregoryhpotter.textlistscanner.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FrameThrottleTest {

    private var fakeTime = 0L
    private lateinit var throttle: FrameThrottle

    @Before
    fun setUp() {
        fakeTime = 0L
        throttle = FrameThrottle(thresholdMs = 300L, clock = { fakeTime })
    }

    // -------------------------------------------------------------------------
    // First frame
    // -------------------------------------------------------------------------

    @Test
    fun `first call is always processed`() {
        assertTrue(throttle.shouldProcess())
    }

    // -------------------------------------------------------------------------
    // Within threshold — skip
    // -------------------------------------------------------------------------

    @Test
    fun `call immediately after first is skipped`() {
        throttle.shouldProcess()
        assertFalse(throttle.shouldProcess())
    }

    @Test
    fun `call within threshold is skipped`() {
        throttle.shouldProcess()
        fakeTime = 299L
        assertFalse(throttle.shouldProcess())
    }

    // -------------------------------------------------------------------------
    // At or past threshold — process
    // -------------------------------------------------------------------------

    @Test
    fun `call exactly at threshold is processed`() {
        throttle.shouldProcess()
        fakeTime = 300L
        assertTrue(throttle.shouldProcess())
    }

    @Test
    fun `call past threshold is processed`() {
        throttle.shouldProcess()
        fakeTime = 500L
        assertTrue(throttle.shouldProcess())
    }

    // -------------------------------------------------------------------------
    // Timer resets correctly
    // -------------------------------------------------------------------------

    @Test
    fun `threshold resets from last processed frame not from first`() {
        throttle.shouldProcess()          // t=0, processed
        fakeTime = 400L
        throttle.shouldProcess()          // t=400, processed — resets timer
        fakeTime = 600L                   // only 200ms since last processed frame
        assertFalse(throttle.shouldProcess())
    }

    @Test
    fun `skipped frames do not reset the timer`() {
        throttle.shouldProcess()          // t=0, processed
        fakeTime = 100L
        throttle.shouldProcess()          // t=100, skipped — must not update timer
        fakeTime = 250L
        throttle.shouldProcess()          // t=250, skipped
        fakeTime = 300L                   // 300ms since t=0, should now process
        assertTrue(throttle.shouldProcess())
    }

    @Test
    fun `two consecutive processed frames each reset the timer independently`() {
        throttle.shouldProcess()          // t=0, processed
        fakeTime = 300L
        throttle.shouldProcess()          // t=300, processed
        fakeTime = 500L                   // only 200ms since second processed frame
        assertFalse(throttle.shouldProcess())
        fakeTime = 600L                   // 300ms since second processed frame
        assertTrue(throttle.shouldProcess())
    }

    // -------------------------------------------------------------------------
    // Default threshold
    // -------------------------------------------------------------------------

    @Test
    fun `default threshold is 300ms`() {
        assertEquals(300L, FrameThrottle.DEFAULT_THRESHOLD_MS)
    }

    @Test
    fun `default constructor uses 300ms threshold`() {
        val defaultThrottle = FrameThrottle(clock = { fakeTime })
        defaultThrottle.shouldProcess()   // t=0
        fakeTime = 299L
        assertFalse(defaultThrottle.shouldProcess())
        fakeTime = 300L
        assertTrue(defaultThrottle.shouldProcess())
    }
}
