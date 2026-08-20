package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.model.StopStatus
import com.guidovezzoni.venice.domain.repository.StopRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val TRIP_ID = "trip-1"

class UndoMarkStopDepartedUseCaseTest {

    private lateinit var stopRepository: StopRepository
    private lateinit var useCase: UndoMarkStopDepartedUseCase

    @Before
    fun setUp() {
        stopRepository = mockk()
        useCase = UndoMarkStopDepartedUseCase(stopRepository)
    }

    @Test
    fun `GIVEN at least one VISITED stop exists WHEN invoke is called THEN updateStopStatus is called with the highest-order VISITED stop ID and PENDING and Result success is returned`() = runTest {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.VISITED),
            Stop("s1", TRIP_ID, "Stop A", 1.0, 1.0, 1, StopStatus.PENDING),
        )
        every { stopRepository.observeStopsForTrip(TRIP_ID) } returns flowOf(stops)
        coEvery { stopRepository.updateStopStatus("s0", StopStatus.PENDING) } returns Result.success(Unit)

        val result = useCase(TRIP_ID)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { stopRepository.updateStopStatus("s0", StopStatus.PENDING) }
    }

    @Test
    fun `GIVEN the most recently departed stop at order 1 WHEN UndoMarkStopDepartedUseCase succeeds THEN Result success wrapping that Stop with status PENDING and order 1 unchanged is returned`() = runTest {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.VISITED),
            Stop("s1", TRIP_ID, "Stop A", 1.0, 1.0, 1, StopStatus.VISITED),
            Stop("s2", TRIP_ID, "Stop B", 2.0, 2.0, 2, StopStatus.PENDING),
        )
        every { stopRepository.observeStopsForTrip(TRIP_ID) } returns flowOf(stops)
        coEvery { stopRepository.updateStopStatus("s1", StopStatus.PENDING) } returns Result.success(Unit)

        val result = useCase(TRIP_ID)

        assertTrue(result.isSuccess)
        val revertedStop = result.getOrThrow()
        val expectedStop = Stop("s1", TRIP_ID, "Stop A", 1.0, 1.0, 1, StopStatus.PENDING)
        assertEquals(expectedStop, revertedStop)
    }

    @Test
    fun `GIVEN no VISITED stops exist WHEN invoke is called THEN Result failure is returned with No departed stop to undo`() = runTest {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.PENDING),
            Stop("s1", TRIP_ID, "Stop A", 1.0, 1.0, 1, StopStatus.PENDING),
        )
        every { stopRepository.observeStopsForTrip(TRIP_ID) } returns flowOf(stops)

        val result = useCase(TRIP_ID)

        assertTrue(result.isFailure)
        assertEquals("No departed stop to undo", result.exceptionOrNull()?.message)
    }

    @Test
    fun `GIVEN multiple VISITED stops WHEN invoke is called THEN only the highest-order one is reverted`() = runTest {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.VISITED),
            Stop("s1", TRIP_ID, "Stop A", 1.0, 1.0, 1, StopStatus.VISITED),
            Stop("s2", TRIP_ID, "Stop B", 2.0, 2.0, 2, StopStatus.VISITED),
            Stop("s3", TRIP_ID, "End", 3.0, 3.0, 3, StopStatus.PENDING),
        )
        every { stopRepository.observeStopsForTrip(TRIP_ID) } returns flowOf(stops)
        coEvery { stopRepository.updateStopStatus("s2", StopStatus.PENDING) } returns Result.success(Unit)

        val result = useCase(TRIP_ID)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { stopRepository.updateStopStatus("s2", StopStatus.PENDING) }
        coVerify(exactly = 0) { stopRepository.updateStopStatus("s0", any()) }
        coVerify(exactly = 0) { stopRepository.updateStopStatus("s1", any()) }
    }

    @Test
    fun `GIVEN repository updateStopStatus fails WHEN invoke is called THEN Result failure is propagated`() = runTest {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.VISITED),
            Stop("s1", TRIP_ID, "Stop A", 1.0, 1.0, 1, StopStatus.PENDING),
        )
        every { stopRepository.observeStopsForTrip(TRIP_ID) } returns flowOf(stops)
        val exception = RuntimeException("DB error")
        coEvery { stopRepository.updateStopStatus("s0", StopStatus.PENDING) } returns Result.failure(exception)

        val result = useCase(TRIP_ID)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
