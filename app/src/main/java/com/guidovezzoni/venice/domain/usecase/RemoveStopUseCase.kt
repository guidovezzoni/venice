package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.repository.StopRepository
import javax.inject.Inject

class RemoveStopUseCase @Inject constructor(
    private val stopRepository: StopRepository,
    private val invalidateRouteUseCase: InvalidateRouteUseCase,
) {
    suspend operator fun invoke(tripId: String, stopId: String): Result<Unit> {
        val result = stopRepository.deleteStop(tripId, stopId)
        if (result.isSuccess) {
            invalidateRouteUseCase(tripId)
        }
        return result
    }
}
