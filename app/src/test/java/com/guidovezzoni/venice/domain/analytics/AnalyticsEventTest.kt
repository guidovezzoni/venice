package com.guidovezzoni.venice.domain.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsEventTest {

    @Test
    fun `GIVEN a trip id WHEN TripCreated is constructed THEN name and properties are correct`() {
        val event = AnalyticsEvent.TripCreated(tripId = "trip-1")

        val expectedName = "trip_created"
        val expectedProperties = mapOf("trip_id" to "trip-1")
        assertEquals(expectedName, event.name)
        assertEquals(expectedProperties, event.properties)
    }

    @Test
    fun `GIVEN a trip id WHEN TripOpened is constructed THEN name and properties are correct`() {
        val event = AnalyticsEvent.TripOpened(tripId = "trip-1")

        val expectedName = "trip_opened"
        val expectedProperties = mapOf("trip_id" to "trip-1")
        assertEquals(expectedName, event.name)
        assertEquals(expectedProperties, event.properties)
    }

    @Test
    fun `GIVEN a trip id and stop type WHEN StopSet is constructed THEN name and properties are correct`() {
        val event = AnalyticsEvent.StopSet(tripId = "trip-1", stopType = "STARTING_POINT")

        val expectedName = "stop_set"
        val expectedProperties = mapOf("trip_id" to "trip-1", "stop_type" to "STARTING_POINT")
        assertEquals(expectedName, event.name)
        assertEquals(expectedProperties, event.properties)
    }

    @Test
    fun `GIVEN a trip id WHEN StopEdited is constructed THEN name and properties are correct`() {
        val event = AnalyticsEvent.StopEdited(tripId = "trip-1")

        val expectedName = "stop_edited"
        val expectedProperties = mapOf("trip_id" to "trip-1")
        assertEquals(expectedName, event.name)
        assertEquals(expectedProperties, event.properties)
    }

    @Test
    fun `GIVEN a trip id WHEN StopRemoved is constructed THEN name and properties are correct`() {
        val event = AnalyticsEvent.StopRemoved(tripId = "trip-1")

        val expectedName = "stop_removed"
        val expectedProperties = mapOf("trip_id" to "trip-1")
        assertEquals(expectedName, event.name)
        assertEquals(expectedProperties, event.properties)
    }

    @Test
    fun `GIVEN a trip id WHEN StopReordered is constructed THEN name and properties are correct`() {
        val event = AnalyticsEvent.StopReordered(tripId = "trip-1")

        val expectedName = "stop_reordered"
        val expectedProperties = mapOf("trip_id" to "trip-1")
        assertEquals(expectedName, event.name)
        assertEquals(expectedProperties, event.properties)
    }

    @Test
    fun `GIVEN a trip id and stop id WHEN StopDeparted is constructed THEN name and properties are correct`() {
        val event = AnalyticsEvent.StopDeparted(tripId = "trip-1", stopId = "stop-1")

        val expectedName = "stop_departed"
        val expectedProperties = mapOf("trip_id" to "trip-1", "stop_id" to "stop-1")
        assertEquals(expectedName, event.name)
        assertEquals(expectedProperties, event.properties)
    }

    @Test
    fun `GIVEN a trip id WHEN RouteCalculated is constructed THEN name and properties are correct`() {
        val event = AnalyticsEvent.RouteCalculated(tripId = "trip-1")

        val expectedName = "route_calculated"
        val expectedProperties = mapOf("trip_id" to "trip-1")
        assertEquals(expectedName, event.name)
        assertEquals(expectedProperties, event.properties)
    }

    @Test
    fun `GIVEN a screen name WHEN ScreenViewed is constructed THEN name and properties are correct`() {
        val event = AnalyticsEvent.ScreenViewed(screenName = "trip_list")

        val expectedName = "screen_viewed"
        val expectedProperties = mapOf("screen_name" to "trip_list")
        assertEquals(expectedName, event.name)
        assertEquals(expectedProperties, event.properties)
    }

    @Test
    fun `GIVEN an operation and error WHEN OperationFailed is constructed THEN name and properties are correct`() {
        val event = AnalyticsEvent.OperationFailed(operation = "create_trip", errorMessage = "Network error")

        val expectedName = "operation_failed"
        val expectedProperties = mapOf("operation" to "create_trip", "error_message" to "Network error")
        assertEquals(expectedName, event.name)
        assertEquals(expectedProperties, event.properties)
    }
}
