package com.cyeam.medicine.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// 版本号：新增表/改表结构时，版本号+1（当前是2）
@Database(
    entities = [Medicine::class, MedicineRecord::class],
    version = 2,
    exportSchema = true // 开启Schema导出，方便后续迁移
)
abstract class MedicineDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun recordDao(): RecordDao

    companion object {
        // 定义数据库迁移规则（版本1→2，新增MedicineRecord表）
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建服药历史表（和实体类结构一致）
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `medicine_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `medicineId` INTEGER NOT NULL,
                        `medicineName` TEXT NOT NULL,
                        `takeTime` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        @Volatile
        private var INSTANCE: MedicineDatabase? = null

        fun getDatabase(context: Context): MedicineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MedicineDatabase::class.java,
                    "cyeam_medicine_db" // 数据库文件名固定，升级不改名
                )
                    // 关键：添加迁移规则，替代破坏性迁移
                    .addMigrations(MIGRATION_1_2)
                    // 仅当迁移规则缺失时，才允许破坏性迁移（兜底，避免崩溃）
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .allowMainThreadQueries() // 新手友好，正式版可替换为协程
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}