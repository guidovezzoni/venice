package com.guidovezzoni.venice.ui.util

import android.content.res.Resources
import com.guidovezzoni.venice.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

private const val MINUTES_PATTERN = "%1\$d min"
private const val HOURS_MINUTES_PATTERN = "%1\$dh %2\$dmin"

class DurationFormatterTest {

    private lateinit var resources: Resources

    @Before
    fun setUp() {
        resources = mockk()
        every { resources.getString(R.string.trip_detail_leg_duration_minutes, any()) } answers {
            val formatArgument = secondArg<Array<Any?>>().first()
            String.format(Locale.US, MINUTES_PATTERN, formatArgument)
        }
        every { resources.getString(R.string.trip_detail_leg_duration_hours_minutes, any(), any()) } answers {
            val args = secondArg<Array<Any?>>()
            String.format(Locale.US, HOURS_MINUTES_PATTERN, args[0], args[1])
        }
    }

    @Test
    fun `GIVEN durationSeconds of 0 WHEN formatDuration is called THEN it returns 0 min`() {
        val durationSeconds = 0

        val result = formatDuration(durationSeconds, resources)

        val expected = "0 min"
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN durationSeconds of 45 WHEN formatDuration is called THEN it returns 0 min`() {
        val durationSeconds = 45

        val result = formatDuration(durationSeconds, resources)

        val expected = "0 min"
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN durationSeconds of 89 WHEN formatDuration is called THEN it returns 1 min (truncation not rounding)`() {
        val durationSeconds = 89

        val result = formatDuration(durationSeconds, resources)

        val expected = "1 min"
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN durationSeconds of 540 (nine minutes sub-hour) WHEN formatDuration is called THEN it returns 9 min`() {
        val durationSeconds = 540

        val result = formatDuration(durationSeconds, resources)

        val expected = "9 min"
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN durationSeconds of 3599 (fifty-nine minutes just below the hour boundary) WHEN formatDuration is called THEN it returns 59 min`() {
        val durationSeconds = 3599

        val result = formatDuration(durationSeconds, resources)

        val expected = "59 min"
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN durationSeconds of 3600 (exactly one hour) WHEN formatDuration is called THEN it returns 1h 0min`() {
        val durationSeconds = 3600

        val result = formatDuration(durationSeconds, resources)

        val expected = "1h 0min"
        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN durationSeconds of 5400 (one hour thirty minutes) WHEN formatDuration is called THEN it returns 1h 30min`() {
        val durationSeconds = 5400

        val result = formatDuration(durationSeconds, resources)

        val expected = "1h 30min"
        assertEquals(expected, result)
    }
}
