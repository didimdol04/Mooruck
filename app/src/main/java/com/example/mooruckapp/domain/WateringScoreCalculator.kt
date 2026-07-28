package com.example.mooruckapp.domain

import com.example.mooruckapp.data.local.dao.WateringRecordDao
import com.example.mooruckapp.data.local.entity.UserPlant
import java.util.concurrent.TimeUnit

/**
 * 물주기 점수 계산 로직.
 *
 * 점수 = (실제 물 준 횟수 / 기대 물주기 횟수) * 100, 0~100점 사이로 제한
 * 기대 물주기 횟수: 식물을 심은 날부터 오늘까지 경과일과 물주기 주기(wateringIntervalDays)를
 * 기준으로 계산하며, 심은 날 자체를 1회차로 포함
 *
 * ex) 물주기 주기 3일, 심은 지 7일 경과 → 기대 횟수 = 7/3 + 1 = 3회
 *     그 중 실제로 2번만 물을 줬다면 → 점수 = 2/3 * 100 ≈ 66점
 */
class WateringScoreCalculator(
    private val wateringRecordDao: WateringRecordDao,
) {

    /**
     * 특정 식물(userPlant)의 현재 물주기 점수 계산
     * @param today 기준 시각(epoch millis).
     */
    suspend fun calculateScore(userPlant: UserPlant, today: Long = System.currentTimeMillis()): Int {
        val expectedCount = calculateExpectedWateringCount(
            plantedDate = userPlant.plantedDate,
            intervalDays = userPlant.wateringIntervalDays,
            today = today,
        )

        // 심은 지 얼마 안 되어 아직 기대 횟수가 0인 경우 만점 처리
        if (expectedCount <= 0) return MAX_SCORE

        val actualCount = wateringRecordDao.getWateringCount(userPlant.id)
        val rawScore = (actualCount.toDouble() / expectedCount.toDouble()) * MAX_SCORE

        return rawScore.toInt().coerceIn(MIN_SCORE, MAX_SCORE)
    }

    /**
     * 마이페이지에 표시할 "전체" 물주기 점수 계산. 사용자가 등록한 식물 전체를 합산해서
     * (전체 실제 물 준 횟수 / 전체 기대 물주기 횟수) * 100 으로 계산
     * 식물이 하나도 없거나 아직 기대 횟수가 없으면 만점 처리
     */
    suspend fun calculateOverallScore(
        userPlants: List<UserPlant>,
        today: Long = System.currentTimeMillis(),
    ): Int {
        if (userPlants.isEmpty()) return MAX_SCORE

        var totalExpected = 0
        var totalActual = 0

        for (plant in userPlants) {
            val expected = calculateExpectedWateringCount(
                plantedDate = plant.plantedDate,
                intervalDays = plant.wateringIntervalDays,
                today = today,
            )
            if (expected <= 0) continue

            totalExpected += expected
            totalActual += wateringRecordDao.getWateringCount(plant.id)
        }

        if (totalExpected <= 0) return MAX_SCORE

        val rawScore = (totalActual.toDouble() / totalExpected.toDouble()) * MAX_SCORE
        return rawScore.toInt().coerceIn(MIN_SCORE, MAX_SCORE)
    }

    /** 심은 날부터 today까지, 물주기 주기 기준으로 몇 번 물을 줬어야 하는지 계산 */
    internal fun calculateExpectedWateringCount(plantedDate: Long, intervalDays: Int, today: Long): Int {
        if (intervalDays <= 0) return 0

        val elapsedDays = TimeUnit.MILLISECONDS.toDays(today - plantedDate)
        if (elapsedDays < 0) return 0

        return (elapsedDays / intervalDays).toInt() + 1
    }

    companion object {
        const val MIN_SCORE = 0
        const val MAX_SCORE = 100
    }
}
