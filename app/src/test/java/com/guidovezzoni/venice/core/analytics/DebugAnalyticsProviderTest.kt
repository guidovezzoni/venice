package com.guidovezzoni.venice.core.analytics

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class DebugAnalyticsProviderTest {

    private lateinit var provider: DebugAnalyticsProvider

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        provider = DebugAnalyticsProvider()
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `GIVEN an AnalyticsEvent WHEN logEvent is called THEN Log d is invoked with the tag and the string produced by formatEventLog`() {
        val event = AnalyticsEvent.TripCreated(isFirstTrip = true)
        val expectedTag = "Analytics"
        val expectedMessage = formatEventLog(event)

        provider.logEvent(event)

        verify(exactly = 1) { Log.d(expectedTag, expectedMessage) }
    }

    @Test
    fun `GIVEN an AnalyticsUserProperty WHEN setUserProperty is called THEN Log d is invoked with the tag and the string produced by formatUserPropertyLog`() {
        val property = AnalyticsUserProperty.DistanceUnit(unit = "metric")
        val expectedTag = "Analytics"
        val expectedMessage = formatUserPropertyLog(property)

        provider.setUserProperty(property)

        verify(exactly = 1) { Log.d(expectedTag, expectedMessage) }
    }

    @Test
    fun `GIVEN a Throwable and an AnalyticsOperation WHEN trackException is called THEN Log d is invoked with the tag and the string produced by formatExceptionLog`() {
        val throwable = RuntimeException("test error")
        val operation = AnalyticsOperation.CALCULATE_ROUTE
        val expectedTag = "Analytics"
        val expectedMessage = formatExceptionLog(throwable, operation)

        provider.trackException(throwable, operation)

        verify(exactly = 1) { Log.d(expectedTag, expectedMessage) }
    }
}
