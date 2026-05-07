package com.guidovezzoni.venice.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.guidovezzoni.venice.data.database.dao.TripDao
import com.guidovezzoni.venice.data.database.entity.TripEntity

@Database(entities = [TripEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
}
