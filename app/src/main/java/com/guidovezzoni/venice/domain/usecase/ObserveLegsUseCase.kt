package com.guidovezzoni.venice.domain.usecase

import com.guidovezzoni.venice.domain.model.Leg
import com.guidovezzoni.venice.domain.repository.RouteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveLegsUseCase @Inject constructor(
    private val routeRepository: RouteRepository,
) {
    operator fun invoke(tripId: String): Flow<List<Leg>> =
        routeRepository.observeLegsForTrip(tripId)
}
