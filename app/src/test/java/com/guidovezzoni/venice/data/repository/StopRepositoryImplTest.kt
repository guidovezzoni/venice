package com.guidovezzoni.venice.data.repository

import com.guidovezzoni.venice.data.database.dao.StopDao
import com.guidovezzoni.venice.data.database.entity.StopEntity
import com.guidovezzoni.venice.domain.model.StopStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val TRIP_ID = "trip-1"
private const val PLACE_NAME = "Rome"
private const val LATITUDE = 41.9028
private const val LONGITUDE = 12.4964

class StopRepositoryImplTest {

    private lateinit var stopDao: StopDao
    private lateinit var repository: StopRepositoryImpl

    @Before
    fun setUp() {
        stopDao = mockk()
        repository = StopRepositoryImpl(stopDao)
    }

    @Test
    fun `GIVEN no stop with order=0 for the trip WHEN upsertStartingPoint is called THEN a new stop is inserted and returned with order=0 and status=PENDING`() = runTest {
        coEvery { stopDao.getStartingPoint(TRIP_ID) } returns null
        val entitySlot = slot<StopEntity>()
        coEvery { stopDao.insert(capture(entitySlot)) } returns Unit

        val result = repository.upsertStartingPoint(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE)

        assertTrue(result.isSuccess)
        val stop = result.getOrThrow()
        val expectedOrder = 0
        assertEquals(expectedOrder, stop.order)
        assertEquals(StopStatus.PENDING, stop.status)
        assertEquals(PLACE_NAME, stop.placeName)
        assertEquals(LATITUDE, stop.latitude, 0.0)
        assertEquals(LONGITUDE, stop.longitude, 0.0)
        assertTrue(stop.id.isNotBlank())
        coVerify(exactly = 1) { stopDao.insert(any()) }
        coVerify(exactly = 0) { stopDao.update(any()) }
    }

    @Test
    fun `GIVEN a stop with order=0 already exists WHEN upsertStartingPoint is called THEN the existing stop is updated with the new name and coordinates`() = runTest {
        val existingEntity = StopEntity(
            id = "existing-id",
            tripId = TRIP_ID,
            placeName = "Milan",
            latitude = 45.4642,
            longitude = 9.1900,
            order = 0,
            status = "PENDING",
        )
        coEvery { stopDao.getStartingPoint(TRIP_ID) } returns existingEntity
        coEvery { stopDao.update(any()) } returns Unit

        val result = repository.upsertStartingPoint(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE)

        assertTrue(result.isSuccess)
        val stop = result.getOrThrow()
        val expectedId = "existing-id"
        assertEquals(expectedId, stop.id)
        assertEquals(PLACE_NAME, stop.placeName)
        assertEquals(LATITUDE, stop.latitude, 0.0)
        assertEquals(LONGITUDE, stop.longitude, 0.0)
        coVerify(exactly = 0) { stopDao.insert(any()) }
        coVerify(exactly = 1) { stopDao.update(any()) }
    }

    @Test
    fun `GIVEN DAO throws an exception WHEN upsertStartingPoint is called THEN Result failure is returned`() = runTest {
        val exception = RuntimeException("DB error")
        coEvery { stopDao.getStartingPoint(TRIP_ID) } throws exception

        val result = repository.upsertStartingPoint(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `GIVEN no stop with order greater than 0 for the trip WHEN upsertDestination is called THEN a new stop is inserted with order=1 and status=PENDING`() = runTest {
        coEvery { stopDao.getDestination(TRIP_ID) } returns null
        val entitySlot = slot<StopEntity>()
        coEvery { stopDao.insert(capture(entitySlot)) } returns Unit

        val result = repository.upsertDestination(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE)

        assertTrue(result.isSuccess)
        val stop = result.getOrThrow()
        val expectedOrder = 1
        assertEquals(expectedOrder, stop.order)
        assertEquals(StopStatus.PENDING, stop.status)
        assertEquals(PLACE_NAME, stop.placeName)
        assertEquals(LATITUDE, stop.latitude, 0.0)
        assertEquals(LONGITUDE, stop.longitude, 0.0)
        assertTrue(stop.id.isNotBlank())
        coVerify(exactly = 1) { stopDao.insert(any()) }
        coVerify(exactly = 0) { stopDao.update(any()) }
    }

    @Test
    fun `GIVEN a destination stop already exists WHEN upsertDestination is called THEN the existing stop is updated with the new name and coordinates`() = runTest {
        val existingEntity = StopEntity(
            id = "existing-dest-id",
            tripId = TRIP_ID,
            placeName = "Milan",
            latitude = 45.4642,
            longitude = 9.1900,
            order = 1,
            status = "PENDING",
        )
        coEvery { stopDao.getDestination(TRIP_ID) } returns existingEntity
        coEvery { stopDao.update(any()) } returns Unit

        val result = repository.upsertDestination(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE)

        assertTrue(result.isSuccess)
        val stop = result.getOrThrow()
        val expectedId = "existing-dest-id"
        assertEquals(expectedId, stop.id)
        assertEquals(PLACE_NAME, stop.placeName)
        assertEquals(LATITUDE, stop.latitude, 0.0)
        assertEquals(LONGITUDE, stop.longitude, 0.0)
        coVerify(exactly = 0) { stopDao.insert(any()) }
        coVerify(exactly = 1) { stopDao.update(any()) }
    }

    @Test
    fun `GIVEN DAO throws an exception WHEN upsertDestination is called THEN Result failure is returned`() = runTest {
        val exception = RuntimeException("DB error")
        coEvery { stopDao.getDestination(TRIP_ID) } throws exception

        val result = repository.upsertDestination(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
