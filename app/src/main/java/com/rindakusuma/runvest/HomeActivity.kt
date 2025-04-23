package com.rindakusuma.runvest

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.*
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

import com.rindakusuma.runvest.LogActivity
import com.rindakusuma.runvest.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }

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
        database.child("data_sensor").child("pulse-sensor").child("bpm")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val bpm = snapshot.getValue(Int::class.java)
                    heartRateTextView?.text = "${bpm ?: "-"} bpm"

                    // Cek kondisi untuk notifikasi
                    if (bpm != null && bpm > 100) {
                        showNotification("Peringatan Kesehatan", "Detak jantung terlalu tinggi!")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "pulse-sensor: ${error.message}")
                }
            })

        // Ambil suhu tubuh dari sensor-suhu
        database.child("data_sensor").child("sensor-suhu").child("suhu-tubuh")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val suhuTubuh = snapshot.getValue(Double::class.java)
                    temperatureTextView?.text = "${suhuTubuh ?: "-"}°"

                    // Cek kondisi untuk notifikasi
                    if (suhuTubuh != null && suhuTubuh >= 40.0) {
                        showNotification("Peringatan Kesehatan", "Suhu tubuh terlalu tinggi!")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "sensor-suhu: ${error.message}")
                }
            })

        // Ambil kecepatan dari GPS
        database.child("data_sensor").child("GPS").child("speed")
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

    private fun showNotification(title: String, message: String) {
        val channelId = "runvest_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Runvest Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.icon_warning) // Ganti dengan icon kamu
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(1, notification)
    }
}
