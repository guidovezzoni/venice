package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.PlaceDetail
import com.guidovezzoni.venice.domain.repository.PlaceSearchRepository
import javax.inject.Inject

class GetPlaceDetailUseCase @Inject constructor(
    private val repository: PlaceSearchRepository,
) {
    suspend operator fun invoke(placeId: String): Result<PlaceDetail> =
        repository.getPlaceDetails(placeId)
}
