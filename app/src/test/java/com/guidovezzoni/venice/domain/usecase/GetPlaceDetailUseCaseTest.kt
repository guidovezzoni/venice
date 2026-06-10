package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.PlaceDetail
import com.guidovezzoni.venice.domain.repository.PlaceSearchRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val PLACE_ID = "abc"

class GetPlaceDetailUseCaseTest {

    private lateinit var repository: PlaceSearchRepository
    private lateinit var useCase: GetPlaceDetailUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetPlaceDetailUseCase(repository)
    }

    @Test
    fun `GIVEN a placeId WHEN invoke is called THEN repository getPlaceDetails is called with that placeId`() =
        runTest {
            val placeDetail = PlaceDetail("Colosseum", 41.8902, 12.4922)
            coEvery { repository.getPlaceDetails(PLACE_ID) } returns Result.success(placeDetail)

            useCase(PLACE_ID)

            coVerify(exactly = 1) { repository.getPlaceDetails(PLACE_ID) }
        }

    @Test
    fun `GIVEN repository returns success WHEN invoke is called THEN Result success with PlaceDetail is returned`() =
        runTest {
            val expectedDetail = PlaceDetail("Colosseum", 41.8902, 12.4922)
            coEvery { repository.getPlaceDetails(PLACE_ID) } returns Result.success(expectedDetail)

            val result = useCase(PLACE_ID)

            assertTrue(result.isSuccess)
            assertEquals(expectedDetail, result.getOrThrow())
        }

    @Test
    fun `GIVEN repository returns failure WHEN invoke is called THEN failure is propagated`() =
        runTest {
            val exception = RuntimeException("API error")
            coEvery { repository.getPlaceDetails(PLACE_ID) } returns Result.failure(exception)

            val result = useCase(PLACE_ID)

            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }
}
