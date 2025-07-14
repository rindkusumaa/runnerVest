package com.rindakusuma.runvest

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DetailAtletActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var chart: LineChart
    private lateinit var adapter: LogAtletAdapter
    private val logList = mutableListOf<LogAtlet>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_atlet)

        val deviceId = intent.getStringExtra("deviceId") ?: return
        val spinnerFilter: Spinner = findViewById(R.id.spinnerFilter)
        val logRecyclerView = findViewById<RecyclerView>(R.id.logRecyclerView)
        chart = findViewById(R.id.chartSensor)
        database = FirebaseDatabase.getInstance().reference

        val adapterFilter = ArrayAdapter.createFromResource(
            this, R.array.filter_options, android.R.layout.simple_spinner_item
        )
        adapterFilter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilter.adapter = adapterFilter
        spinnerFilter.setSelection(0)

        spinnerFilter.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                ambilData(deviceId)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        })

        adapter = LogAtletAdapter(logList)
        logRecyclerView.layoutManager = LinearLayoutManager(this)
        logRecyclerView.adapter = adapter

        ambilData(deviceId)
    }

    private fun ambilData(deviceId: String) {
        val ref = database.child("devices").child(deviceId).child("aktivitas")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                logList.clear()
                val entriesBPM = mutableListOf<Entry>()
                val entriesSuhu = mutableListOf<Entry>()
                var index = 0f
                val namaText = findViewById<TextView>(R.id.namaAtletText)
                val refNama = database.child("devices").child(deviceId).child("namaAtlet")
                refNama.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val nama = snapshot.getValue(String::class.java)
                        namaText.text = nama ?: "Atlet"
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })


                for (data in snapshot.children) {
                    val waktu = data.key ?: continue
                    val bpm = data.child("detakJantung").getValue(Int::class.java) ?: 0
                    val suhu = data.child("suhuTubuh").getValue(Int::class.java) ?: 0
                    val kecepatan = data.child("kecepatan").getValue(Double::class.java) ?: 0.0
                    val jarak = data.child("jarak").getValue(Double::class.java) ?: 0.0

                    val log = LogAtlet(waktu, "$bpm bpm", "$suhu°C", String.format("%.2f km/h", kecepatan), String.format("%.2f km", jarak))
                    logList.add(log)

                    entriesBPM.add(Entry(index, bpm.toFloat()))
                    entriesSuhu.add(Entry(index, suhu.toFloat()))
                    index++
                }

                adapter.notifyDataSetChanged()
                tampilkanGrafik(entriesBPM, entriesSuhu)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DetailAtletActivity, "Gagal ambil data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun tampilkanGrafik(bpmList: List<Entry>, suhuList: List<Entry>) {
        val bpmDataSet = LineDataSet(bpmList, "BPM").apply {
            color = android.graphics.Color.RED
            setCircleColor(android.graphics.Color.RED)
            valueTextSize = 10f
        }

        val suhuDataSet = LineDataSet(suhuList, "Suhu").apply {
            color = android.graphics.Color.BLUE
            setCircleColor(android.graphics.Color.BLUE)
            valueTextSize = 10f
        }

        chart.apply {
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            axisRight.isEnabled = false
            description.text = "Riwayat BPM & Suhu"
            data = LineData(bpmDataSet, suhuDataSet)
            invalidate()
        }
    }
}
