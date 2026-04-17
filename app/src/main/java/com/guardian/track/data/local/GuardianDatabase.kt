package com.guardian.track.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.guardian.track.data.local.dao.EmergencyContactDao
import com.guardian.track.data.local.dao.IncidentDao
import com.guardian.track.data.local.entity.EmergencyContactEntity
import com.guardian.track.data.local.entity.IncidentEntity


@Database(
    entities = [IncidentEntity::class, EmergencyContactEntity::class],
    version = 1,
    exportSchema = true
)
abstract class GuardianDatabase : RoomDatabase() {
    abstract fun incidentDao(): IncidentDao
    abstract fun emergencyContactDao(): EmergencyContactDao

    companion object {
        const val DATABASE_NAME = "guardian_track_db"
    }
}
