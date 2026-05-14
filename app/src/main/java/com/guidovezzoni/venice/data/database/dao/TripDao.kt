package com.guidovezzoni.venice.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.guidovezzoni.venice.data.database.entity.TripEntity
import com.guidovezzoni.venice.data.database.entity.TripWithStopCount
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert
    suspend fun insert(trip: TripEntity)

    @Query(
        """
        SELECT t.*, (SELECT COUNT(*) FROM stops s WHERE s.tripId = t.id) AS stopCount
        FROM trips t
        ORDER BY t.createdAt DESC
        """
    )
    fun observeAll(): Flow<List<TripWithStopCount>>

    @Query(
        """
        SELECT t.*, (SELECT COUNT(*) FROM stops s WHERE s.tripId = t.id) AS stopCount
        FROM trips t
        WHERE t.id = :id
        """
    )
    suspend fun getById(id: String): TripWithStopCount?
}
