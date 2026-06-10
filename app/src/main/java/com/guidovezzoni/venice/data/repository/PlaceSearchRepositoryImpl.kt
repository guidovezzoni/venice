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

private val PLACE_DETAIL_FIELDS = listOf(Place.Field.DISPLAY_NAME, Place.Field.LOCATION)

@Singleton
class PlaceSearchRepositoryImpl @Inject constructor(
    private val placesClient: PlacesClient,
    private val suggestionMapper: PlaceSuggestionMapper,
    private val detailMapper: PlaceDetailMapper,
) : PlaceSearchRepository {

    /*
    The session token lifecycle in this implementation follows the correct Google Places API billing pattern
    1.  Created lazily — on the first getAutocompleteSuggestions call, a new AutocompleteSessionToken is created (line 33-35).
    2. Reused across keystrokes — subsequent autocomplete calls reuse the same token, grouping them into one billing session (line 38).
    3. Consumed on place selection — getPlaceDetails attaches the token to the fetch request (line 48), then nulls it (line 52).
       This tells Google "this autocomplete session ended with a selection," and you're billed for one session instead of per-keystroke.
    4. Reset on cancellation — resetSession() clears the token when the user dismisses without selecting, so the next search starts a fresh session.

    Thread safety — sessionToken is a mutable var with no synchronization. If two coroutines called getAutocompleteSuggestions concurrently, they
    could race on the null check and create two tokens. In practice this is fine because the ViewModel likely serializes calls, but if you ever
    wanted to harden it, you could use a Mutex or ensure single-threaded access via the dispatcher. Not urgent for this codebase.
    */
    private var sessionToken: AutocompleteSessionToken? = null

    override suspend fun getAutocompleteSuggestions(query: String): Result<List<PlaceSuggestion>> {
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
            val response = placesClient.findAutocompletePredictions(request).await()
            response.autocompletePredictions.map { suggestionMapper.map(it) }
        }
    }

    override suspend fun getPlaceDetails(placeId: String): Result<PlaceDetail> {
        return runCatching {
            val request = FetchPlaceRequest.builder(placeId, PLACE_DETAIL_FIELDS)
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
