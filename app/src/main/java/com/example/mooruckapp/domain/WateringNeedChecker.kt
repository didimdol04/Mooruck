package com.example.mooruckapp.domain

import com.example.mooruckapp.data.local.dao.UserPlantDao
import com.example.mooruckapp.data.local.dao.WateringRecordDao
import com.example.mooruckapp.data.local.entity.UserPlant
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * 오늘 물을 줘야 하는 식물 판단 로직
 * 메인 화면 물주기 대상 표시, 물주기 알림(WorkManager) 양쪽에서 공통 사용
 *
 * 판단 기준: (마지막으로 물 준 날, 없으면 심은 날) + 물주기 주기 <= 오늘 → 오늘 물을 줘야 함
 */
class WateringNeedChecker(
    private val userPlantDao: UserPlantDao,
    private val wateringRecordDao: WateringRecordDao,
) {

    /**
     * 등록된 식물 전체 중 오늘 물을 줘야 하는 식물만 반환
     * @param today 기준 시각(epoch millis)
     */
    suspend fun getPlantsNeedingWaterToday(today: Long = System.currentTimeMillis()): List<UserPlant> {
        val allPlants = userPlantDao.observeAll().first()

        return allPlants.filter { plant ->
            val lastWateredDate = wateringRecordDao.getLastWateredDate(plant.id)
            needsWaterToday(
                plantedDate = plant.plantedDate,
                intervalDays = plant.wateringIntervalDays,
                lastWateredDate = lastWateredDate,
                today = today,
            )
        }
    }

    /** 순수 판단 함수. (유닛 테스트 용으로 분리) */
    internal fun needsWaterToday(
        plantedDate: Long,
        intervalDays: Int,
        lastWateredDate: Long?,
        today: Long,
    ): Boolean {
        if (intervalDays <= 0) return false

        val baseDate = lastWateredDate ?: plantedDate
        val nextDueDate = baseDate + TimeUnit.DAYS.toMillis(intervalDays.toLong())

        return today >= nextDueDate
    }
}
