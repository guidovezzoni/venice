package com.guidovezzoni.venice.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stops",
    foreignKeys = [ForeignKey(
        entity = TripEntity::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("tripId")],
)
data class StopEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val placeName: String,
    val latitude: Double,
    val longitude: Double,
    val order: Int,
    val status: String,
)
