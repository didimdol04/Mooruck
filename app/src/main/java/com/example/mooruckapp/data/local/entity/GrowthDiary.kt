package com.example.mooruckapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "growth_diary",
    foreignKeys = [
        ForeignKey(
            entity = UserPlant::class,
            parentColumns = ["id"],
            childColumns = ["user_plant_id"],
            onDelete = ForeignKey.Companion.CASCADE
        )
    ],
    indices = [Index(value = ["user_plant_id"])]
)
data class GrowthDiary(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 어느 식물의 일지인지
    @ColumnInfo(name = "user_plant_id")
    val userPlantId: Long,

    // 일지 작성 날짜
    val diaryDate: Long,

    // 글 내용
    val content: String,

    // 사진 경로
    val imageUrl: String = "",

    // 생성 / 수정 시각
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)