package com.guidovezzoni.venice.data.repository

import com.guidovezzoni.venice.data.database.dao.StopDao
import com.guidovezzoni.venice.data.database.dao.TripDao
import com.guidovezzoni.venice.data.database.dao.TripStopCount
import com.guidovezzoni.venice.data.database.entity.TripEntity
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
    private lateinit var stopDao: StopDao
    private lateinit var repository: TripRepositoryImpl

    @Before
    fun setUp() {
        tripDao = mockk()
        stopDao = mockk()
        every { stopDao.observeStopCounts() } returns flowOf(emptyList())
        repository = TripRepositoryImpl(tripDao, stopDao)
    }

    @Test
    fun `GIVEN a valid name WHEN createTrip is called THEN inserts entity and returns Trip with UUID id`() = runTest {
        val entitySlot = slot<TripEntity>()
        coEvery { tripDao.insert(capture(entitySlot)) } returns Unit

        val result = repository.createTrip("  Summer Drive  ")

        assertTrue(result.isSuccess)
        val trip = result.getOrNull()!!
        assertEquals("Summer Drive", trip.name)
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
    fun `GIVEN trips and stop counts WHEN observeTrips is collected THEN trips include correct stop counts`() = runTest {
        val tripEntity = TripEntity(id = "trip-1", name = "Summer", createdAt = 1L, updatedAt = 1L)
        every { tripDao.observeAll() } returns flowOf(listOf(tripEntity))
        every { stopDao.observeStopCounts() } returns flowOf(listOf(TripStopCount("trip-1", 3)))

        val trips = repository.observeTrips().first()

        val expectedStopCount = 3
        assertEquals(1, trips.size)
        assertEquals(expectedStopCount, trips.first().stopCount)
    }

    @Test
    fun `GIVEN trips with no stops WHEN observeTrips is collected THEN stop count defaults to zero`() = runTest {
        val tripEntity = TripEntity(id = "trip-1", name = "Summer", createdAt = 1L, updatedAt = 1L)
        every { tripDao.observeAll() } returns flowOf(listOf(tripEntity))
        every { stopDao.observeStopCounts() } returns flowOf(emptyList())

        val trips = repository.observeTrips().first()

        val expectedStopCount = 0
        assertEquals(expectedStopCount, trips.first().stopCount)
    }
}
