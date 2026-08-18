package com.guidovezzoni.venice.core.analytics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsLogFormatterTest {

    private val event = AnalyticsEvent.TripCreated(tripId = "trip-1")
    private val userProperty = AnalyticsUserProperty.ForTesting
    private val operation = AnalyticsOperation.CREATE_TRIP
    private val throwable = RuntimeException("something went wrong")

    // Section 6 — event message formatting

    @Test
    fun `GIVEN an AnalyticsEvent with a name and properties WHEN formatEventLog is called THEN the returned string identifies it as an event and includes the event name and properties`() {
        val result = formatEventLog(event)

        assertTrue("Result should identify as an event: $result", result.contains("Event", ignoreCase = true))
        assertTrue("Result should contain event name: $result", result.contains(event.name))
        assertTrue("Result should contain event properties: $result", result.contains(event.properties.toString()))
    }

    // Section 7 — user property message formatting

    @Test
    fun `GIVEN an AnalyticsUserProperty WHEN formatUserPropertyLog is called THEN the returned string identifies it as a user property and is visually distinguishable from the event-format string`() {
        val eventResult = formatEventLog(event)
        val userPropertyResult = formatUserPropertyLog(userProperty)

        assertTrue("Result should identify as a user property: $userPropertyResult", userPropertyResult.contains("UserProperty", ignoreCase = true))
        assertFalse(
            "User property format should be visually distinguishable from event format: $userPropertyResult",
            userPropertyResult.startsWith(eventResult.substringBefore(":")),
        )
    }

    // Section 8 — exception message formatting

    @Test
    fun `GIVEN a Throwable and an AnalyticsOperation WHEN formatExceptionLog is called THEN the returned string identifies it as an exception includes the operation and is visually distinguishable from the event and user property format strings`() {
        val eventResult = formatEventLog(event)
        val userPropertyResult = formatUserPropertyLog(userProperty)
        val exceptionResult = formatExceptionLog(throwable, operation)

        assertTrue("Result should identify as an exception: $exceptionResult", exceptionResult.contains("Exception", ignoreCase = true))
        assertTrue("Result should contain the operation value: $exceptionResult", exceptionResult.contains(operation.value))
        assertFalse(
            "Exception format should be visually distinguishable from event format: $exceptionResult",
            exceptionResult.startsWith(eventResult.substringBefore(":")),
        )
        assertFalse(
            "Exception format should be visually distinguishable from user property format: $exceptionResult",
            exceptionResult.startsWith(userPropertyResult.substringBefore(":")),
        )
    }
}
