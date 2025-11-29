package com.example.qrshieldapp

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class HistoryItem(
    var id: String = "",
    val url: String = "",
    val isMalicious: Boolean = false,
    val mlScore: Float = 0.0f,
    val apiResult: Boolean = false,
    val geminiCategory: String? = null,
    val geminiSummary: String? = null,
    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val userId: String? = null
)
