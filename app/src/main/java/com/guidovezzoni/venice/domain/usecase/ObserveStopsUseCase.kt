package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.repository.StopRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveStopsUseCase @Inject constructor(
    private val stopRepository: StopRepository,
) {
    operator fun invoke(tripId: String): Flow<List<Stop>> =
        stopRepository.observeStopsForTrip(tripId)
}
