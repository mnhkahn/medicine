package com.cyeam.medicine.helper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.cyeam.medicine.data.Medicine
import java.util.Calendar

object AlarmHelper {
    // 设置单次精确闹钟，触发后自动设置下一次（实现每日重复）
    fun setDailyAlarm(context: Context, medicine: Medicine) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("MED_ID", medicine.id)
            putExtra("MED_NAME", medicine.name)
            putExtra("MED_DOSAGE", medicine.dosage)
        }

        // 唯一PendingIntent，用药品ID作为请求码
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicine.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 计算提醒时间
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, medicine.timeHour)
            set(Calendar.MINUTE, medicine.timeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // 如果今天的时间已过，设置为明天
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        // 适配不同Android版本的精确闹钟
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 适配Doze模式，低电耗下也能唤醒
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
    }

    // 取消药品对应的闹钟
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
    }
}