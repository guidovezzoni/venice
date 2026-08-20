package com.guidovezzoni.venice.core.analytics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsLogFormatterTest {

    private val event = AnalyticsEvent.TripCreated(isFirstTrip = true)
    private val userProperty = AnalyticsUserProperty.TripCountBand(band = CountBand.RANGE_2_5)
    private val operation = AnalyticsOperation.CREATE_TRIP

    @Test
    fun `GIVEN an AnalyticsEvent WHEN formatEventLog is called THEN result identifies as an event and includes name and properties`() {
        val result = formatEventLog(event)

        assertTrue("Result should identify as an event: $result", result.contains("Event", ignoreCase = true))
        assertTrue("Result should contain event name: $result", result.contains(event.name))
        assertTrue("Result should contain event properties: $result", result.contains(event.properties.toString()))
    }

    @Test
    fun `GIVEN an AnalyticsUserProperty WHEN formatUserPropertyLog is called THEN result identifies as a user property and is distinguishable from event format`() {
        val eventResult = formatEventLog(event)
        val userPropertyResult = formatUserPropertyLog(userProperty)

        assertTrue("Result should identify as a user property: $userPropertyResult", userPropertyResult.contains("UserProperty", ignoreCase = true))
        assertFalse(
            "User property format should be visually distinguishable from event format: $userPropertyResult",
            userPropertyResult.startsWith(eventResult.substringBefore(":")),
        )
    }

    @Test
    fun `GIVEN a Throwable and an AnalyticsOperation WHEN formatExceptionLog is called THEN result identifies as an exception and is distinguishable from other formats`() {
        val eventResult = formatEventLog(event)
        val userPropertyResult = formatUserPropertyLog(userProperty)
        val exceptionResult = formatExceptionLog(operation)

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

    @Test
    fun `GIVEN a ScreenViewed event WHEN formatEventLog is called THEN result contains event name and screen data`() {
        val newEvent = AnalyticsEvent.ScreenViewed(screen = AnalyticsScreen.TRIP_DETAIL)

        val result = formatEventLog(newEvent)

        assertTrue("Result should identify as an event: $result", result.contains("Event", ignoreCase = true))
        assertTrue("Result should contain event name 'screen_viewed': $result", result.contains("screen_viewed"))
        assertTrue("Result should contain screen_name property: $result", result.contains("screen_name"))
        assertTrue("Result should contain screen value 'trip_detail': $result", result.contains("trip_detail"))
    }

    @Test
    fun `GIVEN an OperationFailed event WHEN formatEventLog is called THEN result contains operation and error type values`() {
        val newEvent = AnalyticsEvent.OperationFailed(
            operation = AnalyticsOperation.CALCULATE_ROUTE,
            errorType = AnalyticsErrorType.NETWORK,
        )

        val result = formatEventLog(newEvent)

        assertTrue("Result should contain 'operation_failed': $result", result.contains("operation_failed"))
        assertTrue("Result should contain 'calculate_route': $result", result.contains("calculate_route"))
        assertTrue("Result should contain 'network': $result", result.contains("network"))
    }

    @Test
    fun `GIVEN a TripCountBand user property WHEN formatUserPropertyLog is called THEN result contains property data`() {
        val newProperty = AnalyticsUserProperty.TripCountBand(band = CountBand.SIX_PLUS)

        val result = formatUserPropertyLog(newProperty)

        assertTrue("Result should identify as a user property: $result", result.contains("UserProperty", ignoreCase = true))
        assertTrue("Result should contain property data: $result", result.contains("SIX_PLUS") || result.contains("6_plus") || result.contains("TripCountBand"))
    }

    @Test
    fun `GIVEN a DistanceUnit user property WHEN formatUserPropertyLog is called THEN result contains property data`() {
        val newProperty = AnalyticsUserProperty.DistanceUnit(unit = DistanceUnitParam.METRIC)

        val result = formatUserPropertyLog(newProperty)

        assertTrue("Result should identify as a user property: $result", result.contains("UserProperty", ignoreCase = true))
        assertTrue("Result should contain property data: $result", result.contains("metric") || result.contains("DistanceUnit"))
    }

    @Test
    fun `GIVEN a new event and a new user property WHEN both are formatted THEN each produces a distinguishable log line`() {
        val newEvent = AnalyticsEvent.RouteCalculated(
            stopCount = 3,
            legCount = 2,
            distanceBand = DistanceBand.RANGE_50_200KM,
            durationBand = DurationBand.RANGE_1_3H,
        )
        val newProperty = AnalyticsUserProperty.DistanceUnit(unit = DistanceUnitParam.IMPERIAL)

        val eventResult = formatEventLog(newEvent)
        val propertyResult = formatUserPropertyLog(newProperty)

        assertTrue("Event log should identify as an event: $eventResult", eventResult.contains("Event", ignoreCase = true))
        assertTrue("Property log should identify as a user property: $propertyResult", propertyResult.contains("UserProperty", ignoreCase = true))
        assertFalse(
            "Event and property log lines should be distinguishable",
            eventResult == propertyResult,
        )
    }
}
