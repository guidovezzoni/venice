package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.model.StopStatus
import com.guidovezzoni.venice.domain.repository.StopRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private const val TRIP_ID = "trip-1"

class ObserveStopsUseCaseTest {

    private lateinit var stopRepository: StopRepository
    private lateinit var useCase: ObserveStopsUseCase

    @Before
    fun setUp() {
        stopRepository = mockk()
        useCase = ObserveStopsUseCase(stopRepository)
    }

    @Test
    fun `GIVEN stops exist for a trip WHEN invoke is called THEN the stops flow from the repository is returned`() = runTest {
        val expectedStops = listOf(
            Stop(
                id = "stop-1",
                tripId = TRIP_ID,
                placeName = "Rome",
                latitude = 41.9028,
                longitude = 12.4964,
                order = 0,
                status = StopStatus.PENDING,
            ),
        )
        every { stopRepository.observeStopsForTrip(TRIP_ID) } returns flowOf(expectedStops)

        val result = useCase(TRIP_ID).first()

        assertEquals(expectedStops, result)
        verify { stopRepository.observeStopsForTrip(TRIP_ID) }
    }

    @Test
    fun `GIVEN no stops exist for a trip WHEN invoke is called THEN an empty list is emitted`() = runTest {
        every { stopRepository.observeStopsForTrip(TRIP_ID) } returns flowOf(emptyList())

        val result = useCase(TRIP_ID).first()

        val expectedStops = emptyList<Stop>()
        assertEquals(expectedStops, result)
    }
}
