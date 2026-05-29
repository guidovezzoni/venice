package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.repository.StopRepository
import javax.inject.Inject

class EditStopUseCase @Inject constructor(
    private val stopRepository: StopRepository,
) {
    // TODO: invalidate affected legs once leg tracking is implemented (Epic 3)
    suspend operator fun invoke(
        stopId: String,
        placeName: String,
        latitude: Double,
        longitude: Double,
    ): Result<Stop> {
        val trimmedName = placeName.trim()
        return stopRepository.updateStop(stopId, trimmedName, latitude, longitude)
    }
}
