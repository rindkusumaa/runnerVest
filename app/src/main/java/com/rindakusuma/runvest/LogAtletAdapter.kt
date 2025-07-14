package com.rindakusuma.runvest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LogAtletAdapter(private val logs: List<LogAtlet>) :
    RecyclerView.Adapter<LogAtletAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val waktu: TextView = view.findViewById(R.id.logWaktu)
        val detak: TextView = view.findViewById(R.id.logDetak)
        val suhu: TextView = view.findViewById(R.id.logSuhu)
        val kecepatan: TextView = view.findViewById(R.id.logKecepatan)
        val jarak: TextView = view.findViewById(R.id.logJarak)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log_atlet, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val log = logs[position]
        holder.waktu.text = log.waktu
        holder.detak.text = log.detak
        holder.suhu.text = log.suhu
        holder.kecepatan.text = log.kecepatan
        holder.jarak.text = log.jarak
    }

    override fun getItemCount(): Int = logs.size
}
