package com.example.mooruckapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 성장 일지 Entity 초안
 */
@Entity(
    tableName = "growth_diary",
    foreignKeys = [
        ForeignKey(
            entity = UserPlant::class,
            parentColumns = ["id"],
            childColumns = ["user_plant_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("user_plant_id")],
)
data class GrowthDiary(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "user_plant_id")
    val userPlantId: Long,

    /** 일지 날짜 (epoch millis, 자정 기준) */
    @ColumnInfo(name = "diary_date")
    val diaryDate: Long,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
)
