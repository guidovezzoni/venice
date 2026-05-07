package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.Trip
import com.guidovezzoni.venice.domain.repository.TripRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateTripUseCaseTest {

    private lateinit var repository: TripRepository
    private lateinit var useCase: CreateTripUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = CreateTripUseCase(repository)
    }

    @Test
    fun `GIVEN a blank name WHEN invoke is called THEN returns failure without calling repository`() = runTest {
        val result = useCase("   ")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { repository.createTrip(any()) }
    }

    @Test
    fun `GIVEN an empty name WHEN invoke is called THEN returns failure without calling repository`() = runTest {
        val result = useCase("")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { repository.createTrip(any()) }
    }

    @Test
    fun `GIVEN a valid name WHEN invoke is called THEN delegates to repository and returns result`() = runTest {
        val expectedTrip = Trip(id = "id", name = "Coast Trip", createdAt = 0L, updatedAt = 0L)
        coEvery { repository.createTrip("Coast Trip") } returns Result.success(expectedTrip)

        val result = useCase("Coast Trip")

        val actualTrip = result.getOrNull()
        assertEquals(expectedTrip, actualTrip)
        coVerify(exactly = 1) { repository.createTrip("Coast Trip") }
    }
}
