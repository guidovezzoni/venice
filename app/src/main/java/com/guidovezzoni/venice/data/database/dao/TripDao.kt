package com.guidovezzoni.venice.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.guidovezzoni.venice.data.database.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert
    suspend fun insert(trip: TripEntity)

    @Query("SELECT * FROM trips ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getById(id: String): TripEntity?
}
