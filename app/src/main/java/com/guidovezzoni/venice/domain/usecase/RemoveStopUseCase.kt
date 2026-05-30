package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.repository.StopRepository
import javax.inject.Inject

class RemoveStopUseCase @Inject constructor(
    private val stopRepository: StopRepository,
) {
    // TODO: invalidate affected legs once leg tracking is implemented (Epic 3)
    suspend operator fun invoke(tripId: String, stopId: String): Result<Unit> =
        stopRepository.deleteStop(tripId, stopId)
}
