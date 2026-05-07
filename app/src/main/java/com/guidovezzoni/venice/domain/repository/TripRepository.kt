package com.guidovezzoni.venice.domain.repository

import com.guidovezzoni.venice.domain.model.Trip
import kotlinx.coroutines.flow.Flow

interface TripRepository {
    suspend fun createTrip(name: String): Result<Trip>
    fun observeTrips(): Flow<List<Trip>>
}
