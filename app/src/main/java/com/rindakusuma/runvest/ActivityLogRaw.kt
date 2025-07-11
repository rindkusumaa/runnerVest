package com.rindakusuma.runvest

data class ActivityLogRaw(
    val timestamp: String,
    val distance: Double,
    val speed: Double,
    val bpm: Int,
    val suhu: Int
)
