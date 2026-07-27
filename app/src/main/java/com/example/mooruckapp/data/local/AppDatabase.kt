package com.example.mooruckapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.mooruckapp.data.local.dao.GrowthDiaryDao

// Entity를 만든 뒤 아래 entities 배열에 추가
@Database(
    entities = [GrowthDiary::class], // TODO: 각 담당자가 자기 Entity 추가
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun growthDiaryDao(): GrowthDiaryDao

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
