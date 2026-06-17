package com.guidovezzoni.venice.data.repository

import com.guidovezzoni.venice.data.database.dao.LegDao
import com.guidovezzoni.venice.data.database.mapper.toDomain
import com.guidovezzoni.venice.data.database.mapper.toEntity
import com.guidovezzoni.venice.data.mapper.toDomain
import com.guidovezzoni.venice.data.network.DirectionsApiService
import com.guidovezzoni.venice.domain.model.Leg
import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.repository.RouteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RouteRepositoryImpl @Inject constructor(
    private val directionsApiService: DirectionsApiService,
    private val legDao: LegDao,
) : RouteRepository {

    override suspend fun calculateRoute(tripId: String, stops: List<Stop>): Result<Unit> = runCatching {
        val response = directionsApiService.fetchRoute(stops).getOrThrow()
        val legs = response.legs.mapIndexed { index, directionsLeg ->
            directionsLeg.toDomain(
                tripId = tripId,
                fromStopId = stops[index].id,
                toStopId = stops[index + 1].id,
            )
        }
        legDao.deleteByTripId(tripId)
        legDao.insertAll(legs.map { it.toEntity() })
    }

    override suspend fun deleteLegsForTrip(tripId: String): Result<Unit> = runCatching {
        legDao.deleteByTripId(tripId)
    }

    override fun observeLegsForTrip(tripId: String): Flow<List<Leg>> =
        legDao.observeByTripId(tripId).map { entities -> entities.map { it.toDomain() } }
}
