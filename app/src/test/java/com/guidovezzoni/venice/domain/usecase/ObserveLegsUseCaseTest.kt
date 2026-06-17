package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.Leg
import com.guidovezzoni.venice.domain.repository.RouteRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private const val TRIP_ID = "trip-1"

class ObserveLegsUseCaseTest {

    private lateinit var routeRepository: RouteRepository
    private lateinit var useCase: ObserveLegsUseCase

    @Before
    fun setUp() {
        routeRepository = mockk()
        useCase = ObserveLegsUseCase(routeRepository)
    }

    @Test
    fun `GIVEN a tripId WHEN ObserveLegsUseCase is invoked THEN a Flow from RouteRepository is returned`() = runTest {
        val expectedLegs = listOf(
            Leg(
                id = "leg-1",
                tripId = TRIP_ID,
                fromStopId = "stop-1",
                toStopId = "stop-2",
                distanceMetres = 1000,
                durationSeconds = 60,
                encodedPolyline = "",
            ),
        )
        every { routeRepository.observeLegsForTrip(TRIP_ID) } returns flowOf(expectedLegs)

        val result = useCase(TRIP_ID).first()

        assertEquals(expectedLegs, result)
    }

    @Test
    fun `GIVEN repository emits empty list WHEN ObserveLegsUseCase is invoked THEN an empty list is returned`() = runTest {
        val expectedLegs = emptyList<Leg>()
        every { routeRepository.observeLegsForTrip(TRIP_ID) } returns flowOf(expectedLegs)

        val result = useCase(TRIP_ID).first()

        assertEquals(expectedLegs, result)
    }
}
