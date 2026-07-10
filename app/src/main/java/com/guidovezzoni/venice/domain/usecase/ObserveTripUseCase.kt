package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.Trip
import com.guidovezzoni.venice.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveTripUseCase @Inject constructor(
    private val tripRepository: TripRepository,
) {
    operator fun invoke(tripId: String): Flow<Trip?> =
        tripRepository.observeTripById(tripId)
}
