package com.guardian.track.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a security incident.
 * Stores fall detections, battery critical events, and manual alerts.
 */
@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val type: String, // FALL | BATTERY | MANUAL
    val latitude: Double,
    val longitude: Double,
    val isSynced: Boolean = false
)
