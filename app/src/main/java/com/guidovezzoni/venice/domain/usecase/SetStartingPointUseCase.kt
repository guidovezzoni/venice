package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.repository.StopRepository
import javax.inject.Inject

class SetStartingPointUseCase @Inject constructor(
    private val stopRepository: StopRepository,
) {
    suspend operator fun invoke(
        tripId: String,
        placeName: String,
        latitude: Double,
        longitude: Double,
    ): Result<Stop> =
        stopRepository.upsertStartingPoint(tripId, placeName.trim(), latitude, longitude)
}
