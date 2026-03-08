package com.cyeam.medicine.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MedicineDao {
    // 查询所有药品（LiveData 自动更新，无需 suspend）
    @Query("SELECT * FROM medicines ORDER BY timeHour, timeMinute")
    fun getAllMedicines(): LiveData<List<Medicine>>

    // 插入：Room 原生支持返回 Long（主键ID），去掉 suspend
    @Insert
    fun insert(medicine: Medicine): Long

    // 删除：Room 原生支持返回 Int（删除行数），去掉 suspend
    @Delete
    fun delete(medicine: Medicine): Int
}