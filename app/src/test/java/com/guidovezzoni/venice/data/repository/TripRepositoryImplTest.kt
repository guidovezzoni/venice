package com.guidovezzoni.venice.data.repository

import com.guidovezzoni.venice.data.database.dao.TripDao
import com.guidovezzoni.venice.data.database.entity.TripEntity
import com.guidovezzoni.venice.data.database.entity.TripWithStopCount
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TripRepositoryImplTest {

    private lateinit var tripDao: TripDao
    private lateinit var repository: TripRepositoryImpl

    @Before
    fun setUp() {
        tripDao = mockk()
        repository = TripRepositoryImpl(tripDao)
    }

    @Test
    fun `GIVEN a valid name WHEN createTrip is called THEN inserts entity and returns Trip with UUID id`() = runTest {
        val entitySlot = slot<TripEntity>()
        coEvery { tripDao.insert(capture(entitySlot)) } returns Unit

        val result = repository.createTrip("  Summer Drive  ")

        assertTrue(result.isSuccess)
        val trip = result.getOrThrow()
        assertEquals("Summer Drive", trip.name)
        assertEquals(0, trip.stopCount)
        assertTrue(trip.id.isNotBlank())
        assertEquals(entitySlot.captured.id, trip.id)
        coVerify(exactly = 1) { tripDao.insert(any()) }
    }

    @Test
    fun `GIVEN DAO throws exception WHEN createTrip is called THEN returns Result failure`() = runTest {
        val exception = RuntimeException("DB error")
        coEvery { tripDao.insert(any()) } throws exception

        val result = repository.createTrip("Coast Trip")

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `GIVEN DAO emits trip entities WHEN observeTrips is called THEN mapped domain trips are emitted`() = runTest {
        val entity1 = TripWithStopCount(
            trip = TripEntity(id = "t1", name = "Trip One", createdAt = 1000L, updatedAt = 2000L),
            stopCount = 3,
        )
        val entity2 = TripWithStopCount(
            trip = TripEntity(id = "t2", name = "Trip Two", createdAt = 3000L, updatedAt = 4000L),
            stopCount = 0,
        )
        every { tripDao.observeAll() } returns flowOf(listOf(entity1, entity2))

        val trips = repository.observeTrips().first()

        val expectedTripCount = 2
        assertEquals(expectedTripCount, trips.size)
        assertEquals("t1", trips[0].id)
        assertEquals("Trip One", trips[0].name)
        val expectedStopCount = 3
        assertEquals(expectedStopCount, trips[0].stopCount)
        assertEquals("t2", trips[1].id)
        val expectedSecondStopCount = 0
        assertEquals(expectedSecondStopCount, trips[1].stopCount)
    }

    @Test
    fun `GIVEN TripDao observeById emits a TripWithStopCount WHEN observeTripById is called THEN a mapped domain Trip is emitted`() = runTest {
        val tripId = "trip-123"
        val entity = TripWithStopCount(
            trip = TripEntity(id = tripId, name = "Summer Drive", createdAt = 1000L, updatedAt = 2000L),
            stopCount = 5,
        )
        every { tripDao.observeById(tripId) } returns flowOf(entity)

        val result = repository.observeTripById(tripId).first()

        val expectedName = "Summer Drive"
        val expectedCreatedAt = 1000L
        val expectedUpdatedAt = 2000L
        val expectedStopCount = 5
        assertEquals(tripId, result?.id)
        assertEquals(expectedName, result?.name)
        assertEquals(expectedCreatedAt, result?.createdAt)
        assertEquals(expectedUpdatedAt, result?.updatedAt)
        assertEquals(expectedStopCount, result?.stopCount)
    }

    @Test
    fun `GIVEN TripDao observeById emits null WHEN observeTripById is called THEN null is emitted`() = runTest {
        val tripId = "non-existent-trip"
        every { tripDao.observeById(tripId) } returns flowOf(null)

        val result = repository.observeTripById(tripId).first()

        assertEquals(null, result)
    }
}
