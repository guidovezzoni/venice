package com.guidovezzoni.venice.domain.repository

import com.guidovezzoni.venice.domain.model.Stop
import com.guidovezzoni.venice.domain.model.StopStatus
import kotlinx.coroutines.flow.Flow

interface StopRepository {
    suspend fun upsertStartingPoint(
        tripId: String,
        placeName: String,
        latitude: Double,
        longitude: Double,
    ): Result<Stop>

    suspend fun upsertDestination(
        tripId: String,
        placeName: String,
        latitude: Double,
        longitude: Double,
    ): Result<Stop>

    fun observeStopsForTrip(tripId: String): Flow<List<Stop>>

    suspend fun addIntermediateStop(
        tripId: String,
        placeName: String,
        latitude: Double,
        longitude: Double,
    ): Result<Stop>

    suspend fun getStopCount(tripId: String): Int

    suspend fun swapStopOrder(tripId: String, fromOrder: Int, toOrder: Int): Result<Unit>

    suspend fun updateStop(
        stopId: String,
        placeName: String,
        latitude: Double,
        longitude: Double,
    ): Result<Stop>

    suspend fun deleteStop(tripId: String, stopId: String): Result<Unit>

    suspend fun updateStopStatus(stopId: String, status: StopStatus): Result<Unit>
}
