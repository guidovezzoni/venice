package com.guidovezzoni.venice.domain.usecase

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

class InvalidateRouteUseCaseTest {

    private lateinit var routeRepository: RouteRepository
    private lateinit var useCase: InvalidateRouteUseCase

    @Before
    fun setUp() {
        routeRepository = mockk()
        useCase = InvalidateRouteUseCase(routeRepository)
    }

    @Test
    fun `GIVEN a tripId WHEN InvalidateRouteUseCase is invoked THEN RouteRepository deleteLegsForTrip is called`() = runTest {
        coEvery { routeRepository.deleteLegsForTrip(TRIP_ID) } returns Result.success(Unit)

        val result = useCase(TRIP_ID)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { routeRepository.deleteLegsForTrip(TRIP_ID) }
    }

    @Test
    fun `GIVEN repository failure WHEN InvalidateRouteUseCase is invoked THEN Result failure is propagated`() = runTest {
        val exception = RuntimeException("Database error")
        coEvery { routeRepository.deleteLegsForTrip(TRIP_ID) } returns Result.failure(exception)

        val result = useCase(TRIP_ID)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        coVerify(exactly = 1) { routeRepository.deleteLegsForTrip(TRIP_ID) }
    }
}
