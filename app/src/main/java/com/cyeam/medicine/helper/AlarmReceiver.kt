package com.cyeam.medicine.helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.cyeam.medicine.data.Medicine
import com.cyeam.medicine.data.MedicineDatabase

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 1. 先通过 context 获取 PowerManager 实例（关键修正）
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        // 2. 再用实例调用 newWakeLock
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Medicine:AlarmWakeLock"
        )
        // 3. 加锁，设置10分钟超时（避免忘记释放导致耗电）
        wakeLock.acquire(10 * 60 * 1000L)

        // 获取药品信息
        val medId = intent.getIntExtra("MED_ID", -1)
        val medName = intent.getStringExtra("MED_NAME") ?: "药品"
        val medDosage = intent.getStringExtra("MED_DOSAGE") ?: ""

        if (medId == -1) {
            wakeLock.release()
            return
        }

        // 显示不可清除的通知
        NotificationHelper.showUnDismissibleNotification(context, medId, medName, medDosage)

        // 自动设置次日的闹钟，实现每日重复
        val db = MedicineDatabase.getDatabase(context)
        val medicine = db.medicineDao().getAllMedicines().value?.find { it.id == medId }
        medicine?.let {
            AlarmHelper.setDailyAlarm(context, it)
        }

        // 4. 释放唤醒锁
        wakeLock.release()
    }
}