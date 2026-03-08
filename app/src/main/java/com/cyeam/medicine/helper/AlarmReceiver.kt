package com.cyeam.medicine.helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("MED_NAME") ?: "药品"
        val dosage = intent.getStringExtra("MED_DOSAGE") ?: ""
        val id = intent.getIntExtra("MED_ID", 0)
        NotificationHelper.showNotification(context, id, name, dosage)
    }
}