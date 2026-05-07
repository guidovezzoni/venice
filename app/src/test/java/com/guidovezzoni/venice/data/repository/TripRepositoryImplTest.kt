package com.guidovezzoni.venice.data.repository

import com.guidovezzoni.venice.data.database.dao.TripDao
import com.guidovezzoni.venice.data.database.entity.TripEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
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
}
