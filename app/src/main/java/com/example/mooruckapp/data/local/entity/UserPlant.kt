package com.example.mooruckapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_plant")
data class UserPlant(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "plant_name")
    val plantName: String,

    val nickname: String? = null,

    @ColumnInfo(name = "profile_image_uri")
    val profileImageUri: String? = null,

    val light: String,

    val humidity: String,

    val temperature: String,

    @ColumnInfo(name = "watering_interval_days")
    val wateringIntervalDays: Int,

    @ColumnInfo(name = "planted_date")
    val plantedDate: Long,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
)