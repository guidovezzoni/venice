package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.repository.StopRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val TRIP_ID = "trip-1"

class MoveStopUseCaseTest {

    private lateinit var stopRepository: StopRepository
    private lateinit var invalidateRouteUseCase: InvalidateRouteUseCase
    private lateinit var moveStopUseCase: MoveStopUseCase

    @Before
    fun setUp() {
        stopRepository = mockk()
        invalidateRouteUseCase = mockk()
        moveStopUseCase = MoveStopUseCase(stopRepository, invalidateRouteUseCase)
    }

    @Test
    fun `GIVEN two adjacent intermediate stops WHEN move up is called on the second THEN swapStopOrder is called with correct orders`() = runTest {
        coEvery { stopRepository.swapStopOrder(TRIP_ID, 2, 1) } returns Result.success(Unit)
        coEvery { invalidateRouteUseCase(TRIP_ID) } returns Result.success(Unit)

        val result = moveStopUseCase(TRIP_ID, 2, 1)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { stopRepository.swapStopOrder(TRIP_ID, 2, 1) }
    }

    @Test
    fun `GIVEN two adjacent intermediate stops WHEN move down is called on the first THEN swapStopOrder is called with correct orders`() = runTest {
        coEvery { stopRepository.swapStopOrder(TRIP_ID, 1, 2) } returns Result.success(Unit)
        coEvery { invalidateRouteUseCase(TRIP_ID) } returns Result.success(Unit)

        val result = moveStopUseCase(TRIP_ID, 1, 2)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { stopRepository.swapStopOrder(TRIP_ID, 1, 2) }
    }

    @Test
    fun `GIVEN repository returns failure WHEN move is called THEN failure is propagated`() = runTest {
        val exception = IllegalStateException("No stop found")
        coEvery { stopRepository.swapStopOrder(TRIP_ID, 1, 2) } returns Result.failure(exception)

        val result = moveStopUseCase(TRIP_ID, 1, 2)

        assertTrue(result.isFailure)
    }

    @Test
    fun `GIVEN a successful move WHEN MoveStopUseCase is invoked THEN InvalidateRouteUseCase is called`() = runTest {
        coEvery { stopRepository.swapStopOrder(TRIP_ID, 1, 2) } returns Result.success(Unit)
        coEvery { invalidateRouteUseCase(TRIP_ID) } returns Result.success(Unit)

        moveStopUseCase(TRIP_ID, 1, 2)

        coVerify(exactly = 1) { invalidateRouteUseCase(TRIP_ID) }
    }

    @Test
    fun `GIVEN a failed move WHEN MoveStopUseCase is invoked THEN InvalidateRouteUseCase is NOT called`() = runTest {
        val exception = IllegalStateException("No stop found")
        coEvery { stopRepository.swapStopOrder(TRIP_ID, 1, 2) } returns Result.failure(exception)

        moveStopUseCase(TRIP_ID, 1, 2)

        coVerify(exactly = 0) { invalidateRouteUseCase(any()) }
    }
}
