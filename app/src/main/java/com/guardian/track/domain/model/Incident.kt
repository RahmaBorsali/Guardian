package com.guardian.track.domain.model

/**
 * Domain model representing a security incident for the UI layer.
 * Separated from Room Entity (data layer) and DTO (network layer).
 */
data class Incident(
    val id: Long,
    val timestamp: Long,
    val type: IncidentType,
    val latitude: Double,
    val longitude: Double,
    val isSynced: Boolean
) {
    val formattedDate: String
        get() {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.FRANCE)
            return sdf.format(java.util.Date(timestamp))
        }

    val formattedTime: String
        get() {
            val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.FRANCE)
            return sdf.format(java.util.Date(timestamp))
        }

    val formattedDateTime: String
        get() {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.FRANCE)
            return sdf.format(java.util.Date(timestamp))
        }
}

enum class IncidentType(val label: String) {
    FALL("Chute détectée"),
    BATTERY("Batterie critique"),
    MANUAL("Alerte manuelle");

    companion object {
        fun fromString(value: String): IncidentType {
            return entries.find { it.name == value } ?: MANUAL
        }
    }
}
