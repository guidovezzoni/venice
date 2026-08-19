package com.guidovezzoni.venice.core.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AnalyticsUserPropertyTest {

    @Test
    fun givenCountBandRange2_5_whenTripCountBandIsConstructed_thenItHoldsTheTypedValue() {
        val property = AnalyticsUserProperty.TripCountBand(band = CountBand.RANGE_2_5)

        val expectedBand = CountBand.RANGE_2_5
        assertEquals(expectedBand, property.band)
    }

    @Test
    fun givenImperialUnit_whenDistanceUnitIsConstructed_thenItHoldsTheStringValue() {
        val property = AnalyticsUserProperty.DistanceUnit(unit = "imperial")

        val expectedUnit = "imperial"
        assertEquals(expectedUnit, property.unit)
    }

    @Test
    fun givenMetricUnit_whenDistanceUnitIsConstructed_thenItHoldsTheStringValue() {
        val property = AnalyticsUserProperty.DistanceUnit(unit = "metric")

        val expectedUnit = "metric"
        assertEquals(expectedUnit, property.unit)
    }

    @Test
    fun givenAllEventParameterNames_whenCheckedAgainstUserPropertyNames_thenNoNameCollisionExists() {
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

        // User property names must not collide with event parameter names
        val userPropertyNames = setOf("trip_count_band", "distance_unit")

        userPropertyNames.forEach { propertyName ->
            assertFalse(
                "User property name '$propertyName' must not collide with any event parameter name",
                eventParameterNames.contains(propertyName),
            )
        }
    }
}
