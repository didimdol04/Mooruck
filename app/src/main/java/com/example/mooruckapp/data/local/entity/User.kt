package com.example.mooruckapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * id = DEFAULT_USER_ID 고정
 * 앱 최초 실행 시 UserDao를 통해 이 row가 없으면 자동으로 생성
 *
 * 물주기 알림: 매일 오전 9시 발송
 */
@Entity(tableName = "user")
data class User(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long = DEFAULT_USER_ID,

    @ColumnInfo(name = "nickname")
    val nickname: String,

    @ColumnInfo(name = "watering_notification_enabled")
    val wateringNotificationEnabled: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val DEFAULT_USER_ID = 1L
    }
}
