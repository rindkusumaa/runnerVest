package com.rindakusuma.runvest

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import android.content.Intent
import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private lateinit var heartRateTextView: TextView
    private lateinit var temperatureTextView: TextView
    private lateinit var speedTextView: TextView
    private lateinit var distanceTextView: TextView
    private lateinit var greetingTextView: TextView
    private lateinit var logoutIcon: ImageView
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        // Permission untuk notifikasi (Android 13+)
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

        // Inisialisasi UI
        heartRateTextView = findViewById(R.id.heartRateTextView)
        temperatureTextView = findViewById(R.id.temperatureTextView)
        speedTextView = findViewById(R.id.speedTextView)
        distanceTextView = findViewById(R.id.distanceTextView)
        greetingTextView = findViewById(R.id.greetingTextView)
        logoutIcon = findViewById(R.id.logoutIcon)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Ambil nama user dari profile
        database.child("users").child(uid).child("profile").child("name").get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.getValue(String::class.java) ?: "Runner"
                greetingTextView.text = "Good day, $name 👋"
            }
            .addOnFailureListener {
                greetingTextView.text = "Hello, ${auth.currentUser?.email} 👋"
            }

        // Ambil data aktivitas terbaru
        getLatestAktivitasSnapshot(uid) { latestSnapshot ->
            if (latestSnapshot == null) {
                Toast.makeText(this, "Belum ada data aktivitas", Toast.LENGTH_SHORT).show()
                return@getLatestAktivitasSnapshot
            }

            // BPM
            val bpm = latestSnapshot.child("pulse-sensor/bpm").getValue(Int::class.java)
            heartRateTextView.text = "${bpm ?: "-"} bpm"
            if (bpm != null && bpm > 190) {
                showNotification("Peringatan Kesehatan", "Detak jantung terlalu tinggi!")
            }

            // Suhu tubuh
            val suhu = latestSnapshot.child("sensor-suhu/suhu-tubuh").getValue(Int::class.java)
            temperatureTextView.text = "${suhu ?: "-"}°"
            if (suhu != null && suhu >= 42) {
                showNotification("Peringatan Kesehatan", "Suhu tubuh terlalu tinggi!")
            }

            // Kecepatan
            val speed = latestSnapshot.child("GPS/speed").getValue(Double::class.java)
            val speedFormatted = if (speed != null) String.format("%.2f", speed) else "-"
            speedTextView.text = "$speedFormatted km/jam"

            // Jarak
            val jarak = latestSnapshot.child("GPS/jarak").getValue(Double::class.java)
            val jarakFormatted = if (jarak != null) String.format("%.2f", jarak) else "-"
            distanceTextView.text = "$jarakFormatted km"
        }

        // Logout
        logoutIcon.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Navigasi
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_log -> {
                    startActivity(Intent(this, LogActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    // Fungsi bantu: ambil aktivitas terakhir berdasarkan timestamp
    private fun getLatestAktivitasSnapshot(uid: String, callback: (DataSnapshot?) -> Unit) {
        val aktivitasRef = database.child("users").child(uid).child("aktivitas")
        aktivitasRef.orderByKey().limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val latestSnapshot = snapshot.children.firstOrNull()
                    callback(latestSnapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback(null)
                }
            })
    }

    // Tampilkan notifikasi
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
            .setSmallIcon(R.drawable.icon_warning)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(1, notification)
    }
}
