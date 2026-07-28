package com.example.mooruckapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.mooruckapp.data.local.dao.UserDao
import com.example.mooruckapp.data.local.dao.UserPlantDao
import com.example.mooruckapp.data.local.dao.WateringRecordDao
import com.example.mooruckapp.data.local.entity.User
import com.example.mooruckapp.data.local.entity.UserPlant
import com.example.mooruckapp.data.local.entity.WateringRecord


@Database(
    entities = [User::class, UserPlant::class, GrowthDiary::class, WateringRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun userPlantDao(): UserPlantDao
    abstract fun wateringRecordDao(): WateringRecordDao
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
