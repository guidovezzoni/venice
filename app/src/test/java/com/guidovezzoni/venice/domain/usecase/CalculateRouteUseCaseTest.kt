package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.model.StopStatus
import com.guidovezzoni.venice.domain.repository.RouteRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val TRIP_ID = "trip-1"

class CalculateRouteUseCaseTest {

    private lateinit var routeRepository: RouteRepository
    private lateinit var useCase: CalculateRouteUseCase

    @Before
    fun setUp() {
        routeRepository = mockk()
        useCase = CalculateRouteUseCase(routeRepository)
    }

    @Test
    fun `GIVEN 3 stops WHEN CalculateRouteUseCase is invoked THEN RouteRepository calculateRoute is called and Result success is returned`() = runTest {
        val stops = listOf(
            Stop("stop-1", TRIP_ID, "Rome", 41.9028, 12.4964, 0, StopStatus.PENDING),
            Stop("stop-2", TRIP_ID, "Florence", 43.7696, 11.2558, 1, StopStatus.PENDING),
            Stop("stop-3", TRIP_ID, "Venice", 45.4408, 12.3155, 2, StopStatus.PENDING),
        )
        coEvery { routeRepository.calculateRoute(TRIP_ID, stops) } returns Result.success(Unit)

        val result = useCase(TRIP_ID, stops)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { routeRepository.calculateRoute(TRIP_ID, stops) }
    }

    @Test
    fun `GIVEN 1 stop WHEN CalculateRouteUseCase is invoked THEN Result failure with IllegalStateException is returned`() = runTest {
        val stops = listOf(
            Stop("stop-1", TRIP_ID, "Rome", 41.9028, 12.4964, 0, StopStatus.PENDING),
        )

        val result = useCase(TRIP_ID, stops)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        coVerify(exactly = 0) { routeRepository.calculateRoute(any(), any()) }
    }

    @Test
    fun `GIVEN repository failure WHEN CalculateRouteUseCase is invoked THEN Result failure is propagated`() = runTest {
        val stops = listOf(
            Stop("stop-1", TRIP_ID, "Rome", 41.9028, 12.4964, 0, StopStatus.PENDING),
            Stop("stop-2", TRIP_ID, "Florence", 43.7696, 11.2558, 1, StopStatus.PENDING),
        )
        val exception = RuntimeException("Network error")
        coEvery { routeRepository.calculateRoute(TRIP_ID, stops) } returns Result.failure(exception)

        val result = useCase(TRIP_ID, stops)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { routeRepository.calculateRoute(TRIP_ID, stops) }
    }
}
