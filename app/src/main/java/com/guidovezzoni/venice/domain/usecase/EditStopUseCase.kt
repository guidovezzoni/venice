package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.repository.StopRepository
import javax.inject.Inject

class EditStopUseCase @Inject constructor(
    private val stopRepository: StopRepository,
    private val invalidateRouteUseCase: InvalidateRouteUseCase,
) {
    suspend operator fun invoke(
        stopId: String,
        placeName: String,
        latitude: Double,
        longitude: Double,
    ): Result<Stop> {
        val trimmedName = placeName.trim()
        val result = stopRepository.updateStop(stopId, trimmedName, latitude, longitude)
        result.onSuccess { stop ->
            invalidateRouteUseCase(stop.tripId)
        }
        return result
    }
}
