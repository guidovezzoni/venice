package com.guidovezzoni.venice.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.guidovezzoni.venice.data.database.entity.StopEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StopDao {
    @Query("SELECT * FROM stops WHERE tripId = :tripId AND `order` = 0 LIMIT 1")
    suspend fun getStartingPoint(tripId: String): StopEntity?

    @Insert
    suspend fun insert(stop: StopEntity)

    @Update
    suspend fun update(stop: StopEntity)

    @Query("SELECT * FROM stops WHERE tripId = :tripId AND `order` > 0 ORDER BY `order` DESC LIMIT 1")
    suspend fun getDestination(tripId: String): StopEntity?

    @Query("SELECT * FROM stops WHERE tripId = :tripId ORDER BY `order` ASC")
    fun observeByTripId(tripId: String): Flow<List<StopEntity>>

    @Query("UPDATE stops SET `order` = `order` + 1 WHERE tripId = :tripId AND `order` >= :fromOrder")
    suspend fun incrementOrderFrom(tripId: String, fromOrder: Int)

    @Query("SELECT COUNT(*) FROM stops WHERE tripId = :tripId")
    suspend fun getStopCount(tripId: String): Int
}
