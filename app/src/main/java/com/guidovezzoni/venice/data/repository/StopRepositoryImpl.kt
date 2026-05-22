package com.guidovezzoni.venice.data.repository

import com.guidovezzoni.venice.data.database.dao.StopDao
import com.guidovezzoni.venice.data.database.entity.StopEntity
import com.guidovezzoni.venice.data.database.mapper.toDomain
import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.model.StopStatus
import com.guidovezzoni.venice.domain.repository.StopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

private const val STARTING_POINT_ORDER = 0
private const val DESTINATION_ORDER = 1

class StopRepositoryImpl @Inject constructor(
    private val stopDao: StopDao,
) : StopRepository {

    override suspend fun upsertStartingPoint(
        tripId: String,
        placeName: String,
        latitude: Double,
        longitude: Double,
    ): Result<Stop> = runCatching {
        upsertStop(tripId, placeName, latitude, longitude, STARTING_POINT_ORDER) {
            stopDao.getStartingPoint(tripId)
        }
    }

    override suspend fun upsertDestination(
        tripId: String,
        placeName: String,
        latitude: Double,
        longitude: Double,
    ): Result<Stop> = runCatching {
        upsertStop(tripId, placeName, latitude, longitude, DESTINATION_ORDER) {
            stopDao.getDestination(tripId)
        }
    }

    override fun observeStopsForTrip(tripId: String): Flow<List<Stop>> =
        stopDao.observeByTripId(tripId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun addIntermediateStop(
        tripId: String,
        placeName: String,
        latitude: Double,
        longitude: Double,
    ): Result<Stop> = runCatching {
        val entity = StopEntity(
            id = UUID.randomUUID().toString(),
            tripId = tripId,
            placeName = placeName,
            latitude = latitude,
            longitude = longitude,
            order = 0,
            status = StopStatus.PENDING.name,
        )
        val insertOrder = stopDao.shiftAndInsertStop(tripId, entity)
        entity.copy(order = insertOrder).toDomain()
    }

    override suspend fun getStopCount(tripId: String): Int =
        stopDao.getStopCount(tripId)

    override suspend fun swapStopOrder(
        tripId: String,
        fromOrder: Int,
        toOrder: Int,
    ): Result<Unit> = runCatching {
        stopDao.swapStopOrders(tripId, fromOrder, toOrder)
    }

    private suspend fun upsertStop(
        tripId: String,
        placeName: String,
        latitude: Double,
        longitude: Double,
        order: Int,
        findExisting: suspend () -> StopEntity?,
    ): Stop {
        val existing = findExisting()
        val entity = if (existing != null) {
            existing.copy(
                placeName = placeName,
                latitude = latitude,
                longitude = longitude,
            ).also { stopDao.update(it) }
        } else {
            StopEntity(
                id = UUID.randomUUID().toString(),
                tripId = tripId,
                placeName = placeName,
                latitude = latitude,
                longitude = longitude,
                order = order,
                status = StopStatus.PENDING.name,
            ).also { stopDao.insert(it) }
        }
        return entity.toDomain()
    }
}
