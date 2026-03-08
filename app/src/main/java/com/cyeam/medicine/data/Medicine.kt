package com.cyeam.medicine.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // 自增主键
    val name: String,        // 药品名
    val dosage: String,      // 剂量
    val timeHour: Int,       // 提醒小时
    val timeMinute: Int      // 提醒分钟
)