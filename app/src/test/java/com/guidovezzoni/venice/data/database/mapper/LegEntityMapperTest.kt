package com.guidovezzoni.venice.data.database.mapper

import com.guidovezzoni.venice.data.database.entity.LegEntity
import com.guidovezzoni.venice.domain.model.Leg
import org.junit.Assert.assertEquals
import org.junit.Test

private const val ID = "leg-1"
private const val TRIP_ID = "trip-1"
private const val FROM_STOP_ID = "stop-a"
private const val TO_STOP_ID = "stop-b"
private const val DISTANCE_METRES = 12345
private const val DURATION_SECONDS = 678
private const val ENCODED_POLYLINE = "abc123"

class LegEntityMapperTest {

    @Test
    fun `GIVEN a LegEntity WHEN toDomain is called THEN a Leg with matching fields is returned`() {
        val entity = LegEntity(
            id = ID,
            tripId = TRIP_ID,
            fromStopId = FROM_STOP_ID,
            toStopId = TO_STOP_ID,
            distanceMetres = DISTANCE_METRES,
            durationSeconds = DURATION_SECONDS,
            encodedPolyline = ENCODED_POLYLINE,
        )

        val result = entity.toDomain()

        assertEquals(ID, result.id)
        assertEquals(TRIP_ID, result.tripId)
        assertEquals(FROM_STOP_ID, result.fromStopId)
        assertEquals(TO_STOP_ID, result.toStopId)
        assertEquals(DISTANCE_METRES, result.distanceMetres)
        assertEquals(DURATION_SECONDS, result.durationSeconds)
        assertEquals(ENCODED_POLYLINE, result.encodedPolyline)
    }

    @Test
    fun `GIVEN a Leg WHEN toEntity is called THEN a LegEntity with matching fields is returned`() {
        val leg = Leg(
            id = ID,
            tripId = TRIP_ID,
            fromStopId = FROM_STOP_ID,
            toStopId = TO_STOP_ID,
            distanceMetres = DISTANCE_METRES,
            durationSeconds = DURATION_SECONDS,
            encodedPolyline = ENCODED_POLYLINE,
        )

        val result = leg.toEntity()

        assertEquals(ID, result.id)
        assertEquals(TRIP_ID, result.tripId)
        assertEquals(FROM_STOP_ID, result.fromStopId)
        assertEquals(TO_STOP_ID, result.toStopId)
        assertEquals(DISTANCE_METRES, result.distanceMetres)
        assertEquals(DURATION_SECONDS, result.durationSeconds)
        assertEquals(ENCODED_POLYLINE, result.encodedPolyline)
    }
}
