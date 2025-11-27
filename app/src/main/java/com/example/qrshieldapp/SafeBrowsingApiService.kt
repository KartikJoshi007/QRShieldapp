package com.example.qrshieldapp

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface SafeBrowsingApiService {
    @POST("v4/threatMatches:find")
    suspend fun findThreatMatches(
        @Query("key") apiKey: String,
        @Body body: ThreatMatchesRequest
    ): Response<ThreatMatchesResponse>
}
