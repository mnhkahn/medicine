package com.cyeam.medicine.ui

import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import com.cyeam.medicine.R
import com.cyeam.medicine.data.Medicine
import com.cyeam.medicine.data.MedicineDatabase
import com.cyeam.medicine.helper.AlarmHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private val TAG = "AlarmReceiver_TAG"
    // 权限请求码
    private val PERMISSION_REQUEST_CODE = 1001

    private val db by lazy { MedicineDatabase.getDatabase(this) }
    private lateinit var viewModel: MedicineViewModel
    private lateinit var adapter: MedicineAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. 启动时申请必须的权限
//        checkAndRequestPermissions()
        // 2. 引导用户关闭电池优化
//        requestIgnoreBatteryOptimization()

        // 初始化列表
        val rv = findViewById<RecyclerView>(R.id.recyclerView)
        val fab = findViewById<FloatingActionButton>(R.id.fab)

        adapter = MedicineAdapter(
            onDeleteClick = { medicine -> deleteMedicine(medicine) }
        )
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(this)

        // 初始化ViewModel
        viewModel = ViewModelProvider(this)[MedicineViewModel::class.java]
        viewModel.medicines.observe(this) {
            adapter.submitList(it)
        }

        // 新增药品按钮
        fab.setOnClickListener {
            showAddMedicineDialog()
        }
    }

    // 权限检查与申请
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Android 13+ 通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Android 12+ 精确闹钟权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                permissionsToRequest.add(android.Manifest.permission.SCHEDULE_EXACT_ALARM)
            }
        }

        // 申请权限
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    // 引导用户关闭电池优化
    private fun requestIgnoreBatteryOptimization() {
        Log.i(TAG,"requestIgnoreBatteryOptimization")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.i(TAG,"111")

            // 1. 先强转为 PowerManager 类型
            val powerManager = getSystemService(POWER_SERVICE) as android.os.PowerManager
            val packageName = packageName

            // 2. 检查是否已忽略电池优化
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                // 3. 引导用户去设置页面关闭电池优化
                val intent = Intent().apply {
                    action = android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = android.net.Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
        Log.i(TAG,"222")
    }

    // 新增药品弹窗
    private fun showAddMedicineDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etDosage = dialogView.findViewById<EditText>(R.id.etDosage)
        val btnTime = dialogView.findViewById<Button>(R.id.btnTime)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        var selectedHour = 8
        var selectedMinute = 0

        // 选择时间
        btnTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, h, m ->
                selectedHour = h
                selectedMinute = m
                btnTime.text = String.format("%02d:%02d", h, m)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        // 保存药品
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
                    timeMinute = selectedMinute
                )
                // 插入数据库
                val medId = db.medicineDao().insert(medicine)
                // 设置每日闹钟
                AlarmHelper.setDailyAlarm(this@MainActivity, medicine.copy(id = medId.toInt()))
            }

            dialog.dismiss()
            Toast.makeText(this, "药品添加成功，提醒已设置", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    // 删除药品
    private fun deleteMedicine(medicine: Medicine) {
        CoroutineScope(Dispatchers.IO).launch {
            // 删除数据库数据
            db.medicineDao().delete(medicine)
            // 取消对应的闹钟
            AlarmHelper.cancelAlarm(this@MainActivity, medicine.id)
        }
        Toast.makeText(this, "药品已删除", Toast.LENGTH_SHORT).show()
    }

    // 权限申请结果回调
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            grantResults.forEachIndexed { index, result ->
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "权限${permissions[index]}被拒绝，可能无法收到提醒", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

// ViewModel
class MedicineViewModel(private val db: MedicineDatabase) : ViewModel() {
    val medicines = db.medicineDao().getAllMedicines()
}

class MedicineViewModelFactory(private val db: MedicineDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedicineViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MedicineViewModel(db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}