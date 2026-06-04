package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.StopStatus
import com.guidovezzoni.venice.domain.repository.StopRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UndoMarkStopDepartedUseCase @Inject constructor(
    private val stopRepository: StopRepository,
) {
    suspend operator fun invoke(tripId: String): Result<Unit> {
        val stops = stopRepository.observeStopsForTrip(tripId).first()
        val lastDeparted = stops.filter { it.status == StopStatus.VISITED }.maxByOrNull { it.order }
            ?: return Result.failure(IllegalStateException("No departed stop to undo"))

        return stopRepository.updateStopStatus(lastDeparted.id, StopStatus.PENDING)
    }
}
