package com.cyeam.medicine.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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

    private val db by lazy { MedicineDatabase.getInstance(this) }
    private val viewModel: MedicineViewModel by viewModels {
        MedicineViewModelFactory(db)
    }

    private lateinit var adapter: MedicineAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rv = findViewById<RecyclerView>(R.id.recyclerView)
        val fab = findViewById<FloatingActionButton>(R.id.fab)

        adapter = MedicineAdapter(
            onDeleteClick = { medicine -> deleteMedicine(medicine) }
        )

        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(this)

        viewModel.medicines.observe(this) {
            adapter.submitList(it)
        }

        fab.setOnClickListener {
            showAddDialog()
        }
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etDosage = dialogView.findViewById<EditText>(R.id.etDosage)
        val btnTime = dialogView.findViewById<Button>(R.id.btnTime)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        var selectedHour = 8
        var selectedMinute = 0

        btnTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, h, m ->
                selectedHour = h
                selectedMinute = m
                btnTime.text = String.format("%02d:%02d", h, m)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString()
            val dosage = etDosage.text.toString()

            if (name.isEmpty()) {
                Toast.makeText(this, "请输入药品名", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                val med = Medicine(name = name, dosage = dosage, timeHour = selectedHour, timeMinute = selectedMinute)
                val id = db.medicineDao().insert(med)

                // 设置闹钟
                AlarmHelper.setAlarm(this@MainActivity, med.copy(id = id.toInt()))
            }
            dialog.dismiss()
            Toast.makeText(this, "药品已添加", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun deleteMedicine(medicine: Medicine) {
        CoroutineScope(Dispatchers.IO).launch {
            db.medicineDao().delete(medicine) // 调用同步 delete
            AlarmHelper.cancelAlarm(this@MainActivity, medicine.id)
        }
        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show()
    }
}

// ViewModel 部分
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