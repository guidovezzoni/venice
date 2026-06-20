package com.guidovezzoni.venice.data.database.mapper

import com.guidovezzoni.venice.data.database.entity.TripEntity
import com.guidovezzoni.venice.data.database.entity.TripWithStopCount
import org.junit.Assert.assertEquals
import org.junit.Test

private const val ID = "trip-1"
private const val NAME = "Summer Drive"
private const val CREATED_AT = 1700000000000L
private const val UPDATED_AT = 1700001000000L
private const val STOP_COUNT = 3

class TripMapperTest {

    @Test
    fun `GIVEN a TripWithStopCount WHEN toDomain is called THEN a Trip with matching fields is returned`() {
        val entity = TripWithStopCount(
            trip = TripEntity(
                id = ID,
                name = NAME,
                createdAt = CREATED_AT,
                updatedAt = UPDATED_AT,
            ),
            stopCount = STOP_COUNT,
        )

        val result = entity.toDomain()

        assertEquals(ID, result.id)
        assertEquals(NAME, result.name)
        assertEquals(CREATED_AT, result.createdAt)
        assertEquals(UPDATED_AT, result.updatedAt)
        assertEquals(STOP_COUNT, result.stopCount)
    }

    @Test
    fun `GIVEN a TripWithStopCount with zero stops WHEN toDomain is called THEN a Trip with stopCount zero is returned`() {
        val entity = TripWithStopCount(
            trip = TripEntity(
                id = ID,
                name = NAME,
                createdAt = CREATED_AT,
                updatedAt = UPDATED_AT,
            ),
            stopCount = 0,
        )

        val result = entity.toDomain()

        val expectedStopCount = 0
        assertEquals(expectedStopCount, result.stopCount)
    }
}
