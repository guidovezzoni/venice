package com.guidovezzoni.venice.domain.repository

import com.guidovezzoni.venice.domain.model.PlaceDetail
import com.guidovezzoni.venice.domain.model.PlaceSuggestion

interface PlaceSearchRepository {
    suspend fun getAutocompleteSuggestions(query: String): Result<List<PlaceSuggestion>>

    suspend fun getPlaceDetails(placeId: String): Result<PlaceDetail>

    fun resetSession()
}
