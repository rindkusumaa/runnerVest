package com.rindakusuma.runvest

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class DetailAtletActivity : AppCompatActivity() {

    private lateinit var chart: LineChart
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LogAtletAdapter
    private lateinit var namaTextView: TextView
    private val logList = mutableListOf<LogAtlet>()
    private val database = FirebaseDatabase.getInstance().reference
    private lateinit var notificationManager: NotificationManager

    companion object {
        const val CHANNEL_ID = "detail_atlet_alert_channel"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_atlet)

        val uidAtlet = intent.getStringExtra("UID_ATLET") ?: return
        val namaAtlet = intent.getStringExtra("NAMA_ATLET") ?: "Tidak dikenal"

        namaTextView = findViewById(R.id.textViewNamaAtlet)
        chart = findViewById(R.id.chartSensor)
        recyclerView = findViewById(R.id.recyclerViewLogAtlet)

        namaTextView.text = "Atlet: $namaAtlet"
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = LogAtletAdapter(logList)
        recyclerView.adapter = adapter

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        ambilDataAktivitas(uidAtlet)
    }

    private fun ambilDataAktivitas(uid: String) {
        database.child("aktivitas").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    logList.clear()
                    val bpmEntries = ArrayList<Entry>()
                    val suhuEntries = ArrayList<Entry>()
                    var index = 0f

                    val sdfInput = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val sdfOutput = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

                    for (data in snapshot.children) {
                        val waktu = data.key ?: continue
                        val bpm = data.child("detakJantung").getValue(Int::class.java) ?: 0
                        val suhu = data.child("suhuTubuh").getValue(Int::class.java) ?: 0
                        val kecepatan = data.child("kecepatan").getValue(Double::class.java) ?: 0.0
                        val jarak = data.child("jarak").getValue(Double::class.java) ?: 0.0

                        val waktuFormatted = try {
                            val date = sdfInput.parse(waktu)
                            sdfOutput.format(date ?: Date())
                        } catch (e: Exception) {
                            waktu
                        }

                        // Tambahkan ke log list
                        logList.add(
                            LogAtlet(
                                waktuFormatted,
                                "$bpm bpm",
                                "$suhu°C",
                                "%.2f km/h".format(kecepatan),
                                "%.2f km".format(jarak)
                            )
                        )

                        // Tambahkan ke grafik
                        bpmEntries.add(Entry(index, bpm.toFloat()))
                        suhuEntries.add(Entry(index, suhu.toFloat()))
                        index++

                        // Cek kondisi ekstrem
                        if (bpm > 190) {
                            showNotification("Detak Jantung Tinggi", "Detak jantung atlet melebihi 190 bpm.")
                        }

                        if (suhu >= 39) {
                            showNotification("Suhu Tubuh Tinggi", "Suhu tubuh atlet mencapai $suhu°C.")
                        }
                    }

                    logList.reverse()
                    adapter.notifyDataSetChanged()

                    val bpmSet = LineDataSet(bpmEntries, "Detak Jantung").apply {
                        color = Color.RED
                        valueTextSize = 8f
                        setCircleColor(Color.RED)
                    }

                    val suhuSet = LineDataSet(suhuEntries, "Suhu Tubuh").apply {
                        color = Color.BLUE
                        valueTextSize = 8f
                        setCircleColor(Color.BLUE)
                    }

                    val lineData = LineData(bpmSet, suhuSet)
                    chart.data = lineData
                    chart.description.text = ""
                    chart.axisRight.isEnabled = false
                    chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                    chart.invalidate()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@DetailAtletActivity, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Detail Atlet Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi kondisi ekstrem atlet"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon_warning)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val intent = Intent(this, DetailAtletActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
