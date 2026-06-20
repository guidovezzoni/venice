package com.guidovezzoni.venice.data.database.mapper

import com.guidovezzoni.venice.data.database.entity.StopEntity
import com.guidovezzoni.venice.domain.model.StopStatus
import org.junit.Assert.assertEquals
import org.junit.Test

private const val ID = "stop-1"
private const val TRIP_ID = "trip-1"
private const val PLACE_NAME = "Florence"
private const val LATITUDE = 43.7696
private const val LONGITUDE = 11.2558
private const val ORDER = 2

class StopMapperTest {

    @Test
    fun `GIVEN a StopEntity with PENDING status WHEN toDomain is called THEN a Stop with matching fields and StopStatus PENDING is returned`() {
        val entity = StopEntity(
            id = ID,
            tripId = TRIP_ID,
            placeName = PLACE_NAME,
            latitude = LATITUDE,
            longitude = LONGITUDE,
            order = ORDER,
            status = "PENDING",
        )

        val result = entity.toDomain()

        assertEquals(ID, result.id)
        assertEquals(TRIP_ID, result.tripId)
        assertEquals(PLACE_NAME, result.placeName)
        assertEquals(LATITUDE, result.latitude, 0.0)
        assertEquals(LONGITUDE, result.longitude, 0.0)
        assertEquals(ORDER, result.order)
        assertEquals(StopStatus.PENDING, result.status)
    }

    @Test
    fun `GIVEN a StopEntity with VISITED status WHEN toDomain is called THEN a Stop with StopStatus VISITED is returned`() {
        val entity = StopEntity(
            id = ID,
            tripId = TRIP_ID,
            placeName = PLACE_NAME,
            latitude = LATITUDE,
            longitude = LONGITUDE,
            order = ORDER,
            status = "VISITED",
        )

        val result = entity.toDomain()

        assertEquals(StopStatus.VISITED, result.status)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `GIVEN a StopEntity with invalid status WHEN toDomain is called THEN IllegalArgumentException is thrown`() {
        val entity = StopEntity(
            id = ID,
            tripId = TRIP_ID,
            placeName = PLACE_NAME,
            latitude = LATITUDE,
            longitude = LONGITUDE,
            order = ORDER,
            status = "INVALID_STATUS",
        )

        entity.toDomain()
    }
}
