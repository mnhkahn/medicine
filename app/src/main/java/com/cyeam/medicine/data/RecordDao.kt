package com.cyeam.medicine.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RecordDao {
    // 新增服药记录
    @Insert
    fun insertRecord(record: MedicineRecord): Long

    // 查询所有历史记录
    @Query("SELECT * FROM medicine_records ORDER BY takeTime DESC")
    fun getAllRecords(): LiveData<List<MedicineRecord>>
}