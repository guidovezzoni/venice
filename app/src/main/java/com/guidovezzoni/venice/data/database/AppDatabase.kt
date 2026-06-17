package com.guidovezzoni.venice.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.guidovezzoni.venice.data.database.dao.LegDao
import com.guidovezzoni.venice.data.database.dao.StopDao
import com.guidovezzoni.venice.data.database.dao.TripDao
import com.guidovezzoni.venice.data.database.entity.LegEntity
import com.guidovezzoni.venice.data.database.entity.StopEntity
import com.guidovezzoni.venice.data.database.entity.TripEntity

@Database(entities = [TripEntity::class, StopEntity::class, LegEntity::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun stopDao(): StopDao
    abstract fun legDao(): LegDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS stops (
                        id TEXT NOT NULL PRIMARY KEY,
                        tripId TEXT NOT NULL,
                        placeName TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        `order` INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        FOREIGN KEY (tripId) REFERENCES trips(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_stops_tripId ON stops (tripId)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS legs (
                        id TEXT NOT NULL PRIMARY KEY,
                        tripId TEXT NOT NULL,
                        fromStopId TEXT NOT NULL,
                        toStopId TEXT NOT NULL,
                        distanceMetres INTEGER NOT NULL,
                        durationSeconds INTEGER NOT NULL,
                        encodedPolyline TEXT NOT NULL,
                        FOREIGN KEY (tripId) REFERENCES trips(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_legs_tripId ON legs (tripId)")
            }
        }
    }
}
