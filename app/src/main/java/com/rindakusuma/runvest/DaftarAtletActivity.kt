package com.rindakusuma.runvest

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DaftarAtletActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DaftarAtletAdapter
    private val atletList = mutableListOf<AtletModel>()
    private val database = FirebaseDatabase.getInstance().reference
    private val uidPemantau = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daftar_atlet)

        recyclerView = findViewById(R.id.recyclerViewDaftarAtlet)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = DaftarAtletAdapter(atletList) { atlet ->
            val intent = Intent(this, DetailAtletActivity::class.java)
            intent.putExtra("UID_ATLET", atlet.uid)
            startActivity(intent)
        }

        recyclerView.adapter = adapter

        // 💡 Tambah atlet secara manual untuk testing sementara
        val uidAtlet = "9pHbbU4LgXefvokx7veRC4rO3Xj1".trim()

        if (uidPemantau.isNotEmpty()) {
            database.child("monitoring").child(uidPemantau).child(uidAtlet).setValue(true)
                .addOnSuccessListener {
                    Log.d("MonitoringDebug", "Berhasil menambahkan $uidAtlet ke monitoring $uidPemantau")
                    ambilDaftarAtlet()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Gagal menambahkan atlet: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
                Toast.makeText(this, "UID pemantau kosong", Toast.LENGTH_SHORT).show()
            }
        }

    private fun ambilDaftarAtlet() {
        database.child("monitoring").child(uidPemantau)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    atletList.clear()

                    if (!snapshot.exists()) {
                        Toast.makeText(this@DaftarAtletActivity, "Belum ada atlet yang dipantau", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val atletUids = snapshot.children.mapNotNull { it.key }

                    for (uidAtlet in atletUids) {
                        val profileRef = database.child("users").child(uidAtlet).child("profile")

                        profileRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(profileSnapshot: DataSnapshot) {
                                if (!profileSnapshot.exists()) {
                                    Log.e("DaftarAtletActivity", "Profil tidak ditemukan untuk $uidAtlet")
                                    return
                                }

                                val name = profileSnapshot.child("name").getValue(String::class.java) ?: "Tidak dikenal"
                                val email = profileSnapshot.child("email").getValue(String::class.java) ?: "-"

                                atletList.add(AtletModel(uidAtlet, name, email))
                                adapter.notifyDataSetChanged()
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("DaftarAtletActivity", "Gagal ambil profil atlet $uidAtlet: ${error.message}")
                            }
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("DaftarAtletActivity", "Gagal ambil daftar atlet: ${error.message}")
                }
            })
    }
}
