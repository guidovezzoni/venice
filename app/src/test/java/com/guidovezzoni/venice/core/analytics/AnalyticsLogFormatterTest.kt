package com.guidovezzoni.venice.core.analytics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsLogFormatterTest {

    private val event = AnalyticsEvent.TripCreated(isFirstTrip = true)
    private val userProperty = AnalyticsUserProperty.TripCountBand(band = CountBand.RANGE_2_5)
    private val operation = AnalyticsOperation.CREATE_TRIP

    // Section 6 — event message formatting

    @Test
    fun givenAnalyticsEventWithNameAndProperties_whenFormatEventLogIsCalled_thenResultIdentifiesAsEventAndIncludesNameAndProperties() {
        val result = formatEventLog(event)

        assertTrue("Result should identify as an event: $result", result.contains("Event", ignoreCase = true))
        assertTrue("Result should contain event name: $result", result.contains(event.name))
        assertTrue("Result should contain event properties: $result", result.contains(event.properties.toString()))
    }

    // Section 7 — user property message formatting

    @Test
    fun givenAnalyticsUserProperty_whenFormatUserPropertyLogIsCalled_thenResultIdentifiesAsUserPropertyAndIsDistinguishableFromEventFormat() {
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
    fun givenThrowableAndAnalyticsOperation_whenFormatExceptionLogIsCalled_thenResultIdentifiesAsExceptionAndIsDistinguishableFromOtherFormats() {
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

    // Section 8.1 — regression guard: new event subtypes and new user property subtypes

    @Test
    fun givenNewEventSubtypeScreenViewed_whenFormatEventLogIsCalled_thenResultContainsEventNameAndScreenData() {
        val newEvent = AnalyticsEvent.ScreenViewed(screen = AnalyticsScreen.TRIP_DETAIL)

        val result = formatEventLog(newEvent)

        assertTrue("Result should identify as an event: $result", result.contains("Event", ignoreCase = true))
        assertTrue("Result should contain event name 'screen_viewed': $result", result.contains("screen_viewed"))
        assertTrue("Result should contain screen_name property: $result", result.contains("screen_name"))
        assertTrue("Result should contain screen value 'trip_detail': $result", result.contains("trip_detail"))
    }

    @Test
    fun givenNewEventSubtypeOperationFailed_whenFormatEventLogIsCalled_thenResultContainsOperationAndErrorTypeValues() {
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
    fun givenNewUserPropertyTripCountBand_whenFormatUserPropertyLogIsCalled_thenResultContainsPropertyData() {
        val newProperty = AnalyticsUserProperty.TripCountBand(band = CountBand.SIX_PLUS)

        val result = formatUserPropertyLog(newProperty)

        assertTrue("Result should identify as a user property: $result", result.contains("UserProperty", ignoreCase = true))
        assertTrue("Result should contain property data: $result", result.contains("SIX_PLUS") || result.contains("6_plus") || result.contains("TripCountBand"))
    }

    @Test
    fun givenNewUserPropertyDistanceUnit_whenFormatUserPropertyLogIsCalled_thenResultContainsPropertyData() {
        val newProperty = AnalyticsUserProperty.DistanceUnit(unit = "metric")

        val result = formatUserPropertyLog(newProperty)

        assertTrue("Result should identify as a user property: $result", result.contains("UserProperty", ignoreCase = true))
        assertTrue("Result should contain property data: $result", result.contains("metric") || result.contains("DistanceUnit"))
    }

    @Test
    fun givenNewEventAndNewUserProperty_whenFormatted_thenEachProducesDistinguishableLogLine() {
        val newEvent = AnalyticsEvent.RouteCalculated(
            stopCount = 3,
            legCount = 2,
            distanceBand = DistanceBand.RANGE_50_200KM,
            durationBand = DurationBand.RANGE_1_3H,
        )
        val newProperty = AnalyticsUserProperty.DistanceUnit(unit = "imperial")

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
