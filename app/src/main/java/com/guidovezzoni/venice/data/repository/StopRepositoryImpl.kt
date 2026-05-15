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

class StopRepositoryImpl @Inject constructor(
    private val stopDao: StopDao,
) : StopRepository {

    override suspend fun upsertStartingPoint(
        tripId: String,
        placeName: String,
        latitude: Double,
        longitude: Double,
    ): Result<Stop> = runCatching {
        val existing = stopDao.getStartingPoint(tripId)
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
                order = STARTING_POINT_ORDER,
                status = StopStatus.PENDING.name,
            ).also { stopDao.insert(it) }
        }
        entity.toDomain()
    }

    override fun observeStopsForTrip(tripId: String): Flow<List<Stop>> =
        stopDao.observeByTripId(tripId).map { entities -> entities.map { it.toDomain() } }
}
