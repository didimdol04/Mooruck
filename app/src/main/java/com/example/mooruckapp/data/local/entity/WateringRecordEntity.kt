package com.example.mooruckapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "watering_record",
    foreignKeys = [
        ForeignKey(
            entity = UserPlantEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_plant_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_plant_id"])
    ]
)
data class WateringRecordEntity(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "user_plant_id")
    val userPlantId: Long,

    @ColumnInfo(name = "watered_date")
    val wateredDate: String,

    @ColumnInfo(name = "created_at")
    val createdAt: String
)