package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.model.StopStatus
import com.guidovezzoni.venice.domain.repository.StopRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val STOP_ID = "stop-1"
private const val TRIP_ID = "trip-1"
private const val PLACE_NAME = "Florence"
private const val LATITUDE = 43.7696
private const val LONGITUDE = 11.2558

class EditStopUseCaseTest {

    private lateinit var stopRepository: StopRepository
    private lateinit var editStopUseCase: EditStopUseCase

    @Before
    fun setUp() {
        stopRepository = mockk()
        editStopUseCase = EditStopUseCase(stopRepository)
    }

    @Test
    fun `GIVEN valid parameters WHEN invoke is called THEN repository updateStop is called with trimmed placeName and Result success is returned`() = runTest {
        val updatedStop = Stop(STOP_ID, TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 2, StopStatus.PENDING)
        coEvery { stopRepository.updateStop(STOP_ID, PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.success(updatedStop)

        val result = editStopUseCase(STOP_ID, PLACE_NAME, LATITUDE, LONGITUDE)

        assertTrue(result.isSuccess)
        assertEquals(updatedStop, result.getOrThrow())
        coVerify(exactly = 1) { stopRepository.updateStop(STOP_ID, PLACE_NAME, LATITUDE, LONGITUDE) }
    }

    @Test
    fun `GIVEN placeName with whitespace WHEN invoke is called THEN repository receives trimmed placeName`() = runTest {
        val updatedStop = Stop(STOP_ID, TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 2, StopStatus.PENDING)
        coEvery { stopRepository.updateStop(STOP_ID, PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.success(updatedStop)

        val result = editStopUseCase(STOP_ID, "  $PLACE_NAME  ", LATITUDE, LONGITUDE)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { stopRepository.updateStop(STOP_ID, PLACE_NAME, LATITUDE, LONGITUDE) }
    }

    @Test
    fun `GIVEN repository returns failure WHEN invoke is called THEN Result failure is propagated`() = runTest {
        val exception = IllegalStateException("Stop not found")
        coEvery { stopRepository.updateStop(STOP_ID, PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.failure(exception)

        val result = editStopUseCase(STOP_ID, PLACE_NAME, LATITUDE, LONGITUDE)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
