package com.cyeam.medicine.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baidu.mobstat.StatService
import com.cyeam.medicine.R
import com.cyeam.medicine.data.CycleType
import com.cyeam.medicine.data.Medicine
import com.cyeam.medicine.data.MedicineDatabase
import com.cyeam.medicine.data.MedicineRecord
import com.cyeam.medicine.helper.AlarmHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class MainActivity : AppCompatActivity() {
    private val PERMISSION_REQUEST_CODE = 1001
    private val TAG = "MainActivity_TAG"

    private val db by lazy { MedicineDatabase.getDatabase(this) }
    private lateinit var viewModel: MedicineViewModel
    private lateinit var adapter: MedicineAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(TAG, "===== MainActivity 开始启动 =====")
        setContentView(R.layout.activity_main)

        checkAndRequestPermissions()
        requestIgnoreBatteryOptimization()

        val rv = findViewById<RecyclerView>(R.id.recyclerView)
        val fab = findViewById<FloatingActionButton>(R.id.fab)

        adapter = MedicineAdapter(
            onDeleteClick = { medicine -> deleteMedicine(medicine) },
            onItemClick = { medicine -> showMedicineConfirmDialog(medicine) }
        )
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(this)

        viewModel = ViewModelProvider(
            this,
            MedicineViewModelFactory(db)
        )[MedicineViewModel::class.java]

        viewModel.medicines.observe(this) {
            adapter.submitList(it)
        }

        // 绑定历史记录按钮
        val btnHistory = findViewById<Button>(R.id.btn_history)
        btnHistory.setOnClickListener {
            // 跳转到服药历史页面
            startActivity(Intent(this, MedicineHistoryActivity::class.java))
        }

        // 绑定未来提醒查询按钮
        val btnFutureReminders = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.btn_future_reminders)
        btnFutureReminders.setOnClickListener {
            showFutureRemindersDialog()
        }

        fab.setOnClickListener {
            showAddMedicineDialog()
        }

        AlarmHelper.initAllAlarms(this)

        StatService.setDebugOn(true);
        // 启动统计
        StatService.autoTrace(this);
        Log.i(TAG, "===== MainActivity 启动完成 =====")
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                permissionsToRequest.add(android.Manifest.permission.SCHEDULE_EXACT_ALARM)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    private fun requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as android.os.PowerManager
            val packageName = packageName

            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent().apply {
                    action = android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = android.net.Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }

    private fun showAddMedicineDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etDosage = dialogView.findViewById<EditText>(R.id.etDosage)
        val btnTime = dialogView.findViewById<Button>(R.id.btnTime)
        val btnStartDate = dialogView.findViewById<Button>(R.id.btnStartDate)
        val btnCycleType = dialogView.findViewById<Button>(R.id.btnCycleType)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        var selectedHour = 8
        var selectedMinute = 0
        var selectedStartDate = Calendar.getInstance().timeInMillis // 默认今天
        var selectedCycleType = CycleType.DAILY // 默认每天
        
        // 初始化按钮文本（使用多语言）
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        btnStartDate.text = getString(R.string.select_start_date) + ": " + sdf.format(Date(selectedStartDate))
        btnCycleType.text = getString(R.string.reminder_period_text) + getString(R.string.cycle_daily)

        // 时间选择
        btnTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, h, m ->
                selectedHour = h
                selectedMinute = m
                btnTime.text = String.format("%02d:%02d", h, m)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        // 开始日期选择
        btnStartDate.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.timeInMillis = selectedStartDate
            DatePickerDialog(
                this,
                DatePickerDialog.OnDateSetListener {
                    _, year, month, dayOfMonth ->
                    val selectedCal = Calendar.getInstance()
                    selectedCal.set(year, month, dayOfMonth, 0, 0, 0)
                    selectedCal.set(Calendar.MILLISECOND, 0)
                    selectedStartDate = selectedCal.timeInMillis
                    
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    btnStartDate.text = getString(R.string.select_start_date) + ": " + sdf.format(Date(selectedStartDate))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // 周期类型选择
        btnCycleType.setOnClickListener {
            val items = arrayOf(
                getString(R.string.cycle_daily),
                getString(R.string.cycle_weekly),
                getString(R.string.cycle_monthly),
                getString(R.string.cycle_yearly)
            )
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_select_cycle))
                .setItems(items) { _, which ->
                    selectedCycleType = when (which) {
                        0 -> CycleType.DAILY
                        1 -> CycleType.WEEKLY
                        2 -> CycleType.MONTHLY
                        3 -> CycleType.YEARLY
                        else -> CycleType.DAILY
                    }
                    btnCycleType.text = getString(R.string.reminder_period_text) + items[which]
                }
                .show()
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val dosage = etDosage.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "请输入药品名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                val medicine = Medicine(
                    name = name,
                    dosage = dosage,
                    timeHour = selectedHour,
                    timeMinute = selectedMinute,
                    startDate = selectedStartDate,
                    cycleType = selectedCycleType
                )
                val medId = db.medicineDao().insert(medicine)
                val newMedicine = medicine.copy(id = medId.toInt())
                AlarmHelper.setDailyAlarm(this@MainActivity, newMedicine)
            }

            dialog.dismiss()
            Toast.makeText(this, "药品添加成功，提醒已设置", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun deleteMedicine(medicine: Medicine) {
        CoroutineScope(Dispatchers.IO).launch {
            db.medicineDao().delete(medicine)
            AlarmHelper.cancelAlarm(this@MainActivity, medicine.id)
        }
        Toast.makeText(this, "药品已删除", Toast.LENGTH_SHORT).show()
    }

    private fun showFutureRemindersDialog() {
        CoroutineScope(Dispatchers.IO).launch {
            val allMedicines = db.medicineDao().getAllMedicinesSync()
            val futureReminders = mutableListOf<String>()
            val currentTime = System.currentTimeMillis()

            for (medicine in allMedicines) {
                // 计算下一次提醒时间
                val nextReminderTime = calculateNextReminderTime(medicine, currentTime)
                if (nextReminderTime > currentTime) {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                    val reminderTimeStr = sdf.format(Date(nextReminderTime))
                    val cycleTypeStr = when (medicine.cycleType) {
                        CycleType.DAILY -> getString(R.string.cycle_daily)
                        CycleType.WEEKLY -> getString(R.string.cycle_weekly)
                        CycleType.MONTHLY -> getString(R.string.cycle_monthly)
                        CycleType.YEARLY -> getString(R.string.cycle_yearly)
                    }
                    futureReminders.add("${medicine.name}（${medicine.dosage}）\n下次提醒：$reminderTimeStr\n周期：$cycleTypeStr")
                }
            }

            runOnUiThread {
                val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("未来提醒明细")

                if (futureReminders.isEmpty()) {
                    dialogBuilder.setMessage("当前没有未来提醒的药品")
                } else {
                    val message = futureReminders.joinToString("\n\n")
                    dialogBuilder.setMessage(message)
                }

                dialogBuilder.setPositiveButton(getString(R.string.btn_confirm)) { dialog, _ ->
                    dialog.dismiss()
                }

                dialogBuilder.show()
            }
        }
    }

    private fun calculateNextReminderTime(medicine: Medicine, currentTime: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime

        val reminderCalendar = Calendar.getInstance()
        reminderCalendar.timeInMillis = medicine.startDate
        reminderCalendar.set(Calendar.HOUR_OF_DAY, medicine.timeHour)
        reminderCalendar.set(Calendar.MINUTE, medicine.timeMinute)
        reminderCalendar.set(Calendar.SECOND, 0)
        reminderCalendar.set(Calendar.MILLISECOND, 0)

        // 如果提醒时间已过，计算下一次提醒时间
        while (reminderCalendar.timeInMillis <= currentTime) {
            when (medicine.cycleType) {
                CycleType.DAILY -> reminderCalendar.add(Calendar.DAY_OF_YEAR, 1)
                CycleType.WEEKLY -> reminderCalendar.add(Calendar.WEEK_OF_YEAR, 1)
                CycleType.MONTHLY -> reminderCalendar.add(Calendar.MONTH, 1)
                CycleType.YEARLY -> reminderCalendar.add(Calendar.YEAR, 1)
            }
        }

        return reminderCalendar.timeInMillis
    }

    private fun showMedicineConfirmDialog(medicine: Medicine) {
        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_title_medicine_confirm))
            .setMessage(getString(R.string.dialog_message_medicine_confirm, medicine.name, medicine.dosage))
            .setPositiveButton(getString(R.string.had_medicine)) { dialog, _ ->
                // 记录服药历史
                CoroutineScope(Dispatchers.IO).launch {
                    val record = MedicineRecord(
                        medicineId = medicine.id,
                        medicineName = medicine.name,
                        takeTime = System.currentTimeMillis(),
                        isCompleted = true
                    )
                    db.recordDao().insertRecord(record)
                }
                Toast.makeText(this, getString(R.string.toast_medicine_taken_record_added), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ ->
                dialog.dismiss()
            }

        dialogBuilder.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            grantResults.forEachIndexed { index, result ->
                val permission = permissions[index]
                if (result == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "权限申请成功：$permission")
                } else {
                    Log.e(TAG, "权限申请被拒绝：$permission（可能导致提醒功能失效）")
                }
            }
        }
    }

    // 显示右上角菜单
    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    // 点击菜单跳转历史页面
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == R.id.menu_history) {
            startActivity(android.content.Intent(this, MedicineHistoryActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // 务必添加页面统计
    override fun onResume() {
        super.onResume()
        StatService.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        StatService.onPause(this)
    }
}

// ViewModel
class MedicineViewModel(private val db: MedicineDatabase) : ViewModel() {
    val medicines = db.medicineDao().getAllMedicines()
}

// ViewModelFactory
class MedicineViewModelFactory(private val db: MedicineDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedicineViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MedicineViewModel(db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

