package com.guidovezzoni.venice.core.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AnalyticsUserPropertyTest {

    @Test
    fun `GIVEN CountBand RANGE_2_5 WHEN TripCountBand is constructed THEN it holds the typed value`() {
        val property = AnalyticsUserProperty.TripCountBand(band = CountBand.RANGE_2_5)

        val expectedBand = CountBand.RANGE_2_5
        assertEquals(expectedBand, property.band)
    }

    @Test
    fun `GIVEN IMPERIAL unit WHEN DistanceUnit is constructed THEN it holds the typed value with correct string`() {
        val property = AnalyticsUserProperty.DistanceUnit(unit = DistanceUnitParam.IMPERIAL)

        val expectedUnit = DistanceUnitParam.IMPERIAL
        assertEquals(expectedUnit, property.unit)
        assertEquals("imperial", property.unit.value)
    }

    @Test
    fun `GIVEN METRIC unit WHEN DistanceUnit is constructed THEN it holds the typed value with correct string`() {
        val property = AnalyticsUserProperty.DistanceUnit(unit = DistanceUnitParam.METRIC)

        val expectedUnit = DistanceUnitParam.METRIC
        assertEquals(expectedUnit, property.unit)
        assertEquals("metric", property.unit.value)
    }

    @Test
    fun `GIVEN all event parameter names and all user property names WHEN checked for collisions THEN no name appears in both sets`() {
        val eventParameterNames = setOf(
            "is_first_trip",
            "stop_count",
            "route_state",
            "stop_type",
            "leg_count",
            "distance_band",
            "duration_band",
            "stop_position",
            "direction",
            "suggestion_count",
            "suggestion_position",
            "operation",
            "error_type",
            "screen_name",
        )

        val userPropertyNames = setOf("trip_count_band", "distance_unit")

        userPropertyNames.forEach { propertyName ->
            assertFalse(
                "User property name '$propertyName' must not collide with any event parameter name",
                eventParameterNames.contains(propertyName),
            )
        }
    }
}
