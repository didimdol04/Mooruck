package com.example.mooruckapp.domain

import com.example.mooruckapp.data.local.dao.WateringRecordDao
import com.example.mooruckapp.data.local.entity.UserPlant
import com.example.mooruckapp.data.local.entity.WateringRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * [WateringScoreCalculator 유닛 테스트]
 * 실제 Room DB 대신 FakeWateringRecordDao로 "실제 물 준 횟수"만 고정하여 검증
 */
class WateringScoreCalculatorTest {

    private val oneDayMillis = TimeUnit.DAYS.toMillis(1)

    private fun calculatorWithWateringCount(count: Int): WateringScoreCalculator {
        val fakeDao = FakeWateringRecordDao(wateringCount = count)
        return WateringScoreCalculator(fakeDao)
    }

    private fun testPlant(plantedDate: Long, intervalDays: Int): UserPlant {
        return UserPlant(
            id = 1L,
            plantName = "테스트식물",
            wateringIntervalDays = intervalDays,
            plantedDate = plantedDate,
        )
    }

    @Test
    fun `기대 물주기 횟수 - 3일 주기, 7일 경과시 3회`() {
        val calculator = calculatorWithWateringCount(0)

        val expected = calculator.calculateExpectedWateringCount(
            plantedDate = 0L,
            intervalDays = 3,
            today = 7 * oneDayMillis,
        )

        assertEquals(3, expected)
    }

    @Test
    fun `기대 물주기 횟수 - 심은 당일에는 1회`() {
        val calculator = calculatorWithWateringCount(0)

        val expected = calculator.calculateExpectedWateringCount(
            plantedDate = 0L,
            intervalDays = 3,
            today = 0L,
        )

        assertEquals(1, expected)
    }

    @Test
    fun `점수 계산 - 기대 3회 중 2회 물 줬으면 약 66점`() = runBlocking {
        val plant = testPlant(plantedDate = 0L, intervalDays = 3)
        val calculator = calculatorWithWateringCount(count = 2)

        val score = calculator.calculateScore(plant, today = 7 * oneDayMillis)

        assertEquals(66, score)
    }

    @Test
    fun `점수 계산 - 기대보다 많이 줬어도 100점을 넘지 않음`() = runBlocking {
        val plant = testPlant(plantedDate = 0L, intervalDays = 3)
        val calculator = calculatorWithWateringCount(count = 10)

        val score = calculator.calculateScore(plant, today = 7 * oneDayMillis)

        assertEquals(100, score)
    }

    @Test
    fun `점수 계산 - 심은 당일 물을 1번 줬으면 만점`() = runBlocking {
        val plant = testPlant(plantedDate = 0L, intervalDays = 5)
        val calculator = calculatorWithWateringCount(count = 1)

        val score = calculator.calculateScore(plant, today = 0L)

        assertEquals(100, score)
    }
}

/**
 * 테스트 전용 DAO. getWateringCount()만 고정값을 반환하고,
 * 이 테스트에서 쓰지 않는 나머지 메서드는 호출 시 예외를 던짐
 */
private class FakeWateringRecordDao(private val wateringCount: Int) : WateringRecordDao {
    override suspend fun insert(record: WateringRecord) {
        throw NotImplementedError("이 테스트에서는 사용하지 않음")
    }

    override suspend fun getLastWateredDate(userPlantId: Long): Long? {
        throw NotImplementedError("이 테스트에서는 사용하지 않음")
    }

    override suspend fun getWateringCount(userPlantId: Long): Int = wateringCount

    override fun observeRecordsForPlant(userPlantId: Long): Flow<List<WateringRecord>> {
        throw NotImplementedError("이 테스트에서는 사용하지 않음")
    }
}
