package com.cyeam.medicine.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cyeam.medicine.R
import com.cyeam.medicine.data.Medicine

class MedicineAdapter(
    private val onDeleteClick: (Medicine) -> Unit,
    private val onItemClick: (Medicine) -> Unit
) : ListAdapter<Medicine, MedicineAdapter.MedicineViewHolder>(DiffCallback) {

    inner class MedicineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvDosage: TextView = itemView.findViewById(R.id.tvDosage)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_medicine, parent, false)
        return MedicineViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        val medicine = getItem(position)
        holder.tvName.text = medicine.name
        val dosageLabel = holder.itemView.context.getString(R.string.item_count)
        holder.tvDosage.text = "$dosageLabel: ${medicine.dosage}"
        holder.tvTime.text = String.format("%02d:%02d", medicine.timeHour, medicine.timeMinute)
        holder.btnDelete.setOnClickListener { onDeleteClick(medicine) }
        // 添加药品项点击事件
        holder.itemView.setOnClickListener { onItemClick(medicine) }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Medicine>() {
        override fun areItemsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Medicine, newItem: Medicine): Boolean {
            return oldItem == newItem
        }
    }
}