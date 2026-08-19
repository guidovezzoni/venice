package com.guidovezzoni.venice.core.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AnalyticsEventTest {

    private fun assertNoScreamingSnakeCase(properties: Map<String, Any>) {
        val screamingSnakePattern = Regex("^[A-Z][A-Z0-9_]+$")
        properties.values
            .filterIsInstance<String>()
            .forEach { value ->
                assertFalse(
                    "Property value '$value' must not be SCREAMING_SNAKE_CASE (use .value not .name)",
                    screamingSnakePattern.matches(value),
                )
            }
    }

    @Test
    fun `GIVEN isFirstTrip true WHEN TripCreated is constructed THEN name and properties match the tracking plan`() {
        val event = AnalyticsEvent.TripCreated(isFirstTrip = true)

        val expectedName = "trip_created"
        val expectedProperties = mapOf("is_first_trip" to true)
        assertEquals(expectedName, event.name)
        assertEquals(expectedProperties, event.properties)
        assertNoScreamingSnakeCase(event.properties)
    }

    @Test
    fun `GIVEN isFirstTrip false WHEN TripCreated is constructed THEN name and properties match the tracking plan`() {
        val event = AnalyticsEvent.TripCreated(isFirstTrip = false)

        val expectedName = "trip_created"
        val expectedProperties = mapOf("is_first_trip" to false)
        assertEquals(expectedName, event.name)
        assertEquals(expectedProperties, event.properties)
    }

    @Test
    fun `GIVEN stop count and route state WHEN TripOpened is constructed THEN name and properties match the tracking plan`() {
        val event = AnalyticsEvent.TripOpened(stopCount = 3, routeState = "complete")

        val expectedName = "trip_opened"
        val expectedProperties = mapOf("stop_count" to 3, "route_state" to "complete")
        assertEquals(expectedName, event.name)
        assertEquals(expectedProperties, event.properties)
        assertNoScreamingSnakeCase(event.properties)
    }

    @Test
    fun `GIVEN stop type STARTING_POINT and stop count WHEN StopAdded is constructed THEN name and properties match the tracking plan`() {
        val event = AnalyticsEvent.StopAdded(stopType = StopTypeParam.STARTING_POINT, stopCount = 1)

        val expectedName = "stop_added"
        val expectedProperties = mapOf("stop_type" to "starting_point", "stop_count" to 1)
        assertEquals(expectedName, event.name)
        assertEquals(expectedProperties, event.properties)
        assertNoScreamingSnakeCase(event.properties)
    }

    @Test
    fun `GIVEN stop type DESTINATION WHEN StopAdded is constructed THEN stop_type value is snake_case`() {
        val event = AnalyticsEvent.StopAdded(stopType = StopTypeParam.DESTINATION, stopCount = 2)

        assertEquals("destination", event.properties["stop_type"])
        assertNoScreamingSnakeCase(event.properties)
    }

    @Test
    fun `GIVEN stop type INTERMEDIATE WHEN StopAdded is constructed THEN stop_type value is snake_case`() {
        val event = AnalyticsEvent.StopAdded(stopType = StopTypeParam.INTERMEDIATE, stopCount = 3)

        assertEquals("intermediate", event.properties["stop_type"])
        assertNoScreamingSnakeCase(event.properties)
    }

    @Test
    fun `GIVEN all params WHEN RouteCalculated is constructed THEN name and properties match the tracking plan`() {
        val event = AnalyticsEvent.RouteCalculated(
            stopCount = 4,
            legCount = 3,
            distanceBand = DistanceBand.RANGE_50_200KM,
            durationBand = DurationBand.RANGE_1_3H,
        )

        val expectedName = "route_calculated"
        val expectedProperties = mapOf(
            "stop_count" to 4,
            "leg_count" to 3,
            "distance_band" to "50_200km",
            "duration_band" to "1_3h",
        )
        assertEquals(expectedName, event.name)
        assertEquals(expectedProperties, event.properties)
        assertNoScreamingSnakeCase(event.properties)
    }

    @Test
    fun `GIVEN stop type and position WHEN NavigationLaunched is constructed THEN name and properties match the tracking plan`() {
        val event = AnalyticsEvent.NavigationLaunched(
            stopType = StopTypeParam.DESTINATION,
            stopPosition = 2,
        )

        val expectedName = "navigation_launched"
        val expectedProperties = mapOf("stop_type" to "destination", "stop_position" to 2)
        assertEquals(expectedName, event.name)
        assertEquals(expectedProperties, event.properties)
        assertNoScreamingSnakeCase(event.properties)
    }

    @Test
    fun `GIVEN stop type WHEN StopEdited is constructed THEN name and properties match the tracking plan`() {
        val event = AnalyticsEvent.StopEdited(stopType = StopTypeParam.INTERMEDIATE)

        val expectedName = "stop_edited"
        val expectedProperties = mapOf("stop_type" to "intermediate")
        assertEquals(expectedName, event.name)
        assertEquals(expectedProperties, event.properties)
        assertNoScreamingSnakeCase(event.properties)
    }

    @Test
    fun `GIVEN stop type and stop count WHEN StopRemoved is constructed THEN name and properties match the tracking plan`() {
        val event = AnalyticsEvent.StopRemoved(stopType = StopTypeParam.DESTINATION, stopCount = 1)

        val expectedName = "stop_removed"
        val expectedProperties = mapOf("stop_type" to "destination", "stop_count" to 1)
        assertEquals(expectedName, event.name)
        assertEquals(expectedProperties, event.properties)
        assertNoScreamingSnakeCase(event.properties)
    }

    @Test
    fun `GIVEN direction and stop count WHEN StopReordered is constructed THEN name and properties match the tracking plan`() {
        val event = AnalyticsEvent.StopReordered(direction = "up", stopCount = 3)

        val expectedName = "stop_reordered"
        val expectedProperties = mapOf("direction" to "up", "stop_count" to 3)
        assertEquals(expectedName, event.name)
        assertEquals(expectedProperties, event.properties)
        assertNoScreamingSnakeCase(event.properties)
    }

    @Test
    fun `GIVEN stop position and stop count WHEN StopDeparted is constructed THEN name and properties match the tracking plan`() {
        val event = AnalyticsEvent.StopDeparted(stopPosition = 0, stopCount = 3)

        val expectedName = "stop_departed"
        val expectedProperties = mapOf("stop_position" to 0, "stop_count" to 3)
        assertEquals(expectedName, event.name)
        assertEquals(expectedProperties, event.properties)
        assertNoScreamingSnakeCase(event.properties)
    }

    @Test
    fun `GIVEN stop position WHEN StopDepartureUndone is constructed THEN name and properties match the tracking plan`() {
        val event = AnalyticsEvent.StopDepartureUndone(stopPosition = 1)

        val expectedName = "stop_departure_undone"
        val expectedProperties = mapOf("stop_position" to 1)
        assertEquals(expectedName, event.name)
        assertEquals(expectedProperties, event.properties)
        assertNoScreamingSnakeCase(event.properties)
    }

    @Test
    fun `GIVEN suggestion count WHEN PlaceSearchPerformed is constructed THEN name and properties match the tracking plan`() {
        val event = AnalyticsEvent.PlaceSearchPerformed(suggestionCount = 5)

        val expectedName = "place_search_performed"
        val expectedProperties = mapOf("suggestion_count" to 5)
        assertEquals(expectedName, event.name)
        assertEquals(expectedProperties, event.properties)
        assertNoScreamingSnakeCase(event.properties)
    }

    @Test
    fun `GIVEN suggestion position WHEN PlaceSuggestionSelected is constructed THEN name and properties match the tracking plan`() {
        val event = AnalyticsEvent.PlaceSuggestionSelected(suggestionPosition = 0)

        val expectedName = "place_suggestion_selected"
        val expectedProperties = mapOf("suggestion_position" to 0)
        assertEquals(expectedName, event.name)
        assertEquals(expectedProperties, event.properties)
        assertNoScreamingSnakeCase(event.properties)
    }

    @Test
    fun `GIVEN operation and error type WHEN OperationFailed is constructed THEN name and properties match the tracking plan`() {
        val event = AnalyticsEvent.OperationFailed(
            operation = AnalyticsOperation.CREATE_TRIP,
            errorType = AnalyticsErrorType.NETWORK,
        )

        val expectedName = "operation_failed"
        val expectedProperties = mapOf("operation" to "create_trip", "error_type" to "network")
        assertEquals(expectedName, event.name)
        assertEquals(expectedProperties, event.properties)
        assertNoScreamingSnakeCase(event.properties)
    }

    @Test
    fun `GIVEN a screen WHEN ScreenViewed is constructed THEN name and properties match the tracking plan`() {
        val event = AnalyticsEvent.ScreenViewed(screen = AnalyticsScreen.TRIP_LIST)

        val expectedName = "screen_viewed"
        val expectedProperties = mapOf("screen_name" to "trip_list")
        assertEquals(expectedName, event.name)
        assertEquals(expectedProperties, event.properties)
        assertNoScreamingSnakeCase(event.properties)
    }

    @Test
    fun `GIVEN all 14 event subtypes constructed WHEN properties are inspected THEN no value is SCREAMING_SNAKE_CASE`() {
        val events: List<AnalyticsEvent> = listOf(
            AnalyticsEvent.TripCreated(isFirstTrip = true),
            AnalyticsEvent.TripOpened(stopCount = 2, routeState = "none"),
            AnalyticsEvent.StopAdded(stopType = StopTypeParam.STARTING_POINT, stopCount = 1),
            AnalyticsEvent.RouteCalculated(
                stopCount = 2,
                legCount = 1,
                distanceBand = DistanceBand.UNDER_50KM,
                durationBand = DurationBand.UNDER_1H,
            ),
            AnalyticsEvent.NavigationLaunched(stopType = StopTypeParam.DESTINATION, stopPosition = 1),
            AnalyticsEvent.StopEdited(stopType = StopTypeParam.INTERMEDIATE),
            AnalyticsEvent.StopRemoved(stopType = StopTypeParam.DESTINATION, stopCount = 1),
            AnalyticsEvent.StopReordered(direction = "down", stopCount = 3),
            AnalyticsEvent.StopDeparted(stopPosition = 0, stopCount = 2),
            AnalyticsEvent.StopDepartureUndone(stopPosition = 1),
            AnalyticsEvent.PlaceSearchPerformed(suggestionCount = 3),
            AnalyticsEvent.PlaceSuggestionSelected(suggestionPosition = 2),
            AnalyticsEvent.OperationFailed(
                operation = AnalyticsOperation.CALCULATE_ROUTE,
                errorType = AnalyticsErrorType.TIMEOUT,
            ),
            AnalyticsEvent.ScreenViewed(screen = AnalyticsScreen.TRIP_DETAIL),
        )

        events.forEach { event ->
            assertNoScreamingSnakeCase(event.properties)
        }
    }
}
