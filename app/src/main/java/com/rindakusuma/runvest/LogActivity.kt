package com.rindakusuma.runvest

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import com.rindakusuma.runvest.databinding.ActivityLogBinding

class LogActivity : AppCompatActivity() {

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ActivityLogAdapter
    private val activityLogs = mutableListOf<ActivityLog>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        swipeRefreshLayout = findViewById(R.id.SwipeRefreshLayout)
        recyclerView = findViewById(R.id.logRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ActivityLogAdapter(activityLogs)
        recyclerView.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener {
            ambilData(uid)
        }

        ambilData(uid)

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_log
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_log -> true
                else -> false
            }
        }
    }

    private fun ambilData(uid: String) {
        swipeRefreshLayout.isRefreshing = true

        val aktivitasRef = database.child("aktivitas").child(uid)
        aktivitasRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                activityLogs.clear()

                for (dataSnapshot in snapshot.children) {
                    val timestamp = dataSnapshot.key ?: continue

                    val formattedTime = try {
                        val sdfInput = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        val sdfOutput = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                        val date = sdfInput.parse(timestamp)
                        sdfOutput.format(date ?: Date())
                    } catch (e: Exception) {
                        timestamp
                    }

                    val distance = dataSnapshot.child("jarak").getValue(Double::class.java) ?: 0.0
                    val speed = dataSnapshot.child("kecepatan").getValue(Double::class.java) ?: 0.0
                    val detak = dataSnapshot.child("detakJantung").getValue(Int::class.java) ?: 0
                    val suhu = dataSnapshot.child("suhuTubuh").getValue(Int::class.java) ?: 0

                    val log = ActivityLog(
                        formattedTime,
                        String.format("%.2f km", distance),
                        String.format("%.2f km/h", speed),
                        "$detak bpm",
                        "$suhu°"
                    )
                    activityLogs.add(log)
                }

                activityLogs.reverse()
                adapter.notifyDataSetChanged()
                swipeRefreshLayout.isRefreshing = false
            }

            override fun onCancelled(error: DatabaseError) {
                swipeRefreshLayout.isRefreshing = false
                Toast.makeText(this@LogActivity, "Gagal mengambil data log", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
