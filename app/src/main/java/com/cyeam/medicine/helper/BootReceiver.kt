package com.cyeam.medicine.helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "BootReceiver_TAG"

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 监听手机开机完成广播
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "手机重启完成，重新初始化所有闹钟")
            AlarmHelper.initAllAlarms(context)
        }
    }
}