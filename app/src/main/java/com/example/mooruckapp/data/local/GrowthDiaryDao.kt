package com.example.mooruckapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface GrowthDiaryDao {

    // 전체 일지 조회 (최신순)
    @Query("SELECT * FROM growth_diary ORDER BY diaryDate DESC")
    suspend fun getAll(): List<GrowthDiary>

    // 특정 식물의 일지만 조회
    @Query("SELECT * FROM growth_diary WHERE userPlantId = :plantId ORDER BY diaryDate DESC")
    suspend fun getByPlant(plantId: Long): List<GrowthDiary>

    // 일지 하나 조회
    @Query("SELECT * FROM growth_diary WHERE id = :diaryId")
    suspend fun getById(diaryId: Long): GrowthDiary?

    // 일지 저장
    @Insert
    suspend fun insert(diary: GrowthDiary)

    // 일지 수정
    @Update
    suspend fun update(diary: GrowthDiary)

    // 일지 삭제
    @Delete
    suspend fun delete(diary: GrowthDiary)
}