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
private const val PLACE_NAME = "Barcelona"
private const val LATITUDE = 41.3851
private const val LONGITUDE = 2.1734

class SetDestinationUseCaseTest {

    private lateinit var stopRepository: StopRepository
    private lateinit var useCase: SetDestinationUseCase

    @Before
    fun setUp() {
        stopRepository = mockk()
        useCase = SetDestinationUseCase(stopRepository)
    }

    @Test
    fun `GIVEN repository returns success WHEN invoke is called THEN Result success with the Stop is returned`() = runTest {
        val expectedStop = Stop(
            id = "stop-1",
            tripId = TRIP_ID,
            placeName = PLACE_NAME,
            latitude = LATITUDE,
            longitude = LONGITUDE,
            order = 1,
            status = StopStatus.PENDING,
        )
        coEvery { stopRepository.upsertDestination(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.success(expectedStop)

        val result = useCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE)

        assertTrue(result.isSuccess)
        assertEquals(expectedStop, result.getOrThrow())
    }

    @Test
    fun `GIVEN repository returns failure WHEN invoke is called THEN Result failure is returned`() = runTest {
        val exception = RuntimeException("DB error")
        coEvery { stopRepository.upsertDestination(any(), any(), any(), any()) } returns Result.failure(exception)

        val result = useCase(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `GIVEN a place name with leading and trailing spaces WHEN invoke is called THEN repository receives the trimmed name`() = runTest {
        val expectedStop = Stop(
            id = "stop-1",
            tripId = TRIP_ID,
            placeName = PLACE_NAME,
            latitude = LATITUDE,
            longitude = LONGITUDE,
            order = 1,
            status = StopStatus.PENDING,
        )
        coEvery { stopRepository.upsertDestination(TRIP_ID, PLACE_NAME, LATITUDE, LONGITUDE) } returns Result.success(expectedStop)

        useCase(TRIP_ID, "  $PLACE_NAME  ", LATITUDE, LONGITUDE)

        val expectedTrimmedName = PLACE_NAME
        coVerify { stopRepository.upsertDestination(TRIP_ID, expectedTrimmedName, LATITUDE, LONGITUDE) }
    }
}
