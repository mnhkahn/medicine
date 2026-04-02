package com.cyeam.medicine.helper

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.cyeam.medicine.R
import com.cyeam.medicine.ui.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "medicine_reminder_channel"
    private const val CHANNEL_NAME = "服药提醒"
    private const val CHANNEL_IMPORTANCE = NotificationManager.IMPORTANCE_HIGH

    // 核心：不可清除的服药提醒通知
    fun showUnDismissibleNotification(
        context: Context,
        notificationId: Int,
        medName: String,
        medDosage: String
    ) {
        // 1. 创建通知渠道（Android 8.0+ 必须）
        createNotificationChannel(context)

        // 2. 点击通知打开APP的PendingIntent
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. 【已服药】按钮的PendingIntent
        val takeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_TAKE
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(NotificationActionReceiver.EXTRA_MED_ID, notificationId)
            putExtra(NotificationActionReceiver.EXTRA_MED_NAME, medName)
        }
        val takePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1000,
            takeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. 【稍后提醒】按钮的PendingIntent
        val laterIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_LATER
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(NotificationActionReceiver.EXTRA_MED_ID, notificationId)
            putExtra(NotificationActionReceiver.EXTRA_MED_NAME, medName)
        }
        val laterPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2000,
            laterIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 5. 构建通知（核心：setOngoing(true) 禁止滑动清除）
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock) // 系统自带图标，避免资源缺失
            .setContentTitle(context.getString(R.string.need_have_medicine)+":"+medName)
            .setContentText(context.getString(R.string.item_count)+":"+medDosage)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(false) // 禁止点击通知自动关闭
            .setOngoing(true) // 核心：禁止滑动清除
            .setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI) // 闹钟铃声
            .setVibrate(longArrayOf(0, 500, 200, 500)) // 震动
            // 添加动作按钮
            .addAction(android.R.drawable.ic_menu_add, context.getString(R.string.had_medicine), takePendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, context.getString(R.string.remind_latar), laterPendingIntent)
            .build()

        // 6. 发送通知
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    // 创建通知渠道
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                CHANNEL_IMPORTANCE
            ).apply {
                description = "服药提醒专属渠道，保证准时收到通知"
                enableVibration(true)
                enableLights(true)
                setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI, null)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}