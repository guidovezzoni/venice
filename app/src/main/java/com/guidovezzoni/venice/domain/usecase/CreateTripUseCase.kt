package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.Trip
import com.guidovezzoni.venice.domain.repository.TripRepository
import javax.inject.Inject

class CreateTripUseCase @Inject constructor(
    private val repository: TripRepository,
) {
    suspend operator fun invoke(name: String): Result<Trip> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Trip name must not be blank"))
        return repository.createTrip(name)
    }
}
