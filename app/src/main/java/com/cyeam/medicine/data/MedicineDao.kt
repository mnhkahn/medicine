package com.cyeam.medicine.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MedicineDao {
    // 原有：给UI用的LiveData查询
    @Query("SELECT * FROM medicines ORDER BY timeHour, timeMinute")
    fun getAllMedicines(): LiveData<List<Medicine>>

    // 新增：给后台用的同步查询（核心！广播里用这个拿数据）
    @Query("SELECT * FROM medicines ORDER BY timeHour, timeMinute")
    fun getAllMedicinesSync(): List<Medicine>

    // 新增：根据ID查询单个药品（续期闹钟用）
    @Query("SELECT * FROM medicines WHERE id = :medId LIMIT 1")
    fun getMedicineById(medId: Int): Medicine?

    // 原有插入/删除方法
    @Insert
    fun insert(medicine: Medicine): Long

    @Delete
    fun delete(medicine: Medicine): Int
}