package com.guardian.track.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Network DTO for sending incident alerts to the remote API.
 * Separated from Room Entity and Domain Model per clean architecture.
 */
data class IncidentDto(
    @SerializedName("id")
    val id: Long? = null,
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("type")
    val type: String,
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("deviceId")
    val deviceId: String = ""
)
