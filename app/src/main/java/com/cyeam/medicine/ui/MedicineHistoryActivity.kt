package com.cyeam.medicine.ui

import android.os.Bundle
import android.view.LayoutInflater // 新增：导入LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cyeam.medicine.R
import com.cyeam.medicine.data.MedicineDatabase
import com.cyeam.medicine.data.MedicineRecord
import java.text.SimpleDateFormat
import java.util.*

class MedicineHistoryActivity : AppCompatActivity() {

    private val db by lazy { MedicineDatabase.getDatabase(this) }
    private lateinit var viewModel: HistoryViewModel
    private lateinit var adapter: RecordAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        supportActionBar?.title = "服药历史记录"

        val rv = findViewById<RecyclerView>(R.id.rv_history)
        adapter = RecordAdapter()
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(this)

        viewModel = ViewModelProvider(
            this,
            HistoryViewModelFactory(db)
        )[HistoryViewModel::class.java]

        viewModel.records.observe(this) { list ->
            adapter.submitList(list)
        }
    }
}

// 适配器（修正 layoutInflater 引用问题）
class RecordAdapter : ListAdapter<MedicineRecord, RecordAdapter.VH>(
    object : DiffUtil.ItemCallback<MedicineRecord>() {
        override fun areItemsTheSame(old: MedicineRecord, new: MedicineRecord) = old.id == new.id
        override fun areContentsTheSame(old: MedicineRecord, new: MedicineRecord) = old == new
    }
) {
    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tv_name)
        val time: TextView = itemView.findViewById(R.id.tv_time)
        val status: TextView = itemView.findViewById(R.id.tv_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        // 关键修正：通过 parent.context 获取 LayoutInflater
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_record, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.name.text = "药品：${item.medicineName}"
        holder.time.text = "时间：${formatTime(item.takeTime)}"
        holder.status.text = if (item.isCompleted) "已服药" else "未服药"
    }

    // 时间戳 → 格式化时间
    private fun formatTime(time: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(time))
    }
}

// ViewModel
class HistoryViewModel(val db: MedicineDatabase) : ViewModel() {
    val records = db.recordDao().getAllRecords()
}

class HistoryViewModelFactory(private val db: MedicineDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}