package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.PlaceSuggestion
import com.guidovezzoni.venice.domain.repository.PlaceSearchRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val QUERY = "Colo"
private const val QUERY_WITH_SPACES = "  Rome  "
private const val TRIMMED_QUERY = "Rome"

class SearchPlacesUseCaseTest {

    private lateinit var repository: PlaceSearchRepository
    private lateinit var useCase: SearchPlacesUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = SearchPlacesUseCase(repository)
    }

    @Test
    fun `GIVEN query with leading and trailing spaces WHEN invoke is called THEN repository receives trimmed query`() =
        runTest(UnconfinedTestDispatcher()) {
            val expectedSuggestions = emptyList<PlaceSuggestion>()
            coEvery { repository.getAutocompleteSuggestions(TRIMMED_QUERY) } returns Result.success(expectedSuggestions)

            useCase(QUERY_WITH_SPACES)

            coVerify(exactly = 1) { repository.getAutocompleteSuggestions(TRIMMED_QUERY) }
        }

    @Test
    fun `GIVEN query WHEN repository returns success THEN Result success with suggestions is returned`() =
        runTest(UnconfinedTestDispatcher()) {
            val expectedSuggestions = listOf(
                PlaceSuggestion("id1", "Colosseum", "Rome, Italy"),
                PlaceSuggestion("id2", "Cologne", "Germany"),
            )
            coEvery { repository.getAutocompleteSuggestions(QUERY) } returns Result.success(expectedSuggestions)

            val result = useCase(QUERY)

            assertTrue(result.isSuccess)
            assertEquals(expectedSuggestions, result.getOrThrow())
        }

    @Test
    fun `GIVEN repository returns failure WHEN invoke is called THEN failure is propagated`() =
        runTest(UnconfinedTestDispatcher()) {
            val exception = RuntimeException("API error")
            coEvery { repository.getAutocompleteSuggestions(QUERY) } returns Result.failure(exception)

            val result = useCase(QUERY)

            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }
}
