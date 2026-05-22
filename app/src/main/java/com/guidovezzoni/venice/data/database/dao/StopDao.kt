package com.guidovezzoni.venice.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("SELECT * FROM stops WHERE tripId = :tripId AND `order` = :order LIMIT 1")
    suspend fun getStopByTripIdAndOrder(tripId: String, order: Int): StopEntity?

    @Query("UPDATE stops SET `order` = :newOrder WHERE id = :stopId")
    suspend fun updateStopOrder(stopId: String, newOrder: Int)

    @Transaction
    suspend fun shiftAndInsertStop(tripId: String, stop: StopEntity): Int {
        val destination = getDestination(tripId)
        val insertOrder = destination?.order ?: 1
        if (destination != null) {
            incrementOrderFrom(tripId, insertOrder)
        }
        insert(stop.copy(order = insertOrder))
        return insertOrder
    }

    @Transaction
    suspend fun swapStopOrders(
        tripId: String,
        fromOrder: Int,
        toOrder: Int,
    ) {
        val fromStop = getStopByTripIdAndOrder(tripId, fromOrder)
            ?: throw IllegalStateException("No stop found at order $fromOrder")
        val toStop = getStopByTripIdAndOrder(tripId, toOrder)
            ?: throw IllegalStateException("No stop found at order $toOrder")
        updateStopOrder(fromStop.id, toOrder)
        updateStopOrder(toStop.id, fromOrder)
    }
}
