package com.guardian.track.domain.model

/**
 * Domain model representing an emergency contact for the UI layer.
 */
data class EmergencyContact(
    val id: Long = 0,
    val name: String,
    val phoneNumber: String
)
