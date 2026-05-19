package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.repository.StopRepository
import javax.inject.Inject

// TODO: Consolidate with SetStartingPointUseCase into a single SetStopUseCase when StopType is introduced (story 1.2.3.1)
class SetDestinationUseCase @Inject constructor(
    private val stopRepository: StopRepository,
) {
    suspend operator fun invoke(
        tripId: String,
        placeName: String,
        latitude: Double,
        longitude: Double,
    ): Result<Stop> =
        stopRepository.upsertDestination(tripId, placeName.trim(), latitude, longitude)
}
