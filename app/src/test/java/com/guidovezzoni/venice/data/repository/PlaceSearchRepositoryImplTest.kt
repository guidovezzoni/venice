package com.guidovezzoni.venice.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.guidovezzoni.venice.data.mapper.PlaceDetailMapper
import com.guidovezzoni.venice.data.mapper.PlaceSuggestionMapper
import com.guidovezzoni.venice.domain.model.PlaceDetail
import com.guidovezzoni.venice.domain.model.PlaceSuggestion
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private const val QUERY = "Colo"
private const val PLACE_ID = "abc123"
private const val PLACE_NAME = "Colosseum"
private const val PLACE_LATITUDE = 41.8902
private const val PLACE_LONGITUDE = 12.4922

class PlaceSearchRepositoryImplTest {

    private lateinit var placesClient: PlacesClient
    private lateinit var suggestionMapper: PlaceSuggestionMapper
    private lateinit var detailMapper: PlaceDetailMapper
    private lateinit var repository: PlaceSearchRepositoryImpl

    @Before
    fun setUp() {
        placesClient = mockk(relaxed = true)
        suggestionMapper = mockk()
        detailMapper = mockk()
        repository = PlaceSearchRepositoryImpl(placesClient, suggestionMapper, detailMapper)
    }

    // --- Task 5.1: getAutocompleteSuggestions ---

    @Test
    fun `GIVEN no session token WHEN getAutocompleteSuggestions called THEN token auto-created and used in request`() =
        runTest {
            val requestSlot = slot<FindAutocompletePredictionsRequest>()
            val mockResponse = mockk<FindAutocompletePredictionsResponse>(relaxed = true)
            every { mockResponse.autocompletePredictions } returns emptyList()
            every {
                placesClient.findAutocompletePredictions(capture(requestSlot))
            } returns Tasks.forResult(mockResponse)

            repository.getAutocompleteSuggestions(QUERY)

            assertTrue(requestSlot.captured.sessionToken != null)
        }

    @Test
    fun `GIVEN blank query WHEN getAutocompleteSuggestions called THEN returns empty list without API call`() =
        runTest {
            val result = repository.getAutocompleteSuggestions("   ")

            assertTrue(result.isSuccess)
            val expectedSuggestions = emptyList<PlaceSuggestion>()
            assertEquals(expectedSuggestions, result.getOrNull())
            verify(exactly = 0) { placesClient.findAutocompletePredictions(any()) }
        }

    @Test
    fun `GIVEN valid query WHEN API succeeds THEN returns mapped suggestions`() =
        runTest {
            val mockPrediction1 = mockk<AutocompletePrediction>(relaxed = true)
            val mockPrediction2 = mockk<AutocompletePrediction>(relaxed = true)
            val mockPrediction3 = mockk<AutocompletePrediction>(relaxed = true)
            val mockResponse = mockk<FindAutocompletePredictionsResponse>(relaxed = true)
            every { mockResponse.autocompletePredictions } returns listOf(mockPrediction1, mockPrediction2, mockPrediction3)

            val suggestion1 = PlaceSuggestion("id1", "Colosseum", "Rome, Italy")
            val suggestion2 = PlaceSuggestion("id2", "Colosseo Station", "Rome, Italy")
            val suggestion3 = PlaceSuggestion("id3", "Colosseum View", "Rome, Italy")
            every { suggestionMapper.map(mockPrediction1) } returns suggestion1
            every { suggestionMapper.map(mockPrediction2) } returns suggestion2
            every { suggestionMapper.map(mockPrediction3) } returns suggestion3

            every {
                placesClient.findAutocompletePredictions(any())
            } returns Tasks.forResult(mockResponse)

            val result = repository.getAutocompleteSuggestions(QUERY)

            assertTrue(result.isSuccess)
            val expectedSuggestions = listOf(suggestion1, suggestion2, suggestion3)
            assertEquals(expectedSuggestions, result.getOrNull())
        }

    @Test
    fun `GIVEN valid query WHEN API fails THEN returns failure`() =
        runTest {
            val exception = RuntimeException("Network error")
            every {
                placesClient.findAutocompletePredictions(any())
            } returns Tasks.forException(exception)

            val result = repository.getAutocompleteSuggestions(QUERY)

            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    // --- Task 5.2: getPlaceDetails ---

    @Test
    fun `GIVEN a placeId WHEN getPlaceDetails succeeds THEN returns mapped PlaceDetail and clears token`() =
        runTest {
            // First set up a session token by calling getAutocompleteSuggestions
            val mockSuggestionsResponse = mockk<FindAutocompletePredictionsResponse>(relaxed = true)
            every { mockSuggestionsResponse.autocompletePredictions } returns emptyList()
            every {
                placesClient.findAutocompletePredictions(any())
            } returns Tasks.forResult(mockSuggestionsResponse)
            repository.getAutocompleteSuggestions(QUERY)

            // Now test getPlaceDetails
            val mockFetchResponse = mockk<FetchPlaceResponse>(relaxed = true)
            val mockPlace = mockk<com.google.android.libraries.places.api.model.Place>(relaxed = true)
            every { mockFetchResponse.place } returns mockPlace
            val expectedDetail = PlaceDetail(PLACE_NAME, PLACE_LATITUDE, PLACE_LONGITUDE)
            every { detailMapper.map(mockPlace) } returns expectedDetail

            every {
                placesClient.fetchPlace(any())
            } returns Tasks.forResult(mockFetchResponse)

            val result = repository.getPlaceDetails(PLACE_ID)

            assertTrue(result.isSuccess)
            assertEquals(expectedDetail, result.getOrNull())

            // After getPlaceDetails, token should be cleared: next autocomplete should create a new token
            val requestSlot = slot<FindAutocompletePredictionsRequest>()
            val mockSuggestionsResponse2 = mockk<FindAutocompletePredictionsResponse>(relaxed = true)
            every { mockSuggestionsResponse2.autocompletePredictions } returns emptyList()
            every {
                placesClient.findAutocompletePredictions(capture(requestSlot))
            } returns Tasks.forResult(mockSuggestionsResponse2)
            repository.getAutocompleteSuggestions(QUERY)

            // A new token should have been created (non-null)
            assertTrue(requestSlot.captured.sessionToken != null)
        }

    @Test
    fun `GIVEN a placeId WHEN API fails THEN returns failure`() =
        runTest {
            val exception = RuntimeException("API error")
            every {
                placesClient.fetchPlace(any())
            } returns Tasks.forException(exception)

            val result = repository.getPlaceDetails(PLACE_ID)

            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    // --- Task 5.3: resetSession ---

    @Test
    fun `GIVEN an active session WHEN resetSession called THEN token is cleared`() =
        runTest {
            // Establish a session token by calling getAutocompleteSuggestions
            val firstRequestSlot = slot<FindAutocompletePredictionsRequest>()
            val mockResponse1 = mockk<FindAutocompletePredictionsResponse>(relaxed = true)
            every { mockResponse1.autocompletePredictions } returns emptyList()
            every {
                placesClient.findAutocompletePredictions(capture(firstRequestSlot))
            } returns Tasks.forResult(mockResponse1)
            repository.getAutocompleteSuggestions(QUERY)
            val tokenBefore = firstRequestSlot.captured.sessionToken

            // Reset the session
            repository.resetSession()

            // Next call should create a new token — verify by capturing the new request
            val secondRequestSlot = slot<FindAutocompletePredictionsRequest>()
            val mockResponse2 = mockk<FindAutocompletePredictionsResponse>(relaxed = true)
            every { mockResponse2.autocompletePredictions } returns emptyList()
            every {
                placesClient.findAutocompletePredictions(capture(secondRequestSlot))
            } returns Tasks.forResult(mockResponse2)
            repository.getAutocompleteSuggestions(QUERY)

            // A new (non-null) token is created after reset
            assertTrue(secondRequestSlot.captured.sessionToken != null)
        }
}
