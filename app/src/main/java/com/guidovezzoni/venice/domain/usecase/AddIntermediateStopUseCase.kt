package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.repository.StopRepository
import javax.inject.Inject

private const val MAX_STOP_COUNT = 25
private const val MIN_LATITUDE = -90.0
private const val MAX_LATITUDE = 90.0
private const val MIN_LONGITUDE = -180.0
private const val MAX_LONGITUDE = 180.0

class AddIntermediateStopUseCase @Inject constructor(
    private val stopRepository: StopRepository,
) {
    suspend operator fun invoke(
        tripId: String,
        placeName: String,
        latitude: Double,
        longitude: Double,
    ): Result<Stop> {
        val trimmedName = placeName.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Place name must not be blank"))
        }
        if (latitude < MIN_LATITUDE || latitude > MAX_LATITUDE) {
            return Result.failure(IllegalArgumentException("Latitude must be between $MIN_LATITUDE and $MAX_LATITUDE"))
        }
        if (longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE) {
            return Result.failure(IllegalArgumentException("Longitude must be between $MIN_LONGITUDE and $MAX_LONGITUDE"))
        }
        val currentCount = stopRepository.getStopCount(tripId)
        if (currentCount >= MAX_STOP_COUNT) {
            return Result.failure(IllegalStateException("Maximum of $MAX_STOP_COUNT stops reached"))
        }
        return stopRepository.addIntermediateStop(tripId, trimmedName, latitude, longitude)
    }
}
