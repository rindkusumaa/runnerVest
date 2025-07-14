package com.rindakusuma.runvest

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class DaftarAtletActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DaftarAtletAdapter
    private lateinit var database: DatabaseReference
    private val daftarAlat = mutableListOf<AtletModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daftar_atlet)

        recyclerView = findViewById(R.id.recyclerViewAtlet)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = DaftarAtletAdapter(daftarAlat) { selectedDeviceId ->
            val intent = Intent(this, DetailAtletActivity::class.java)
            intent.putExtra("deviceId", selectedDeviceId)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        database = FirebaseDatabase.getInstance().reference
        ambilDaftarAtlet()
    }

    private fun ambilDaftarAtlet() {
        database.child("devices").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                daftarAlat.clear()
                for (alat in snapshot.children) {
                    val deviceId = alat.key ?: continue
                    val nama = alat.child("namaAtlet").getValue(String::class.java) ?: "Tanpa Nama"
                    daftarAlat.add(AtletModel(deviceId, nama))
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DaftarAtletActivity, "Gagal ambil data", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
