package com.rindakusuma.runvest

import android.content.Intent
import android.os.Bundle
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import android.graphics.Color
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter

class LogActivity : AppCompatActivity() {

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ActivityLogAdapter
    private lateinit var spinnerFilter: Spinner
    private lateinit var chartSensor: LineChart
    private val activityLogs = mutableListOf<ActivityLog>()
    private var fullLogs: List<ActivityLogRaw> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        spinnerFilter = findViewById(R.id.spinnerFilter)
        chartSensor = findViewById(R.id.chartSensor)

        val filterAdapter = ArrayAdapter.createFromResource(
            this, R.array.filter_options, android.R.layout.simple_spinner_item
        )
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilter.adapter = filterAdapter

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val filter = parent.getItemAtPosition(pos).toString()
                tampilkanDataDenganFilter(filter)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

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
                val logs = mutableListOf<ActivityLogRaw>()

                for (dataSnapshot in snapshot.children) {
                    val timestamp = dataSnapshot.key ?: continue
                    val distance = dataSnapshot.child("jarak").getValue(Double::class.java) ?: 0.0
                    val speed = dataSnapshot.child("kecepatan").getValue(Double::class.java) ?: 0.0
                    val detak = dataSnapshot.child("detakJantung").getValue(Int::class.java) ?: 0
                    val suhu = dataSnapshot.child("suhuTubuh").getValue(Int::class.java) ?: 0

                    logs.add(ActivityLogRaw(timestamp, distance, speed, detak, suhu))
                }
                fullLogs = logs
                tampilkanDataDenganFilter(spinnerFilter.selectedItem.toString())
                swipeRefreshLayout.isRefreshing = false
            }

            override fun onCancelled(error: DatabaseError) {
                swipeRefreshLayout.isRefreshing = false
                Toast.makeText(this@LogActivity, "Gagal mengambil data log", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun tampilkanDataDenganFilter(filter: String) {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        val today = LocalDate.now()

        val filtered = fullLogs.filter {
            val date = try {
                LocalDateTime.parse(it.timestamp, formatter).toLocalDate()
            } catch (e: Exception) {
                null
            }
            when (filter) {
                "Harian" -> date == today
                "Mingguan" -> date != null && date >= today.minusDays(7)
                "Bulanan" -> date != null && date.month == today.month && date.year == today.year
                else -> true
            }

        }

        tampilkanRecyclerView(filtered)
        tampilkanGrafik(filtered)
    }

    private fun tampilkanRecyclerView(logs: List<ActivityLogRaw>) {
        activityLogs.clear()
        val sdfOutput = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        val sdfInput = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

        logs.forEach {
            val formatted = try {
                val date = sdfInput.parse(it.timestamp)
                sdfOutput.format(date ?: Date())
            } catch (e: Exception) {
                it.timestamp
            }
            activityLogs.add(
                ActivityLog(
                    formatted,
                    String.format("%.2f km", it.distance),
                    String.format("%.2f km/h", it.speed),
                    "${it.bpm} bpm",
                    "${it.suhu}°"
                )
            )
        }

        activityLogs.reverse()
        adapter.notifyDataSetChanged()
    }

    private fun tampilkanGrafik(logs: List<ActivityLogRaw>) {
        val bpmEntries = ArrayList<Entry>()
        val suhuEntries = ArrayList<Entry>()

        logs.forEachIndexed { index, log ->
            bpmEntries.add(Entry(index.toFloat(), log.bpm.toFloat()))
            suhuEntries.add(Entry(index.toFloat(), log.suhu.toFloat()))
        }

        val bpmSet = LineDataSet(bpmEntries, "Detak Jantung (BPM)").apply {
            color = Color.RED
            valueTextSize = 8f
            setCircleColor(Color.RED)
        }

        val suhuSet = LineDataSet(suhuEntries, "Suhu Tubuh (°C)").apply {
            color = Color.BLUE
            valueTextSize = 8f
            setCircleColor(Color.BLUE)
        }

        val lineData = LineData(bpmSet, suhuSet)
        chartSensor.data = lineData
        chartSensor.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chartSensor.axisRight.isEnabled = false
        chartSensor.description.text = ""
        chartSensor.invalidate()
    }
}
