package com.rindakusuma.runvest

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.*
import android.content.Intent
import com.rindakusuma.runvest.LogActivity
import com.rindakusuma.runvest.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        database = FirebaseDatabase.getInstance().reference

        val heartRateTextView: TextView? = findViewById(R.id.heartRateTextView)
        val temperatureTextView: TextView? = findViewById(R.id.temperatureTextView)
        val speedTextView: TextView? = findViewById(R.id.speedTextView)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    true
                }
                R.id.nav_log -> {
                    startActivity(Intent(this, LogActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }


        // Ambil BPM dari pulse-sensor
        database.child("pulse-sensor").child("bpm")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val bpm = snapshot.getValue(Int::class.java)
                    heartRateTextView?.text = "${bpm ?: "-"} bpm"
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "pulse-sensor: ${error.message}")
                }
            })

        // Ambil suhu objek dari sensor-mlx
        database.child("sensor-mlx").child("suhu-objek")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val suhuObjek = snapshot.getValue(Double::class.java)
                    temperatureTextView?.text = "${suhuObjek ?: "-"}°"
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "sensor-mlx: ${error.message}")
                }
            })

        // Ambil kecepatan dari GPS
        database.child("GPS").child("speed")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val speed = snapshot.getValue(Double::class.java)
                    speedTextView?.text = "${speed ?: "-"} km/jam"
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "GPS: ${error.message}")
                }
            })

    }
}
