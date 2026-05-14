package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.model.StopStatus
import com.guidovezzoni.venice.domain.repository.StopRepository
import io.mockk.every
import io.mockk.mockk
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
    fun `GIVEN repository emits stops WHEN invoke is called THEN the same stops are returned`() = runTest {
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
    }

    @Test
    fun `GIVEN repository emits empty list WHEN invoke is called THEN an empty list is returned`() = runTest {
        val expectedStops = emptyList<Stop>()
        every { stopRepository.observeStopsForTrip(TRIP_ID) } returns flowOf(expectedStops)

        val result = useCase(TRIP_ID).first()

        assertEquals(expectedStops, result)
    }
}
