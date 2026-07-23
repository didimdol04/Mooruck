package com.example.mooruckapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// 담당자별로 Entity를 만든 뒤 아래 entities 배열에 추가해주세요.
// (Plant, GrowthLog, WateringLog, UserProfile 등)
// 이 파일은 여러 명이 동시에 수정하기 쉬우니, 수정 전 팀 채팅에 공유해주세요.
@Database(
    entities = [], // TODO: 각 담당자가 자기 Entity 추가
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "waterians_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
