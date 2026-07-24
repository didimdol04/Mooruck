package com.example.mooruckapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 식물 등록 및 상세 Entity 초안
 * light_level / humidity / temperature / watering_interval_days 는
 * 농사로 API 검색 성공 시 API 값 / 실패 시 사용자 직접 입력 값
 */
@Entity(
    tableName = "user_plant",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("user_id")],
)
data class UserPlant(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long = User.DEFAULT_USER_ID,

    @ColumnInfo(name = "plant_name")
    val plantName: String,

    @ColumnInfo(name = "nickname")
    val nickname: String? = null,

    @ColumnInfo(name = "profile_image_url")
    val profileImageUrl: String? = null,

    @ColumnInfo(name = "light_level")
    val lightLevel: String? = null,

    @ColumnInfo(name = "humidity")
    val humidity: String? = null,

    @ColumnInfo(name = "temperature")
    val temperature: String? = null,

    @ColumnInfo(name = "watering_interval_days")
    val wateringIntervalDays: Int,

    /** 식물을 심은 날 (epoch millis, 자정 기준) —> 물주기 점수 계산의 기준일로 사용 */
    @ColumnInfo(name = "planted_date")
    val plantedDate: Long,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
