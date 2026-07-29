package com.example.mooruckapp.domain

import com.example.mooruckapp.data.local.dao.WateringRecordDao
import com.example.mooruckapp.data.local.entity.UserPlant
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 물주기 점수 계산기.
 *
 * 계산 시작일:
 * 식물 등록 시 입력받아 최초 WateringRecord로 저장한 물주기 날짜
 *
 * 기대 횟수:
 * 최초 물주기 날짜부터 오늘까지 주기대로 물을 줬어야 하는 총횟수
 *
 * 실제 횟수:
 * 해당 식물에 저장된 전체 물주기 기록 수
 *
 * 점수:
 * 실제 횟수 / 기대 횟수 * 100
 */
class WateringScoreCalculator(
    private val wateringRecordDao: WateringRecordDao,
) {

    suspend fun calculateScore(
        userPlant: UserPlant,
        today: Long = System.currentTimeMillis(),
    ): Int {
        // 등록 시 저장된 최초 물주기 기록 날짜를 가져온다.
        val firstWateredDate =
            wateringRecordDao.getFirstWateredDate(userPlant.id)
                ?: return MAX_SCORE

        // 최초 물주기 날짜를 기준으로 기대 횟수를 계산한다.
        val expectedCount = calculateExpectedWateringCount(
            firstWateredDate = firstWateredDate,
            intervalDays = userPlant.wateringIntervalDays,
            today = today,
        )

        // 계산할 기대 횟수가 없다면 만점으로 처리한다.
        if (expectedCount <= 0) {
            return MAX_SCORE
        }

        // 최초 기록을 포함해 실제 저장된 물주기 기록 수를 구한다.
        val actualCount =
            wateringRecordDao.getWateringCount(userPlant.id)

        // 실제 횟수를 기대 횟수와 비교해 점수를 계산한다.
        val rawScore =
            (actualCount.toDouble() / expectedCount.toDouble()) * MAX_SCORE

        // 점수가 0점 미만이나 100점 초과가 되지 않도록 제한한다.
        return rawScore.toInt()
            .coerceIn(MIN_SCORE, MAX_SCORE)
    }

    /**
     * 사용자가 등록한 모든 식물의 전체 물주기 점수를 계산한다.
     *
     * 전체 실제 횟수 / 전체 기대 횟수 * 100
     */
    suspend fun calculateOverallScore(
        userPlants: List<UserPlant>,
        today: Long = System.currentTimeMillis(),
    ): Int {
        // 등록된 식물이 없다면 만점으로 처리한다.
        if (userPlants.isEmpty()) {
            return MAX_SCORE
        }

        // 모든 식물의 기대 물주기 횟수를 합산한다.
        var totalExpected = 0

        // 모든 식물의 실제 물주기 횟수를 합산한다.
        var totalActual = 0

        for (plant in userPlants) {
            // 해당 식물의 최초 물주기 날짜를 가져온다.
            val firstWateredDate =
                wateringRecordDao.getFirstWateredDate(plant.id)
                    ?: continue // 최초 기록이 없는 식물은 계산에서 제외한다.

            // 해당 식물의 기대 물주기 횟수를 계산한다.
            val expected = calculateExpectedWateringCount(
                firstWateredDate = firstWateredDate,
                intervalDays = plant.wateringIntervalDays,
                today = today,
            )

            // 올바른 기대 횟수가 없으면 합산하지 않는다.
            if (expected <= 0) {
                continue
            }

            // 해당 식물의 기대 횟수를 전체 기대 횟수에 더한다.
            totalExpected += expected

            // 해당 식물의 실제 기록 수를 전체 실제 횟수에 더한다.
            totalActual += wateringRecordDao.getWateringCount(plant.id)
        }

        // 계산 대상이 없다면 만점으로 처리한다.
        if (totalExpected <= 0) {
            return MAX_SCORE
        }

        // 전체 실제 횟수를 전체 기대 횟수로 나누어 점수를 계산한다.
        val rawScore =
            (totalActual.toDouble() / totalExpected.toDouble()) * MAX_SCORE

        // 최종 결과를 0~100점 사이로 제한한다.
        return rawScore.toInt()
            .coerceIn(MIN_SCORE, MAX_SCORE)
    }

    /**
     * 최초 물주기 날짜부터 오늘까지
     * 주기대로 몇 번 물을 줬어야 하는지 계산한다.
     */
    internal fun calculateExpectedWateringCount(
        firstWateredDate: Long, // 등록할 때 입력한 최초 물주기 날짜
        intervalDays: Int,      // 며칠마다 물을 주는지
        today: Long,            // 오늘 시각
    ): Int {
        // 물주기 주기가 잘못 설정된 경우 계산하지 않는다.
        if (intervalDays <= 0) {
            return 0
        }

        // 최초 물주기 날짜를 자정으로 맞춘다.
        val firstWateredDay = Calendar.getInstance().apply {
            timeInMillis = firstWateredDate

            // 시간 차이가 아니라 날짜 차이를 계산하기 위해
            // 시, 분, 초, 밀리초를 모두 0으로 만든다.
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // 오늘 날짜도 자정으로 맞춘다.
        val todayDay = Calendar.getInstance().apply {
            timeInMillis = today

            // 오늘의 시간 부분을 제거한다.
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // 최초 물주기 날짜부터 오늘까지 지난 날짜 수를 구한다.
        val elapsedDays = TimeUnit.MILLISECONDS.toDays(
            todayDay - firstWateredDay,
        )

        // 최초 물주기 날짜가 미래라면 계산하지 않는다.
        if (elapsedDays < 0) {
            return 0
        }

        // 최초 물주기 기록 자체를 1회로 포함한다.
        return (elapsedDays / intervalDays).toInt() + 1
    }

    companion object {
        const val MIN_SCORE = 0
        const val MAX_SCORE = 100
    }
}