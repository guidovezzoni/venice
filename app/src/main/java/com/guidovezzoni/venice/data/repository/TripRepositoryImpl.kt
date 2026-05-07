package com.guidovezzoni.venice.data.repository

import com.guidovezzoni.venice.data.database.dao.TripDao
import com.guidovezzoni.venice.data.database.entity.TripEntity
import com.guidovezzoni.venice.data.database.mapper.toDomain
import com.guidovezzoni.venice.domain.model.Trip
import com.guidovezzoni.venice.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class TripRepositoryImpl @Inject constructor(
    private val tripDao: TripDao,
) : TripRepository {

    override suspend fun createTrip(name: String): Result<Trip> = runCatching {
        val now = System.currentTimeMillis()
        val entity = TripEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            createdAt = now,
            updatedAt = now,
        )
        tripDao.insert(entity)
        entity.toDomain()
    }

    override fun observeTrips(): Flow<List<Trip>> =
        tripDao.observeAll().map { entities -> entities.map { it.toDomain() } }
}
