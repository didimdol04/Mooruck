package com.example.mooruckapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 메인 화면 물주기 버튼, 물주기 알림, 마이페이지 물주기 점수가 공통으로 사용하는 테이블
 * 물주기 기록 Entity 초안
 * "마지막으로 물 준 날" = 특정 식물의 watered_date 중 가장 최신 값
 */
@Entity(
    tableName = "watering_record",
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
data class WateringRecord(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "user_plant_id")
    val userPlantId: Long,

    /** 물 준 날 (epoch millis, 자정 기준) */
    @ColumnInfo(name = "watered_date")
    val wateredDate: Long,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)