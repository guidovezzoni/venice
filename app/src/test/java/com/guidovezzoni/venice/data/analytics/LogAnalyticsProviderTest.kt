package com.guidovezzoni.venice.data.analytics

import android.util.Log
import com.guidovezzoni.venice.domain.analytics.AnalyticsEvent
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LogAnalyticsProviderTest {

    private lateinit var provider: LogAnalyticsProvider

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        provider = LogAnalyticsProvider()
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `GIVEN any event WHEN shouldTrack is called THEN returns true`() {
        val event = AnalyticsEvent.TripCreated(tripId = "trip-1")

        val result = provider.shouldTrack(event)

        assertTrue(result)
    }

    @Test
    fun `GIVEN an event WHEN track is called THEN Log d is called with correct tag and message`() {
        val event = AnalyticsEvent.TripCreated(tripId = "trip-1")

        provider.track(event)

        val expectedTag = "Analytics"
        val expectedMessage = "Event: trip_created | Properties: {trip_id=trip-1}"
        verify(exactly = 1) { Log.d(expectedTag, expectedMessage) }
    }

    @Test
    fun `GIVEN an OperationFailed event WHEN track is called THEN Log d includes operation and error`() {
        val event = AnalyticsEvent.OperationFailed(operation = "create_trip", errorMessage = "Network error")

        provider.track(event)

        val expectedTag = "Analytics"
        val expectedMessage = "Event: operation_failed | Properties: {operation=create_trip, error_message=Network error}"
        verify(exactly = 1) { Log.d(expectedTag, expectedMessage) }
    }
}
