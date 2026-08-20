package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.model.StopStatus
import com.guidovezzoni.venice.domain.repository.StopRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Reverts the most recently departed stop back to pending.
 *
 * Returns the reverted [Stop] on success, constructed locally from the stop loaded before the update.
 * This is an acceptable trade-off since [StopRepository.updateStopStatus] only changes `status` —
 * an implicit contract verified by its signature. If that contract ever widens, switch to a
 * repository read-back.
 */
class UndoMarkStopDepartedUseCase @Inject constructor(
    private val stopRepository: StopRepository,
) {
    suspend operator fun invoke(tripId: String): Result<Stop> {
        val stops = stopRepository.observeStopsForTrip(tripId).first()
        val lastDeparted = stops.filter { it.status == StopStatus.VISITED }.maxByOrNull { it.order }
            ?: return Result.failure(IllegalStateException("No departed stop to undo"))

        return stopRepository.updateStopStatus(lastDeparted.id, StopStatus.PENDING)
            .map { lastDeparted.copy(status = StopStatus.PENDING) }
    }
}
