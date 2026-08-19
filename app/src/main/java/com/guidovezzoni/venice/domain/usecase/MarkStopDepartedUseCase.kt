package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.model.StopStatus
import com.guidovezzoni.venice.domain.repository.StopRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Marks the current pending stop as departed.
 *
 * Returns the departed [Stop] on success, constructed locally from the stop loaded before the update.
 * This is an acceptable trade-off since [StopRepository.updateStopStatus] only changes `status` —
 * an implicit contract verified by its signature. If that contract ever widens, switch to a
 * repository read-back.
 */
class MarkStopDepartedUseCase @Inject constructor(
    private val stopRepository: StopRepository,
) {
    suspend operator fun invoke(tripId: String, stopId: String): Result<Stop> {
        val stops = stopRepository.observeStopsForTrip(tripId).first()
        val currentStop = stops.sortedBy { it.order }.firstOrNull { it.status == StopStatus.PENDING }

        if (currentStop == null || currentStop.id != stopId) {
            return Result.failure(IllegalStateException("Only the current stop can be marked as departed"))
        }

        return stopRepository.updateStopStatus(stopId, StopStatus.VISITED)
            .map { currentStop.copy(status = StopStatus.VISITED) }
    }
}
