package com.example.qrshieldapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private var items: List<HistoryItem>,
    private val onItemClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryVH>() {

    inner class HistoryVH(view: View) : RecyclerView.ViewHolder(view) {
        val tvUrl: TextView = view.findViewById(R.id.tvUrl)
        val tvSummary: TextView = view.findViewById(R.id.tvSummary)
        val tvVerdict: TextView = view.findViewById(R.id.tvVerdict)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.history_item, parent, false)
        return HistoryVH(v)
    }

    override fun onBindViewHolder(holder: HistoryVH, position: Int) {
        val item = items[position]
        holder.tvUrl.text = item.url
        holder.tvSummary.text = item.geminiSummary ?: "ML score: ${"%.2f".format(item.mlScore)}"
        holder.tvVerdict.text = if (item.isMalicious) "NOT SAFE" else "SAFE"
        holder.tvVerdict.setTextColor(if (item.isMalicious) 0xFFD32F2F.toInt() else 0xFF388E3C.toInt())

        // Timestamp formatting
        val ts = item.timestamp
        if (ts != null) {
            val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            val date = ts.toDate()
            holder.tvTimestamp.text = sdf.format(date)
        } else {
            holder.tvTimestamp.text = ""
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<HistoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
