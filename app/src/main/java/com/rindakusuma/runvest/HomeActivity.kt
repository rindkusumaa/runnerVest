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

    // UI Components
    private lateinit var heartRateTextView: TextView
    private lateinit var temperatureTextView: TextView
    private lateinit var speedTextView: TextView
    private lateinit var distanceTextView: TextView
    private lateinit var greetingTextView: TextView
    private lateinit var logoutIcon: ImageView
    private lateinit var bottomNavigation: BottomNavigationView

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

        // Initialize FCM
        setupFirebaseMessaging()

        // Check and request notification permission
        checkNotificationPermission()

        // Initialize UI components
        initUI()

        val uid = auth.currentUser?.uid
        if (uid == null) {
            handleUnauthenticatedUser()
            return
        }

        // Setup Firebase Realtime Database
        setupFirebaseDatabase(uid)

        // Setup event listeners
        setupEventListeners()
    }

    private fun setupFirebaseMessaging() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d("FCM", "FCM Token: $token")

            // You can send this token to your server if needed
            sendTokenToServer(token)
        }

        // Subscribe to topic for broadcast messages
        FirebaseMessaging.getInstance().subscribeToTopic("alerts")
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FCM", "Subscribe to alerts topic failed", task.exception)
                }
            }
    }

    private fun sendTokenToServer(token: String) {
        // Implement your logic to send token to server
        val uid = auth.currentUser?.uid
        uid?.let {
            database.child("users").child(it).child("fcmToken").setValue(token)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) -> {
                    // Explain why you need the permission
                    Toast.makeText(
                        this,
                        "Notifikasi diperlukan untuk menerima peringatan kesehatan",
                        Toast.LENGTH_LONG
                    ).show()
                    requestNotificationPermission()
                }
                else -> {
                    requestNotificationPermission()
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            NOTIFICATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun initUI() {
        heartRateTextView = findViewById(R.id.heartRateTextView)
        temperatureTextView = findViewById(R.id.temperatureTextView)
        speedTextView = findViewById(R.id.speedTextView)
        distanceTextView = findViewById(R.id.distanceTextView)
        greetingTextView = findViewById(R.id.greetingTextView)
        logoutIcon = findViewById(R.id.logoutIcon)
        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    private fun handleUnauthenticatedUser() {
        Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun setupFirebaseDatabase(uid: String) {
        // Set UID to config so ESP32 knows which user is active
        database.child("config").child("activeDeviceUid").setValue(uid)

        // Get user name
        database.child("users").child(uid).child("name").get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.getValue(String::class.java) ?: "Runner"
                greetingTextView.text = "Good day, $name 👋"
            }
            .addOnFailureListener {
                greetingTextView.text = "Hello, ${auth.currentUser?.email} 👋"
            }

        // Get latest activity data
        fetchLatestActivityData(uid)
    }

    private fun fetchLatestActivityData(uid: String) {
        val aktivitasRef = database.child("aktivitas").child(uid)
        aktivitasRef.orderByKey().limitToLast(1)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val latestSnapshot = snapshot.children.firstOrNull()
                    latestSnapshot?.let { updateUIWithActivityData(it) }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@HomeActivity,
                        "Gagal memuat data aktivitas",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun updateUIWithActivityData(snapshot: DataSnapshot) {
        // Heart rate
        val detak = snapshot.child("detakJantung").getValue(Int::class.java)
        heartRateTextView.text = "${detak ?: "-"} bpm"
        if (detak != null && detak > 190) {
            showHealthAlert("Peringatan Kesehatan", "Detak jantung terlalu tinggi!")
        }

        // Temperature
        val suhu = snapshot.child("suhuTubuh").getValue(Double::class.java)?.toInt()
        temperatureTextView.text = "${suhu ?: "-"}°"
        if (suhu != null && suhu >= 42) {
            showHealthAlert("Peringatan Kesehatan", "Suhu tubuh terlalu tinggi!")
        }

        // Speed
        val speed = snapshot.child("kecepatan").getValue(Double::class.java)
        val speedFormatted = if (speed != null) String.format("%.2f", speed) else "-"
        speedTextView.text = "$speedFormatted km/jam"

        // Distance
        val jarak = snapshot.child("jarak").getValue(Double::class.java)
        val jarakFormatted = if (jarak != null) String.format("%.2f", jarak) else "-"
        distanceTextView.text = "$jarakFormatted km"
    }

    private fun setupEventListeners() {
        // Logout
        logoutIcon.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Bottom navigation
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
        // Show in-app toast
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        // Show notification
        showNotification(title, message)

        // You could also trigger vibration or other alerts here
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

        // Use unique ID for each notification
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    Toast.makeText(this, "Izin notifikasi diberikan", Toast.LENGTH_SHORT).show()
                } else {
                    // Permission denied
                    Toast.makeText(
                        this,
                        "Izin notifikasi ditolak, Anda mungkin melewatkan peringatan penting",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}