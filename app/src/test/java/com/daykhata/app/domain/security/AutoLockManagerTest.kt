package com.daykhata.app.domain.security

import com.daykhata.app.domain.model.AutoLockTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoLockManagerTest {

    @Test
    fun `auto lock triggers after configured timeout duration`() {
        var currentTime = 1000000L
        val manager = AutoLockManager(timeProvider = { currentTime })

        // Check initially
        assertFalse(manager.isTimedOut(AutoLockTimeout.SECONDS_30))

        // Advance 29 seconds
        currentTime += 29000L
        assertFalse(manager.isTimedOut(AutoLockTimeout.SECONDS_30))

        // Advance 1 more second (total 30 seconds)
        currentTime += 1000L
        assertTrue(manager.isTimedOut(AutoLockTimeout.SECONDS_30))
    }

    @Test
    fun `user interaction resets inactivity timer`() {
        var currentTime = 1000000L
        val manager = AutoLockManager(timeProvider = { currentTime })

        // Advance 25 seconds
        currentTime += 25000L
        assertFalse(manager.isTimedOut(AutoLockTimeout.SECONDS_30))

        // User interacts
        manager.recordInteraction()

        // Advance 10 more seconds (elapsed since interaction is only 10s)
        currentTime += 10000L
        assertFalse(manager.isTimedOut(AutoLockTimeout.SECONDS_30))

        // Advance 20 more seconds (total 30s since last interaction)
        currentTime += 20000L
        assertTrue(manager.isTimedOut(AutoLockTimeout.SECONDS_30))
    }

    @Test
    fun `immediately timeout returns true on idle check`() {
        val manager = AutoLockManager()
        assertTrue(manager.isTimedOut(AutoLockTimeout.IMMEDIATELY))
    }

    @Test
    fun `different timeout options behave correctly`() {
        var currentTime = 1000000L
        val manager = AutoLockManager(timeProvider = { currentTime })

        // Advance 45 seconds
        currentTime += 45000L
        assertTrue(manager.isTimedOut(AutoLockTimeout.SECONDS_30))
        assertFalse(manager.isTimedOut(AutoLockTimeout.MINUTE_1))
        assertFalse(manager.isTimedOut(AutoLockTimeout.MINUTES_5))

        // Advance another 20 seconds (total 65s)
        currentTime += 20000L
        assertTrue(manager.isTimedOut(AutoLockTimeout.MINUTE_1))
        assertFalse(manager.isTimedOut(AutoLockTimeout.MINUTES_5))

        // Advance another 240 seconds (total 305s > 5 min)
        currentTime += 240000L
        assertTrue(manager.isTimedOut(AutoLockTimeout.MINUTES_5))
    }
}
