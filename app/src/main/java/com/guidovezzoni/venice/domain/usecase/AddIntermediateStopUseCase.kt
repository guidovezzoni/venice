package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.repository.StopRepository
import javax.inject.Inject

private const val MAX_STOP_COUNT = 25

class AddIntermediateStopUseCase @Inject constructor(
    private val stopRepository: StopRepository,
) {
    suspend operator fun invoke(
        tripId: String,
        placeName: String,
        latitude: Double,
        longitude: Double,
    ): Result<Stop> {
        val currentCount = stopRepository.getStopCount(tripId)
        if (currentCount >= MAX_STOP_COUNT) {
            return Result.failure(IllegalStateException("Maximum of $MAX_STOP_COUNT stops reached"))
        }
        return stopRepository.addIntermediateStop(tripId, placeName.trim(), latitude, longitude)
    }
}
