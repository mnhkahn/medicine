package com.cyeam.medicine.helper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.cyeam.medicine.data.CycleType
import com.cyeam.medicine.data.Medicine
import com.cyeam.medicine.data.MedicineDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object AlarmHelper {
    private const val TAG = "AlarmHelper_TAG"
    const val EXTRA_MED_ID = "MED_ID"
    const val EXTRA_MED_NAME = "MED_NAME"
    const val EXTRA_MED_DOSAGE = "MED_DOSAGE"

    // 【核心1】初始化所有药品的闹钟（APP启动/手机重启时调用）
    fun initAllAlarms(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = MedicineDatabase.getDatabase(context)
            val medicineList = db.medicineDao().getAllMedicinesSync()
            Log.i(TAG, "初始化所有闹钟，共${medicineList.size}个药品")

            medicineList.forEach { medicine ->
                setDailyAlarm(context, medicine)
            }
        }
    }

    // 【核心2】设置单个药品的闹钟（支持不同周期类型）
    fun setDailyAlarm(context: Context, medicine: Medicine) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_MED_ID, medicine.id)
            putExtra(EXTRA_MED_NAME, medicine.name)
            putExtra(EXTRA_MED_DOSAGE, medicine.dosage)
        }

        // 用药品ID作为唯一请求码，避免闹钟互相覆盖
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicine.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 计算闹钟触发时间（考虑开始时间和周期类型）
        val calendar = Calendar.getInstance().apply {
            // 首先设置为开始日期的提醒时间
            timeInMillis = medicine.startDate
            set(Calendar.HOUR_OF_DAY, medicine.timeHour)
            set(Calendar.MINUTE, medicine.timeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // 如果提醒时间已过，根据周期类型计算下一次提醒时间
            val currentTime = System.currentTimeMillis()
            while (timeInMillis <= currentTime) {
                when (medicine.cycleType) {
                    CycleType.DAILY -> add(Calendar.DAY_OF_YEAR, 1)
                    CycleType.WEEKLY -> add(Calendar.WEEK_OF_YEAR, 1)
                    CycleType.MONTHLY -> add(Calendar.MONTH, 1)
                    CycleType.YEARLY -> add(Calendar.YEAR, 1)
                }
            }
            Log.i(TAG, "药品${medicine.name}今日时间已过，设置为下次${medicine.timeHour}:${medicine.timeMinute}")
        }

        // 适配所有Android版本的精确闹钟，Doze模式下也能唤醒
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.i(TAG, "闹钟设置成功！药品：${medicine.name}，触发时间：${calendar.time}，周期：${medicine.cycleType}")
        } catch (e: SecurityException) {
            Log.e(TAG, "闹钟设置失败，缺少精确闹钟权限：${e.message}")
        }
    }

    // 取消单个药品的闹钟
    fun cancelAlarm(context: Context, medicineId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicineId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.i(TAG, "闹钟已取消，药品ID：$medicineId")
    }
}