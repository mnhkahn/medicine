package com.cyeam.medicine.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// 版本号：新增表/改表结构时，版本号+1（当前是3）
@Database(
    entities = [Medicine::class, MedicineRecord::class],
    version = 3,
    exportSchema = true // 开启Schema导出，方便后续迁移
)
@TypeConverters(Converters::class)
abstract class MedicineDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun recordDao(): RecordDao

    companion object {
        // 定义数据库迁移规则（版本1→2，新增MedicineRecord表）
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建服药历史表（和实体类结构一致）
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `medicine_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `medicineId` INTEGER NOT NULL,
                        `medicineName` TEXT NOT NULL,
                        `takeTime` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL
                    )
                """.trimIndent()
                )
            }
        }

        // 定义数据库迁移规则（版本2→3，新增startDate和cycleType字段）
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 新增startDate字段，默认值为2025年3月13日，NOT NULL
                database.execSQL("ALTER TABLE medicines ADD COLUMN startDate INTEGER NOT NULL DEFAULT 1741872000000")
                // 新增cycleType字段，默认值为DAILY，NOT NULL
                database.execSQL("ALTER TABLE medicines ADD COLUMN cycleType TEXT NOT NULL DEFAULT 'DAILY'")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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