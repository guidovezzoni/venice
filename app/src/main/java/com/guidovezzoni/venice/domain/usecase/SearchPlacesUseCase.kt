package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.PlaceSuggestion
import com.guidovezzoni.venice.domain.repository.PlaceSearchRepository
import javax.inject.Inject

class SearchPlacesUseCase @Inject constructor(
    private val repository: PlaceSearchRepository,
) {
    suspend operator fun invoke(query: String): Result<List<PlaceSuggestion>> =
        repository.getAutocompleteSuggestions(query.trim())
}
