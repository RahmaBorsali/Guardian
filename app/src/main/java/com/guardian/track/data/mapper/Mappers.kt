package com.guardian.track.data.mapper

import com.guardian.track.data.local.entity.EmergencyContactEntity
import com.guardian.track.data.local.entity.IncidentEntity
import com.guardian.track.data.remote.dto.IncidentDto
import com.guardian.track.domain.model.EmergencyContact
import com.guardian.track.domain.model.Incident
import com.guardian.track.domain.model.IncidentType



// ── Entity → Domain ──────────────────────────────────────────────

fun IncidentEntity.toDomain(): Incident = Incident(
    id = id,
    timestamp = timestamp,
    type = IncidentType.fromString(type),
    latitude = latitude,
    longitude = longitude,
    isSynced = isSynced
)

fun EmergencyContactEntity.toDomain(): EmergencyContact = EmergencyContact(
    id = id,
    name = name,
    phoneNumber = phoneNumber
)

// ── Domain → Entity ──────────────────────────────────────────────

fun Incident.toEntity(): IncidentEntity = IncidentEntity(
    id = id,
    timestamp = timestamp,
    type = type.name,
    latitude = latitude,
    longitude = longitude,
    isSynced = isSynced
)

fun EmergencyContact.toEntity(): EmergencyContactEntity = EmergencyContactEntity(
    id = id,
    name = name,
    phoneNumber = phoneNumber
)

// ── Entity → DTO (for network sync) ─────────────────────────────

fun IncidentEntity.toDto(deviceId: String = ""): IncidentDto = IncidentDto(
    id = id,
    timestamp = timestamp,
    type = type,
    latitude = latitude,
    longitude = longitude,
    deviceId = deviceId
)

// ── DTO → Entity (for server responses) ──────────────────────────

fun IncidentDto.toEntity(): IncidentEntity = IncidentEntity(
    id = id ?: 0,
    timestamp = timestamp,
    type = type,
    latitude = latitude,
    longitude = longitude,
    isSynced = true
)

// ── List extensions ──────────────────────────────────────────────

fun List<IncidentEntity>.toDomainList(): List<Incident> = map { it.toDomain() }
fun List<EmergencyContactEntity>.toDomainContactList(): List<EmergencyContact> = map { it.toDomain() }
