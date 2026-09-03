package io.github.nicohinze.trainingtracker

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatDurationTest {
    @Test
    fun zeroSeconds() {
        assertEquals("00:00", formatDuration(0))
    }

    @Test
    fun secondsOnly() {
        assertEquals("00:45", formatDuration(45))
    }

    @Test
    fun minutesAndSeconds() {
        assertEquals("05:30", formatDuration(330))
    }

    @Test
    fun exactMinutes() {
        assertEquals("10:00", formatDuration(600))
    }

    @Test
    fun justUnderOneHour() {
        assertEquals("59:59", formatDuration(3599))
    }

    @Test
    fun exactlyOneHour() {
        assertEquals("01:00:00", formatDuration(3600))
    }

    @Test
    fun hoursMinutesSeconds() {
        assertEquals("01:30:45", formatDuration(5445))
    }

    @Test
    fun multipleHours() {
        assertEquals("02:15:30", formatDuration(8130))
    }
}
