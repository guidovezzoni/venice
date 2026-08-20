package com.guidovezzoni.venice.data.repository

import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.guidovezzoni.venice.data.mapper.PlaceDetailMapper
import com.guidovezzoni.venice.data.mapper.PlaceSuggestionMapper
import com.guidovezzoni.venice.domain.model.PlaceDetail
import com.guidovezzoni.venice.domain.model.PlaceSuggestion
import com.guidovezzoni.venice.domain.repository.PlaceSearchRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private val PLACE_DETAIL_FIELDS =
    listOf(Place.Field.DISPLAY_NAME, Place.Field.LOCATION)

@Singleton
class PlaceSearchRepositoryImpl @Inject constructor(
    private val placesClient: PlacesClient,
    private val suggestionMapper: PlaceSuggestionMapper,
    private val detailMapper: PlaceDetailMapper,
) : PlaceSearchRepository {

    private var sessionToken: AutocompleteSessionToken? = null

    override suspend fun getAutocompleteSuggestions(
        query: String,
    ): Result<List<PlaceSuggestion>> {
        if (query.isBlank()) {
            return Result.success(emptyList())
        }
        return runCatching {
            if (sessionToken == null) {
                sessionToken = AutocompleteSessionToken.newInstance()
            }
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setSessionToken(sessionToken)
                .build()
            val response = placesClient
                .findAutocompletePredictions(request).await()
            response.autocompletePredictions
                .map { suggestionMapper.map(it) }
        }
    }

    override suspend fun getPlaceDetails(
        placeId: String,
    ): Result<PlaceDetail> {
        return runCatching {
            val request = FetchPlaceRequest
                .builder(placeId, PLACE_DETAIL_FIELDS)
                .setSessionToken(sessionToken)
                .build()
            val response = placesClient.fetchPlace(request).await()
            val placeDetail = detailMapper.map(response.place)
            sessionToken = null
            placeDetail
        }
    }

    override fun resetSession() {
        sessionToken = null
    }
}
