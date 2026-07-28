package com.example.mooruckapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mooruckapp.data.local.entity.User
import kotlinx.coroutines.flow.Flow

/**
 * User 테이블 접근용 DAO(Data Access Object)
 *
 * 사용 순서
 * 1. 앱 시작 시 [insertIfNotExists]로 기본 row 보장
 * 2. 화면에서는 [observeUser]로 구독해서 닉네임/알림 상태 실시간 표시
 * 3. 마이페이지에서 닉네임/알림 설정을 바꾸면 [updateNickname] / [updateNotificationEnabled] 호출
 */
@Dao
interface UserDao {

    /** 마이페이지 화면에서 구독용으로 사용 (설정이 바뀌면 자동 갱신) */
    @Query("SELECT * FROM user WHERE id = :userId LIMIT 1")
    fun observeUser(userId: Long = User.DEFAULT_USER_ID): Flow<User?>

    /** 알림 워커(WorkManager)처럼 1회성으로 값만 필요할 때 사용 */
    @Query("SELECT * FROM user WHERE id = :userId LIMIT 1")
    suspend fun getUser(userId: Long = User.DEFAULT_USER_ID): User?

    /** 이미 row가 있으면 동작 X (OnConflictStrategy.IGNORE) — 앱 최초 실행 시 1회 호출 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(user: User)

    @Query("UPDATE user SET nickname = :nickname WHERE id = :userId")
    suspend fun updateNickname(nickname: String, userId: Long = User.DEFAULT_USER_ID)

    @Query("UPDATE user SET watering_notification_enabled = :enabled WHERE id = :userId")
    suspend fun updateNotificationEnabled(enabled: Boolean, userId: Long = User.DEFAULT_USER_ID)
}
