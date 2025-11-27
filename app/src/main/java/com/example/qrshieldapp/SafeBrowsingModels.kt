package com.example.qrshieldapp

import com.squareup.moshi.JsonClass

// Request models
@JsonClass(generateAdapter = true)
data class ThreatEntry(val url: String)

@JsonClass(generateAdapter = true)
data class ThreatInfo(
    val threatTypes: List<String>,
    val platformTypes: List<String>,
    val threatEntryTypes: List<String>,
    val threatEntries: List<ThreatEntry>
)

@JsonClass(generateAdapter = true)
data class ClientInfo(val clientId: String, val clientVersion: String)

@JsonClass(generateAdapter = true)
data class ThreatMatchesRequest(val client: ClientInfo, val threatInfo: ThreatInfo)

// Response models
@JsonClass(generateAdapter = true)
data class ThreatMatch(val threatType: String?, val platformType: String?, val threat: ThreatEntry?)

@JsonClass(generateAdapter = true)
data class ThreatMatchesResponse(val matches: List<ThreatMatch>?)
