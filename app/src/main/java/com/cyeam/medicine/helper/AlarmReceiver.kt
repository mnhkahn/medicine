package com.cyeam.medicine.helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.cyeam.medicine.data.MedicineDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "AlarmReceiver_TAG"
private const val WAKELOCK_TIMEOUT = 10 * 60 * 1000L // 10分钟超时，避免耗电

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "闹钟触发！接收到广播")
        // 1. 获取唤醒锁，保证系统不会中途休眠
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Medicine:AlarmWakeLock"
        )
        wakeLock.acquire(WAKELOCK_TIMEOUT)

        // 2. 获取药品参数
        val medId = intent.getIntExtra(AlarmHelper.EXTRA_MED_ID, -1)
        val medName = intent.getStringExtra(AlarmHelper.EXTRA_MED_NAME) ?: "药品"
        val medDosage = intent.getStringExtra(AlarmHelper.EXTRA_MED_DOSAGE) ?: ""

        if (medId == -1) {
            Log.e(TAG, "闹钟触发失败：药品ID为空")
            wakeLock.release()
            return
        }
        Log.i(TAG, "当前触发药品：ID=$medId，名称=$medName")

        // 3. 显示不可清除的服药通知
        NotificationHelper.showUnDismissibleNotification(context, medId, medName, medDosage)

        // 4. 【核心续期】异步设置次日闹钟，确保每日重复
        CoroutineScope(Dispatchers.IO).launch {
            val db = MedicineDatabase.getDatabase(context)
            val medicine = db.medicineDao().getMedicineById(medId)

            if (medicine != null) {
                // 药品存在，设置次日闹钟
                AlarmHelper.setDailyAlarm(context, medicine)
                Log.i(TAG, "次日闹钟续期成功！药品：$medName")
            } else {
                Log.w(TAG, "药品已删除，取消闹钟续期，ID=$medId")
                AlarmHelper.cancelAlarm(context, medId)
            }
            // 5. 释放唤醒锁
            wakeLock.release()
        }
    }
}