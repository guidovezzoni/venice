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

private const val TRIP_ID = "trip-1"
private const val PLACE_NAME = "Florence"
private const val LATITUDE = 43.77
private const val LONGITUDE = 11.25

class AddIntermediateStopUseCaseTest {

    private lateinit var stopRepository: StopRepository
    private lateinit var useCase: AddIntermediateStopUseCase

    @Before
    fun setUp() {
        stopRepository = mockk()
        useCase = AddIntermediateStopUseCase(stopRepository)
    }

    @Test
    fun `GIVEN valid inputs WHEN invoke is called THEN repository method is called with correct arguments`() = runTest {
        val expectedStop = Stop(
            id = "stop-1",
            tripId = TRIP_ID,
            placeName = PLACE_NAME,
            latitude = LATITUDE,
            longitude = LONGITUDE,
            order = 1,
            status = StopStatus.PENDING,
        )
        val expectedCount = 3
        coEvery { stopRepository.getStopCount(TRIP_ID) } returns expectedCount
        coEvery { stopRepository.addIntermediateStop(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.success(expectedStop)

        val result = useCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE)

        assertTrue(result.isSuccess)
        assertEquals(expectedStop, result.getOrThrow())
        coVerify(exactly = 1) { stopRepository.addIntermediateStop(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE) }
    }

    @Test
    fun `GIVEN stop count already at 25 WHEN invoke is called THEN returns failure`() = runTest {
        val maxStopCount = 25
        coEvery { stopRepository.getStopCount(TRIP_ID) } returns maxStopCount

        val result = useCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        coVerify(exactly = 0) { stopRepository.addIntermediateStop(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN place name with whitespace WHEN invoke is called THEN repository receives trimmed name`() = runTest {
        val expectedStop = Stop(
            id = "stop-1",
            tripId = TRIP_ID,
            placeName = PLACE_NAME,
            latitude = LATITUDE,
            longitude = LONGITUDE,
            order = 1,
            status = StopStatus.PENDING,
        )
        val expectedCount = 3
        coEvery { stopRepository.getStopCount(TRIP_ID) } returns expectedCount
        coEvery { stopRepository.addIntermediateStop(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.success(expectedStop)

        useCase(TRIP_ID, "  $PLACE_NAME  ", LATITUDE, LONGITUDE)

        val expectedTrimmedName = PLACE_NAME
        coVerify { stopRepository.addIntermediateStop(TRIP_ID, expectedTrimmedName, LATITUDE, LONGITUDE) }
    }

    @Test
    fun `GIVEN repository returns failure WHEN invoke is called THEN failure is propagated`() = runTest {
        val exception = RuntimeException("DB error")
        val expectedCount = 3
        coEvery { stopRepository.getStopCount(TRIP_ID) } returns expectedCount
        coEvery { stopRepository.addIntermediateStop(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.failure(exception)

        val result = useCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
}
