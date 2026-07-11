package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.Trip
import com.guidovezzoni.venice.domain.repository.TripRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private const val TRIP_ID = "trip-1"

class ObserveTripUseCaseTest {

    private lateinit var tripRepository: TripRepository
    private lateinit var useCase: ObserveTripUseCase

    @Before
    fun setUp() {
        tripRepository = mockk()
        useCase = ObserveTripUseCase(tripRepository)
    }

    @Test
    fun `GIVEN repository emits a trip WHEN invoke is called THEN the same trip is returned`() = runTest {
        val expectedTrip = Trip(
            id = TRIP_ID,
            name = "Road Trip",
            createdAt = 1000L,
            updatedAt = 2000L,
            stopCount = 3,
        )
        every { tripRepository.observeTripById(TRIP_ID) } returns flowOf(expectedTrip)

        val result = useCase(TRIP_ID).first()

        assertEquals(expectedTrip, result)
    }

    @Test
    fun `GIVEN repository emits null WHEN invoke is called THEN null is returned`() = runTest {
        val expectedTrip = null
        every { tripRepository.observeTripById(TRIP_ID) } returns flowOf(expectedTrip)

        val result = useCase(TRIP_ID).first()

        assertEquals(expectedTrip, result)
    }
}
