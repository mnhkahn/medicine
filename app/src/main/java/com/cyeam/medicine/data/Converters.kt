package com.cyeam.medicine.data

import androidx.room.TypeConverter

// 周期类型枚举
enum class CycleType {
    DAILY,   // 每天
    WEEKLY,  // 每周
    MONTHLY, // 每月
    YEARLY   // 每年
}

// Room 类型转换器，用于处理枚举类型
class Converters {
    @TypeConverter
    fun fromCycleType(cycleType: CycleType): String {
        return cycleType.name
    }

    @TypeConverter
    fun toCycleType(value: String): CycleType {
        return CycleType.valueOf(value)
    }
}