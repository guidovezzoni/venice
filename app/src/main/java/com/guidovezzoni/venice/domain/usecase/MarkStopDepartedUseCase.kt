package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.StopStatus
import com.guidovezzoni.venice.domain.repository.StopRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class MarkStopDepartedUseCase @Inject constructor(
    private val stopRepository: StopRepository,
) {
    suspend operator fun invoke(tripId: String, stopId: String): Result<Unit> {
        val stops = stopRepository.observeStopsForTrip(tripId).first()
        val currentStop = stops.sortedBy { it.order }.firstOrNull { it.status == StopStatus.PENDING }
            ?: return Result.failure(IllegalStateException("Only the current stop can be marked as departed"))

        if (currentStop.id != stopId) {
            return Result.failure(IllegalStateException("Only the current stop can be marked as departed"))
        }

        return stopRepository.updateStopStatus(stopId, StopStatus.VISITED)
    }
}
