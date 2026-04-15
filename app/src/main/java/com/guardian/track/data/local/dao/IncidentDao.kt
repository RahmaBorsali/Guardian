package com.guardian.track.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.guardian.track.data.local.entity.IncidentEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for incident CRUD operations.
 * Returns Flow for reactive UI updates and suspend functions for write operations.
 */
@Dao
interface IncidentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: IncidentEntity): Long

    @Query("SELECT * FROM incidents ORDER BY timestamp DESC")
    fun getAllIncidents(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedIncidents(): List<IncidentEntity>

    @Query("UPDATE incidents SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Long)

    @Query("DELETE FROM incidents WHERE id = :id")
    suspend fun deleteIncident(id: Long)

    @Query("DELETE FROM incidents")
    suspend fun deleteAllIncidents()

    @Query("SELECT COUNT(*) FROM incidents")
    fun getIncidentCount(): Flow<Int>
}
