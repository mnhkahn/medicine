package com.cyeam.medicine.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// 核心：entities = [Medicine::class] 必须包含实体类，version 必须是整数
@Database(entities = [Medicine::class], version = 1, exportSchema = false)
abstract class MedicineDatabase : RoomDatabase() {
    // 关联 Dao 接口
    abstract fun medicineDao(): MedicineDao

    companion object {
        @Volatile
        private var INSTANCE: MedicineDatabase? = null

        fun getInstance(context: Context): MedicineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MedicineDatabase::class.java,
                    "medicine_db" // 数据库文件名
                )
                    .fallbackToDestructiveMigration() // 版本升级时清空数据（测试用）
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}