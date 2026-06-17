package com.guidovezzoni.venice.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "legs",
    foreignKeys = [ForeignKey(
        entity = TripEntity::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("tripId")],
)
data class LegEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val fromStopId: String,
    val toStopId: String,
    val distanceMetres: Int,
    val durationSeconds: Int,
    val encodedPolyline: String,
)
