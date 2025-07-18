package com.rindakusuma.runvest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DaftarAtletAdapter(
    private val atletList: List<AtletModel>,
    private val onClick: (AtletModel) -> Unit
) : RecyclerView.Adapter<DaftarAtletAdapter.AtletViewHolder>() {

    inner class AtletViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val namaTextView: TextView = itemView.findViewById(R.id.atletNameTextView)
        val emailTextView: TextView = itemView.findViewById(R.id.atletEmailTextView)
        val avatarImageView: ImageView = itemView.findViewById(R.id.avatarImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AtletViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_atlet, parent, false)
        return AtletViewHolder(view)
    }

    override fun onBindViewHolder(holder: AtletViewHolder, position: Int) {
        val atlet = atletList[position]
        holder.namaTextView.text = atlet.name
        holder.emailTextView.text = atlet.email

        // Bisa diganti dengan Glide atau Picasso jika ingin gambar dari URL
        holder.avatarImageView.setImageResource(R.drawable.ic_user)

        holder.itemView.setOnClickListener { onClick(atlet) }
    }

    override fun getItemCount(): Int = atletList.size
}