package com.example.mooruckapp.domain

import com.example.mooruckapp.data.local.dao.UserPlantDao
import com.example.mooruckapp.data.local.dao.WateringRecordDao
import com.example.mooruckapp.data.local.entity.UserPlant
import com.example.mooruckapp.data.local.entity.WateringRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * WateringNeedChecker 유닛 테스트.
 * 실제 Room DB 대신 FakeUserPlantDao / FakeWateringRecordDao로 값을 고정해서 검증한다.
 * (가짜 DAO는 다른 테스트 파일의 같은 이름 클래스와 충돌하지 않도록 이 클래스 내부에 nested로 둔다.)
 */
class WateringNeedCheckerTest {

    private val oneDayMillis = TimeUnit.DAYS.toMillis(1)

    private fun testPlant(id: Long, plantedDate: Long, intervalDays: Int): UserPlant {
        return UserPlant(
            id = id,
            plantName = "테스트식물$id",
            light = "보통",
            humidity = "보통",
            temperature = "보통",
            wateringIntervalDays = intervalDays,
            plantedDate = plantedDate,
        )
    }

    private fun checker(
        plants: List<UserPlant> = emptyList(),
        lastWateredDates: Map<Long, Long?> = emptyMap(),
    ): WateringNeedChecker {
        return WateringNeedChecker(
            userPlantDao = FakeUserPlantDao(plants),
            wateringRecordDao = FakeWateringRecordDao(lastWateredDates),
        )
    }

    @Test
    fun `한번도 물을 안 준 식물 - 주기가 지났으면 물 줘야 함`() {
        val checker = checker()

        val result = checker.needsWaterToday(
            plantedDate = 0L,
            intervalDays = 3,
            lastWateredDate = null,
            today = 3 * oneDayMillis,
        )

        assertEquals(true, result)
    }

    @Test
    fun `최근에 물을 줬으면 아직 물 줄 필요 없음`() {
        val checker = checker()

        val result = checker.needsWaterToday(
            plantedDate = 0L,
            intervalDays = 3,
            lastWateredDate = 2 * oneDayMillis,
            today = 3 * oneDayMillis,
        )

        assertEquals(false, result)
    }

    @Test
    fun `마지막 급수일 + 주기 == 오늘 이면 물 줘야 함 (경계값)`() {
        val checker = checker()

        val result = checker.needsWaterToday(
            plantedDate = 0L,
            intervalDays = 3,
            lastWateredDate = 2 * oneDayMillis,
            today = 5 * oneDayMillis,
        )

        assertEquals(true, result)
    }

    @Test
    fun `물주기 주기가 0 이하면 항상 false`() {
        val checker = checker()

        val result = checker.needsWaterToday(
            plantedDate = 0L,
            intervalDays = 0,
            lastWateredDate = null,
            today = 100 * oneDayMillis,
        )

        assertEquals(false, result)
    }

    @Test
    fun `getPlantsNeedingWaterToday - 물 줄 식물만 필터링`() = runBlocking {
        val duePlant = testPlant(id = 1L, plantedDate = 0L, intervalDays = 3)
        val notDuePlant = testPlant(id = 2L, plantedDate = 0L, intervalDays = 3)
        val today = 3 * oneDayMillis

        val checker = checker(
            plants = listOf(duePlant, notDuePlant),
            lastWateredDates = mapOf(
                1L to null, // 한 번도 안 줌 -> 심은 날(0) + 3일 <= 오늘(3일) -> 물 줘야 함
                2L to 2 * oneDayMillis, // 최근에 줌 -> 2일 + 3일 > 오늘(3일) -> 아직 아님
            ),
        )

        val result = checker.getPlantsNeedingWaterToday(today)

        assertEquals(listOf(duePlant), result)
    }

    /** 테스트 전용 가짜 DAO. observeAll()만 고정된 목록을 반환한다. */
    private class FakeUserPlantDao(private val plants: List<UserPlant>) : UserPlantDao {
        override suspend fun insert(userPlant: UserPlant): Long {
            throw NotImplementedError("이 테스트에서는 사용하지 않음")
        }

        override fun observeAll(): Flow<List<UserPlant>> = flowOf(plants)

        override fun observeById(id: Long): Flow<UserPlant?> {
            throw NotImplementedError("이 테스트에서는 사용하지 않음")
        }

        override suspend fun update(userPlant: UserPlant) {
            throw NotImplementedError("이 테스트에서는 사용하지 않음")
        }

        override suspend fun delete(userPlant: UserPlant) {
            throw NotImplementedError("이 테스트에서는 사용하지 않음")
        }
    }

    /** 테스트 전용 가짜 DAO. getLastWateredDate()만 식물 ID별 고정값을 반환한다. */
    private class FakeWateringRecordDao(
        private val lastWateredDates: Map<Long, Long?>,
    ) : WateringRecordDao {
        override suspend fun insert(record: WateringRecord) {
            throw NotImplementedError("이 테스트에서는 사용하지 않음")
        }

        override suspend fun getLastWateredDate(userPlantId: Long): Long? = lastWateredDates[userPlantId]

        override suspend fun getWateringCount(userPlantId: Long): Int {
            throw NotImplementedError("이 테스트에서는 사용하지 않음")
        }

        override fun observeRecordsForPlant(userPlantId: Long): Flow<List<WateringRecord>> {
            throw NotImplementedError("이 테스트에서는 사용하지 않음")
        }
    }
}
