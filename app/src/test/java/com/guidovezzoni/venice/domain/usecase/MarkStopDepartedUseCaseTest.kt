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

class MarkStopDepartedUseCaseTest {

    private lateinit var stopRepository: StopRepository
    private lateinit var useCase: MarkStopDepartedUseCase

    @Before
    fun setUp() {
        stopRepository = mockk()
        useCase = MarkStopDepartedUseCase(stopRepository)
    }

    @Test
    fun `GIVEN the target stop is the current stop WHEN invoke is called THEN updateStopStatus is called with VISITED and Result success is returned`() = runTest {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.VISITED),
            Stop("s1", TRIP_ID, "Stop A", 1.0, 1.0, 1, StopStatus.PENDING),
            Stop("s2", TRIP_ID, "Stop B", 2.0, 2.0, 2, StopStatus.PENDING),
        )
        every { stopRepository.observeStopsForTrip(TRIP_ID) } returns flowOf(stops)
        coEvery { stopRepository.updateStopStatus("s1", StopStatus.VISITED) } returns Result.success(Unit)

        val result = useCase(TRIP_ID, "s1")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { stopRepository.updateStopStatus("s1", StopStatus.VISITED) }
    }

    @Test
    fun `GIVEN the target stop is NOT the current stop WHEN invoke is called THEN Result failure is returned`() = runTest {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.VISITED),
            Stop("s1", TRIP_ID, "Stop A", 1.0, 1.0, 1, StopStatus.PENDING),
            Stop("s2", TRIP_ID, "Stop B", 2.0, 2.0, 2, StopStatus.PENDING),
        )
        every { stopRepository.observeStopsForTrip(TRIP_ID) } returns flowOf(stops)

        val result = useCase(TRIP_ID, "s2")

        assertTrue(result.isFailure)
        assertEquals("Only the current stop can be marked as departed", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { stopRepository.updateStopStatus(any(), any()) }
    }

    @Test
    fun `GIVEN no PENDING stops exist WHEN invoke is called THEN Result failure is returned`() = runTest {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.VISITED),
            Stop("s1", TRIP_ID, "Stop A", 1.0, 1.0, 1, StopStatus.VISITED),
        )
        every { stopRepository.observeStopsForTrip(TRIP_ID) } returns flowOf(stops)

        val result = useCase(TRIP_ID, "s1")

        assertTrue(result.isFailure)
        assertEquals("Only the current stop can be marked as departed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `GIVEN repository updateStopStatus fails WHEN invoke is called THEN Result failure is propagated`() = runTest {
        val stops = listOf(
            Stop("s0", TRIP_ID, "Start", 0.0, 0.0, 0, StopStatus.PENDING),
        )
        every { stopRepository.observeStopsForTrip(TRIP_ID) } returns flowOf(stops)
        val exception = RuntimeException("DB error")
        coEvery { stopRepository.updateStopStatus("s0", StopStatus.VISITED) } returns Result.failure(exception)

        val result = useCase(TRIP_ID, "s0")

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
