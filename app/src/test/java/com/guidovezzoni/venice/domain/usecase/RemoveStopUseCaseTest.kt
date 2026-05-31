package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.repository.StopRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val TRIP_ID = "trip-1"
private const val STOP_ID = "stop-2"

class RemoveStopUseCaseTest {

    private lateinit var stopRepository: StopRepository
    private lateinit var removeStopUseCase: RemoveStopUseCase

    @Before
    fun setUp() {
        stopRepository = mockk()
        removeStopUseCase = RemoveStopUseCase(stopRepository)
    }

    @Test
    fun `GIVEN repository returns success WHEN invoke is called THEN Result success Unit is returned`() = runTest {
        coEvery { stopRepository.deleteStop(TRIP_ID, STOP_ID) } returns Result.success(Unit)

        val result = removeStopUseCase(TRIP_ID, STOP_ID)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { stopRepository.deleteStop(TRIP_ID, STOP_ID) }
    }

    @Test
    fun `GIVEN repository returns failure WHEN invoke is called THEN Result failure is propagated`() = runTest {
        val exception = IllegalStateException("Stop not found")
        coEvery { stopRepository.deleteStop(TRIP_ID, STOP_ID) } returns Result.failure(exception)

        val result = removeStopUseCase(TRIP_ID, STOP_ID)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
