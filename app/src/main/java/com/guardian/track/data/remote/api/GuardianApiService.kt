package com.guardian.track.data.remote.api

import com.guardian.track.data.remote.dto.IncidentDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit API service for GuardianTrack remote sync.
 * Uses suspend functions for coroutine integration.
 */
interface GuardianApiService {

    @POST("alerts")
    suspend fun sendAlert(@Body incident: IncidentDto): Response<IncidentDto>

    @GET("alerts")
    suspend fun getAlerts(): Response<List<IncidentDto>>
}
