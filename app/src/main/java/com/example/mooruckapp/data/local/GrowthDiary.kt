package com.example.mooruckapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "growth_diary")
data class GrowthDiary(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 어느 식물의 일지인지
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