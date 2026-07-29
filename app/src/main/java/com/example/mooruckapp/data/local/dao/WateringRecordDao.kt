package com.example.mooruckapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.mooruckapp.data.local.entity.WateringRecord
import kotlinx.coroutines.flow.Flow

/**
 * WateringRecord 테이블 접근용 DAO.
 * 메인 화면 물주기 버튼, 식물 등록 시 1일차 자동 기록, 물주기 점수·알림 공통 사용
 */
@Dao
interface WateringRecordDao {

    /** 물주기 버튼/1일차 기록을 남길 때 호출 */
    @Insert
    suspend fun insert(record: WateringRecord)

    /** 특정 식물의 "마지막으로 물 준 날" (epoch millis). 기록이 하나도 없으면 null */
    @Query("SELECT MAX(watered_date) FROM watering_record WHERE user_plant_id = :userPlantId")
    suspend fun getLastWateredDate(userPlantId: Long): Long?

    /**
     * 해당 식물의 최초 물주기 날짜를 가져온다.
     *
     * 식물 등록 시 입력한 '마지막 물 준 날짜'가
     * 첫 번째 WateringRecord로 저장되므로 MIN을 사용한다.
     */
    @Query(
        """
        SELECT MIN(watered_date)
        FROM watering_record
        WHERE user_plant_id = :userPlantId
        """
    )
    suspend fun getFirstWateredDate(
        userPlantId: Long,
        ): Long?

    /** 마이페이지 물주기 점수 계산의 분자(실제 물 준 횟수) */
    @Query("SELECT COUNT(*) FROM watering_record WHERE user_plant_id = :userPlantId")
    suspend fun getWateringCount(userPlantId: Long): Int

    /** 특정 식물의 물주기 기록 전체를 최신순으로 구독 */
    @Query("SELECT * FROM watering_record WHERE user_plant_id = :userPlantId ORDER BY watered_date DESC")
    fun observeRecordsForPlant(userPlantId: Long): Flow<List<WateringRecord>>
}
