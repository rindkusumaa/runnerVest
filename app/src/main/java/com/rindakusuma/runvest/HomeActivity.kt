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
import android.app.PendingIntent
import android.os.Build
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging

class HomeActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var notificationManager: NotificationManager

    private lateinit var heartRateTextView: TextView
    private lateinit var temperatureTextView: TextView
    private lateinit var speedTextView: TextView
    private lateinit var distanceTextView: TextView
    private lateinit var greetingTextView: TextView
    private lateinit var logoutIcon: ImageView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        const val CHANNEL_ID = "runvest_alerts_channel"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        setupFirebaseMessaging()
        checkNotificationPermission()
        initUI()

        val uid = auth.currentUser?.uid
        if (uid == null) {
            handleUnauthenticatedUser()
            return
        }

        setupFirebaseDatabase(uid)
        setupEventListeners()
    }

    private fun setupFirebaseMessaging() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            sendTokenToServer(token)
        }
        FirebaseMessaging.getInstance().subscribeToTopic("alerts")
    }

    private fun sendTokenToServer(token: String) {
        val uid = auth.currentUser?.uid
        uid?.let {
            database.child("users").child(it).child("fcmToken").setValue(token)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun initUI() {
        heartRateTextView = findViewById(R.id.heartRateTextView)
        temperatureTextView = findViewById(R.id.temperatureTextView)
        speedTextView = findViewById(R.id.speedTextView)
        distanceTextView = findViewById(R.id.distanceTextView)
        greetingTextView = findViewById(R.id.greetingTextView)
        logoutIcon = findViewById(R.id.logoutIcon)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        startButton = findViewById(R.id.btnStart)
        stopButton = findViewById(R.id.btnStop)
    }

    private fun handleUnauthenticatedUser() {
        Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

//    private fun setupFirebaseDatabase(uid: String) {
//        Log.d("FirebaseDebug", "Mengambil data untuk UID: $uid")
//        database.child("users").child(uid).child("profile")
//            .addListenerForSingleValueEvent(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    if (snapshot.exists()) {
//                        val name = snapshot.child("name").getValue(String::class.java) ?: "Runner"
//                        val deviceId = snapshot.child("deviceId").getValue(String::class.java)
//
//                        greetingTextView.text = "Good day, $name 👋"
//
//                        if (deviceId != null) {
//                            fetchLatestActivityData(deviceId)
//                        } else {
//                            Toast.makeText(this@HomeActivity, "Device ID belum diatur untuk pengguna ini", Toast.LENGTH_SHORT).show()
//                        }
//                    } else {
//                        Log.e("FirebaseDebug", "Data pengguna tidak ditemukan untuk UID: $uid")
//                        Toast.makeText(this@HomeActivity, "Data pengguna tidak ditemukan", Toast.LENGTH_SHORT).show()
//                    }
//                }
private fun setupFirebaseDatabase(uid: String) {
    Log.d("FirebaseDebug", "Mengambil data untuk UID: $uid")
    database.child("users").child(uid).child("profile")
        .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val name = snapshot.child("name").getValue(String::class.java) ?: "Runner"
                    greetingTextView.text = "Good day, $name 👋"

                    fetchLatestActivityData(uid)
                } else {
                    Log.e("FirebaseDebug", "Data pengguna tidak ditemukan untuk UID: $uid")
                    Toast.makeText(this@HomeActivity, "Data pengguna tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseDebug", "Error saat mengambil data: ${error.message}")
                    greetingTextView.text = "Hello, ${auth.currentUser ?.email} 👋"
                }
            })
    }

    private fun fetchLatestActivityData(uid: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val aktivitasRef = database.child("aktivitas").child(uid!!)
        aktivitasRef.orderByKey().limitToLast(1)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val latestSnapshot = snapshot.children.firstOrNull()
                        if (latestSnapshot != null) {
                            updateUIWithActivityData(latestSnapshot)
                        } else {
                            Toast.makeText(this@HomeActivity, "Belum ada aktivitas yang terekam", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("FirebaseDebug", "Tidak ada data aktivitas untuk UID: $uid")
                        Toast.makeText(this@HomeActivity, "Tidak ada data aktivitas", Toast.LENGTH_SHORT).show()
                    }
                }
//    private fun fetchLatestActivityData(deviceId: String) {
//        val uid = auth.currentUser ?.uid
//        Log.d("FirebaseDebug", "UID: $uid, Device ID: $deviceId")
//
//        val aktivitasRef = database.child("devices").child(deviceId).child("aktivitas")
//        aktivitasRef.orderByKey().limitToLast(1)
//            .addValueEventListener(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    if (snapshot.exists()) {
//                        val latestSnapshot = snapshot.children.firstOrNull()
//                        if (latestSnapshot != null) {
//                            updateUIWithActivityData(latestSnapshot)
//                        } else {
//                            Toast.makeText(this@HomeActivity, "Belum ada aktivitas yang terekam", Toast.LENGTH_SHORT).show()
//                        }
//                    } else {
//                        Log.e("FirebaseDebug", "Tidak ada data aktivitas untuk deviceId: $deviceId")
//                        Toast.makeText(this@HomeActivity, "Tidak ada data aktivitas", Toast.LENGTH_SHORT).show()
//                    }
//                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseDebug", "Gagal memuat data aktivitas: ${error.message}")
                    Toast.makeText(this@HomeActivity, "Gagal memuat data aktivitas", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateUIWithActivityData(snapshot: DataSnapshot) {
        val detak = snapshot.child("detakJantung").getValue(Int::class.java)
        heartRateTextView.text = "${detak ?: "-"} bpm"
        if (detak != null && detak > 190) showHealthAlert("Peringatan Kesehatan", "Detak jantung terlalu tinggi!")

        val suhu = snapshot.child("suhuTubuh").getValue(Double::class.java)?.toInt()
        temperatureTextView.text = "${suhu ?: "-"}°"
        if (suhu != null && suhu >= 39) showHealthAlert("Peringatan Kesehatan", "Suhu tubuh terlalu tinggi!")

        val speed = snapshot.child("kecepatan").getValue(Double::class.java)
        val speedFormatted = if (speed != null) String.format("%.2f", speed) else "-"
        speedTextView.text = "$speedFormatted km/jam"

        val jarak = snapshot.child("jarak").getValue(Double::class.java)
        val jarakFormatted = if (jarak != null) String.format("%.2f", jarak) else "-"
        distanceTextView.text = "$jarakFormatted km"
    }

    private fun setupEventListeners() {
        startButton.setOnClickListener {
            Toast.makeText(this, "Monitoring dimulai", Toast.LENGTH_SHORT).show()
        }

        stopButton.setOnClickListener {
            Toast.makeText(this, "Monitoring dihentikan", Toast.LENGTH_SHORT).show()
        }

        logoutIcon.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

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

    private fun showHealthAlert(title: String, message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        showNotification(title, message)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "RunVest Health Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for health alerts and notifications"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, message: String) {
        createNotificationChannel()

        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.icon_warning)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
