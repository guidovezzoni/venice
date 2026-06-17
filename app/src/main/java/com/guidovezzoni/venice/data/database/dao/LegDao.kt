package com.guidovezzoni.venice.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.guidovezzoni.venice.data.database.entity.LegEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LegDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(legs: List<LegEntity>)

    @Query("DELETE FROM legs WHERE tripId = :tripId")
    suspend fun deleteByTripId(tripId: String)

    @Query("SELECT * FROM legs WHERE tripId = :tripId")
    fun observeByTripId(tripId: String): Flow<List<LegEntity>>

    @Query("SELECT * FROM legs WHERE tripId = :tripId")
    suspend fun getByTripId(tripId: String): List<LegEntity>
}
