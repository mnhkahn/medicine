package com.cyeam.medicine.helper

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cyeam.medicine.data.MedicineDatabase
import com.cyeam.medicine.data.MedicineRecord
import java.util.Date

class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_TAKE = "ACTION_TAKE_MEDICINE"
        const val ACTION_LATER = "ACTION_REMIND_LATER"
        const val EXTRA_NOTIFICATION_ID = "NOTIFICATION_ID"
        const val EXTRA_MED_ID = "MED_ID"
        const val EXTRA_MED_NAME = "MED_NAME"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val medId = intent.getIntExtra(EXTRA_MED_ID, 0)
        val medName = intent.getStringExtra(EXTRA_MED_NAME) ?: ""

        when (intent.action) {
            // 点击【已服药】：记录历史+关闭通知
            ACTION_TAKE -> {
                // 记录服药历史
                val db = MedicineDatabase.getDatabase(context)
                val record = MedicineRecord(
                    medicineId = medId,
                    medicineName = medName,
                    takeTime = Date().time,
                    isCompleted = true
                )
                db.recordDao().insertRecord(record)

                // 关闭通知
                notificationManager.cancel(notificationId)
            }

            // 点击【稍后提醒】：延迟15分钟再次提醒
            ACTION_LATER -> {
                notificationManager.cancel(notificationId)
                // 15分钟后再次发送通知
                android.os.Handler(context.mainLooper).postDelayed({
                    NotificationHelper.showUnDismissibleNotification(context, medId, medName, "")
                }, 15 * 60 * 1000)
            }
        }
    }
}