package com.cyeam.medicine.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// 服药历史记录表
@Entity(tableName = "medicine_records")
data class MedicineRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicineId: Int,       // 关联的药品ID
    val medicineName: String,  // 药品名称
    val takeTime: Long,        // 服药时间（时间戳）
    val isCompleted: Boolean   // 是否完成服药
)