package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.model.StopStatus
import com.guidovezzoni.venice.domain.model.StopType
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
private const val PLACE_NAME = "Rome"
private const val LATITUDE = 41.9028
private const val LONGITUDE = 12.4964
private const val MAX_STOP_COUNT = 25

class SetStopUseCaseTest {

    private lateinit var stopRepository: StopRepository
    private lateinit var useCase: SetStopUseCase

    @Before
    fun setUp() {
        stopRepository = mockk()
        useCase = SetStopUseCase(stopRepository)
    }

    @Test
    fun `GIVEN a valid starting point WHEN invoke is called THEN repository upsertStartingPoint is called with trimmed name`() = runTest {
        val expectedStop = Stop("stop-1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 0, StopStatus.PENDING)
        coEvery { stopRepository.upsertStartingPoint(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.success(expectedStop)

        val result = useCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, StopType.STARTING_POINT)

        assertTrue(result.isSuccess)
        assertEquals(expectedStop, result.getOrThrow())
        coVerify(exactly = 1) { stopRepository.upsertStartingPoint(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE) }
    }

    @Test
    fun `GIVEN a valid destination WHEN invoke is called THEN repository upsertDestination is called with trimmed name`() = runTest {
        val expectedStop = Stop("stop-1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        coEvery { stopRepository.upsertDestination(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.success(expectedStop)

        val result = useCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, StopType.DESTINATION)

        assertTrue(result.isSuccess)
        assertEquals(expectedStop, result.getOrThrow())
        coVerify(exactly = 1) { stopRepository.upsertDestination(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE) }
    }

    @Test
    fun `GIVEN a valid intermediate stop below limit WHEN invoke is called THEN repository addIntermediateStop is called with trimmed name`() = runTest {
        val expectedStop = Stop("stop-1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 2, StopStatus.PENDING)
        val expectedCount = 3
        coEvery { stopRepository.getStopCount(TRIP_ID) } returns expectedCount
        coEvery { stopRepository.addIntermediateStop(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.success(expectedStop)

        val result = useCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, StopType.INTERMEDIATE)

        assertTrue(result.isSuccess)
        assertEquals(expectedStop, result.getOrThrow())
        coVerify(exactly = 1) { stopRepository.addIntermediateStop(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE) }
    }

    @Test
    fun `GIVEN stop count at limit WHEN adding intermediate stop THEN result is failure with IllegalStateException`() = runTest {
        coEvery { stopRepository.getStopCount(TRIP_ID) } returns MAX_STOP_COUNT

        val result = useCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, StopType.INTERMEDIATE)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        coVerify(exactly = 0) { stopRepository.addIntermediateStop(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN repository returns failure for starting point WHEN invoke is called THEN failure is propagated`() = runTest {
        val exception = RuntimeException("DB error")
        coEvery { stopRepository.upsertStartingPoint(any(), any(), any(), any()) } returns Result.failure(exception)

        val result = useCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, StopType.STARTING_POINT)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `GIVEN repository returns failure for destination WHEN invoke is called THEN failure is propagated`() = runTest {
        val exception = RuntimeException("DB error")
        coEvery { stopRepository.upsertDestination(any(), any(), any(), any()) } returns Result.failure(exception)

        val result = useCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, StopType.DESTINATION)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `GIVEN repository returns failure for intermediate stop WHEN invoke is called THEN failure is propagated`() = runTest {
        val exception = RuntimeException("DB error")
        val expectedCount = 3
        coEvery { stopRepository.getStopCount(TRIP_ID) } returns expectedCount
        coEvery { stopRepository.addIntermediateStop(any(), any(), any(), any()) } returns Result.failure(exception)

        val result = useCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, StopType.INTERMEDIATE)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `GIVEN place name with whitespace WHEN invoke is called for each stop type THEN name is trimmed`() = runTest {
        val paddedName = "  $PLACE_NAME  "
        val startStop = Stop("s1", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 0, StopStatus.PENDING)
        val destStop = Stop("s2", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 1, StopStatus.PENDING)
        val interStop = Stop("s3", TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE, 2, StopStatus.PENDING)
        coEvery { stopRepository.upsertStartingPoint(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.success(startStop)
        coEvery { stopRepository.upsertDestination(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.success(destStop)
        coEvery { stopRepository.getStopCount(TRIP_ID) } returns 3
        coEvery { stopRepository.addIntermediateStop(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.success(interStop)

        useCase(TRIP_ID, paddedName, LATITUDE, LONGITUDE, StopType.STARTING_POINT)
        useCase(TRIP_ID, paddedName, LATITUDE, LONGITUDE, StopType.DESTINATION)
        useCase(TRIP_ID, paddedName, LATITUDE, LONGITUDE, StopType.INTERMEDIATE)

        val expectedTrimmedName = PLACE_NAME
        coVerify { stopRepository.upsertStartingPoint(TRIP_ID, expectedTrimmedName, LATITUDE, LONGITUDE) }
        coVerify { stopRepository.upsertDestination(TRIP_ID, expectedTrimmedName, LATITUDE, LONGITUDE) }
        coVerify { stopRepository.addIntermediateStop(TRIP_ID, expectedTrimmedName, LATITUDE, LONGITUDE) }
    }
}
