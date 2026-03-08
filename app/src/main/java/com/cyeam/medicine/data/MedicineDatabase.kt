package com.cyeam.medicine.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Medicine::class, MedicineRecord::class], // 新增历史记录表
    version = 2, // 版本升级
    exportSchema = false
)
abstract class MedicineDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun recordDao(): RecordDao // 新增历史记录Dao

    companion object {
        @Volatile
        private var INSTANCE: MedicineDatabase? = null

        fun getDatabase(context: Context): MedicineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MedicineDatabase::class.java,
                    "medicine_db"
                )
                    .fallbackToDestructiveMigration() // 版本升级自动重建表
                    .allowMainThreadQueries() // 临时允许主线程操作（新手友好）
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}