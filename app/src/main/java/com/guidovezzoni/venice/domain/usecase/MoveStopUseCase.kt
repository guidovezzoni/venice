package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.repository.StopRepository
import javax.inject.Inject

class MoveStopUseCase @Inject constructor(
    private val stopRepository: StopRepository,
) {
    suspend operator fun invoke(
        tripId: String,
        fromOrder: Int,
        toOrder: Int,
    ): Result<Unit> = stopRepository.swapStopOrder(tripId, fromOrder, toOrder)
}
