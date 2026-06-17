package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.repository.RouteRepository
import javax.inject.Inject

private const val MINIMUM_STOP_COUNT = 2
private const val MINIMUM_STOP_COUNT_ERROR_MESSAGE = "At least 2 stops required to calculate a route"

class CalculateRouteUseCase @Inject constructor(
    private val routeRepository: RouteRepository,
) {

    suspend operator fun invoke(tripId: String, stops: List<Stop>): Result<Unit> {
        if (stops.size < MINIMUM_STOP_COUNT) {
            return Result.failure(IllegalStateException(MINIMUM_STOP_COUNT_ERROR_MESSAGE))
        }
        return routeRepository.calculateRoute(tripId, stops)
    }
}
