package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.repository.StopRepository
import javax.inject.Inject

class MoveStopUseCase @Inject constructor(
    private val stopRepository: StopRepository,
    private val invalidateRouteUseCase: InvalidateRouteUseCase,
) {
    suspend operator fun invoke(
        tripId: String,
        fromOrder: Int,
        toOrder: Int,
    ): Result<Unit> {
        val result = stopRepository.swapStopOrder(tripId, fromOrder, toOrder)
        if (result.isSuccess) {
            invalidateRouteUseCase(tripId)
        }
        return result
    }
}
