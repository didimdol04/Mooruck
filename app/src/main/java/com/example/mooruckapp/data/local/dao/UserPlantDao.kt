package com.example.mooruckapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.mooruckapp.data.local.entity.UserPlant
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPlantDao {

    // 식물 등록
    @Insert
    suspend fun insert(userPlant: UserPlant): Long

    // 등록된 식물 전체 조회
    @Query("SELECT * FROM user_plant ORDER BY created_at DESC")
    fun observeAll(): Flow<List<UserPlant>>

    // 식물 ID로 상세 정보 실시간 조회
    @Query("SELECT * FROM user_plant WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<UserPlant?>

    // 전체 식물 한 번 조회 (성장 일지 필터용)
    @Query("SELECT * FROM user_plant ORDER BY created_at DESC")
    suspend fun getAllOnce(): List<UserPlant>

    // 식물 하나 조회 (프로필 표시용)
    @Query("SELECT * FROM user_plant WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: Long): UserPlant?

    // 식물 정보 수정
    @Update
    suspend fun update(userPlant: UserPlant)

    // 식물 삭제
    @Delete
    suspend fun delete(userPlant: UserPlant)
}