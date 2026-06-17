package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.repository.RouteRepository
import javax.inject.Inject

class InvalidateRouteUseCase @Inject constructor(
    private val routeRepository: RouteRepository,
) {

    suspend operator fun invoke(tripId: String): Result<Unit> =
        routeRepository.deleteLegsForTrip(tripId)
}
