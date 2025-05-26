package com.rindakusuma.runvest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ActivityLogAdapter(private val logs: List<ActivityLog>) :
    RecyclerView.Adapter<ActivityLogAdapter.LogViewHolder>() {

    inner class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTime: TextView = itemView.findViewById(R.id.dateTimeTextView)
        val distance: TextView = itemView.findViewById(R.id.distanceLogTextView)
        val speed: TextView = itemView.findViewById(R.id.speedLogTextView)
        val heartRate: TextView = itemView.findViewById(R.id.heartRateLogTextView)
        val temperature: TextView = itemView.findViewById(R.id.temperatureLogTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]
        holder.dateTime.text = log.dateTime
        holder.distance.text = "Distance: ${log.distance}"
        holder.speed.text = "Avg Speed: ${log.speed}"
        holder.heartRate.text = "Heart Rate: ${log.heartRate}"
        holder.temperature.text = "Body Temp: ${log.temperature}"
    }

    override fun getItemCount(): Int = logs.size
}
