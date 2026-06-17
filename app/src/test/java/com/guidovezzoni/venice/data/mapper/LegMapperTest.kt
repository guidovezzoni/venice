package com.guidovezzoni.venice.data.mapper

import com.guidovezzoni.venice.data.network.dto.DirectionsLeg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegMapperTest {

    @Test
    fun `GIVEN a DirectionsLeg WHEN mapped with tripId and stopIds THEN a Leg with correct fields and UUID id is returned`() {
        val directionsLeg = DirectionsLeg(distanceMetres = 12345, durationSeconds = 678, encodedPolyline = "poly123")
        val tripId = "trip-1"
        val fromStopId = "stop-a"
        val toStopId = "stop-b"

        val result = directionsLeg.toDomain(tripId, fromStopId, toStopId)

        // UUID is generated, so just assert it's not blank
        assertTrue(result.id.isNotBlank())
        assertEquals(tripId, result.tripId)
        assertEquals(fromStopId, result.fromStopId)
        assertEquals(toStopId, result.toStopId)
        assertEquals(12345, result.distanceMetres)
        assertEquals(678, result.durationSeconds)
        assertEquals("poly123", result.encodedPolyline)
    }

    @Test
    fun `GIVEN a DirectionsLeg with null fields WHEN mapped THEN defaults are applied`() {
        val directionsLeg = DirectionsLeg(distanceMetres = null, durationSeconds = null, encodedPolyline = null)

        val result = directionsLeg.toDomain("trip-1", "stop-a", "stop-b")

        assertEquals(0, result.distanceMetres)
        assertEquals(0, result.durationSeconds)
        assertEquals("", result.encodedPolyline)
    }
}
