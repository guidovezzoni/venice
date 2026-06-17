package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.model.StopType
import com.guidovezzoni.venice.domain.repository.StopRepository
import javax.inject.Inject

private const val MAX_STOP_COUNT = 25

class SetStopUseCase @Inject constructor(
    private val stopRepository: StopRepository,
    private val invalidateRouteUseCase: InvalidateRouteUseCase,
) {
    suspend operator fun invoke(
        tripId: String,
        placeName: String,
        latitude: Double,
        longitude: Double,
        stopType: StopType,
    ): Result<Stop> {
        val trimmedName = placeName.trim()
        val result = when (stopType) {
            StopType.STARTING_POINT ->
                stopRepository.upsertStartingPoint(tripId, trimmedName, latitude, longitude)

            StopType.DESTINATION ->
                stopRepository.upsertDestination(tripId, trimmedName, latitude, longitude)

            StopType.INTERMEDIATE -> {
                val currentCount = stopRepository.getStopCount(tripId)
                if (currentCount >= MAX_STOP_COUNT) {
                    Result.failure(IllegalStateException("Maximum of $MAX_STOP_COUNT stops reached"))
                } else {
                    stopRepository.addIntermediateStop(tripId, trimmedName, latitude, longitude)
                }
            }
        }
        if (result.isSuccess) {
            invalidateRouteUseCase(tripId)
        }
        return result
    }
}
