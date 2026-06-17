package com.guidovezzoni.venice.domain.repository

import com.guidovezzoni.venice.domain.model.Leg
import com.guidovezzoni.venice.domain.model.Stop
import kotlinx.coroutines.flow.Flow

interface RouteRepository {
    suspend fun calculateRoute(tripId: String, stops: List<Stop>): Result<Unit>
    suspend fun deleteLegsForTrip(tripId: String): Result<Unit>
    fun observeLegsForTrip(tripId: String): Flow<List<Leg>>
}
