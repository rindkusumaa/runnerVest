package com.rindakusuma.runvest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DaftarAtletAdapter(
    private val daftarAtlet: List<AtletModel>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<DaftarAtletAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val namaText: TextView = itemView.findViewById(R.id.namaAtletText)

        fun bind(atlet: AtletModel) {
            namaText.text = atlet.nama
            itemView.setOnClickListener {
                onItemClick(atlet.deviceId)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_atlet, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = daftarAtlet.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(daftarAtlet[position])
    }
}
