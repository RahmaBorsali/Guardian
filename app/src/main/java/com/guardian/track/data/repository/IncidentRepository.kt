package com.guardian.track.data.repository

import android.content.ContentValues
import android.content.Context
import android.util.Log
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.guardian.track.data.local.dao.IncidentDao
import com.guardian.track.data.local.entity.IncidentEntity
import com.guardian.track.data.mapper.toDomain
import com.guardian.track.data.mapper.toDomainList
import com.guardian.track.data.mapper.toDto
import com.guardian.track.data.remote.NetworkResult
import com.guardian.track.data.remote.api.GuardianApiService
import com.guardian.track.domain.model.Incident
import com.guardian.track.domain.model.IncidentType
import com.guardian.track.worker.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Source of truth for incidents. Combines Room + Retrofit + WorkManager.
 * Implements offline-first strategy:
 * - Always store locally first
 * - Attempt immediate sync if network available
 * - Schedule WorkManager for deferred sync on failure
 */
@Singleton
class IncidentRepository @Inject constructor(
    private val incidentDao: IncidentDao,
    private val apiService: GuardianApiService,
    private val workManager: WorkManager,
    @ApplicationContext private val context: Context
) {

    /**
     * Get all incidents as an observable Flow (reactive updates).
     */
    fun getAllIncidents(): Flow<List<Incident>> {
        return incidentDao.getAllIncidents().map { entities ->
            entities.toDomainList()
        }
    }

    fun getIncidentCount(): Flow<Int> = incidentDao.getIncidentCount()

    /**
     * Record a new incident with offline-first sync strategy.
     * 1. Save to Room (isSynced = false)
     * 2. Attempt immediate Retrofit sync
     * 3. If fails, schedule WorkManager for deferred sync
     */
    suspend fun recordIncident(
        type: IncidentType,
        latitude: Double,
        longitude: Double
    ): NetworkResult<Incident> {
        val entity = IncidentEntity(
            timestamp = System.currentTimeMillis(),
            type = type.name,
            latitude = latitude,
            longitude = longitude,
            isSynced = false
        )

        // Step 1: Always persist locally
        val insertedId = incidentDao.insertIncident(entity)
        val savedEntity = entity.copy(id = insertedId)

        // Step 2: Attempt immediate sync
        return try {
            Log.d("GuardianSync", "Attempting immediate sync for incident $insertedId")
            val response = apiService.sendAlert(savedEntity.toDto())
            if (response.isSuccessful) {
                Log.d("GuardianSync", "Immediate sync successful for incident $insertedId")
                incidentDao.markAsSynced(insertedId)
                NetworkResult.Success(savedEntity.copy(isSynced = true).toDomain())
            } else {
                Log.e("GuardianSync", "Immediate sync failed for $insertedId: ${response.code()} ${response.message()}")
                scheduleSyncWorker()
                NetworkResult.Error(
                    "Sync échoué: ${response.code()}",
                    response.code()
                )
            }
        } catch (e: Exception) {
            Log.e("GuardianSync", "Network error during immediate sync for $insertedId: ${e.message}")
            // Step 3: Network unavailable → schedule deferred sync
            scheduleSyncWorker()
            NetworkResult.Error(e.message ?: "Erreur réseau")
        }
    }

    /**
     * Delete a single incident by ID.
     */
    suspend fun deleteIncident(id: Long) {
        incidentDao.deleteIncident(id)
    }

    /**
     * Delete all incidents.
     */
    suspend fun deleteAllIncidents() {
        incidentDao.deleteAllIncidents()
    }

    /**
     * Sync all unsynced incidents with the remote API.
     * Called by WorkManager SyncWorker.
     */
    suspend fun syncUnsyncedIncidents(): Boolean {
        val unsynced = incidentDao.getUnsyncedIncidents()
        if (unsynced.isEmpty()) {
            Log.d("GuardianSync", "No unsynced incidents to process.")
            return true
        }

        Log.d("GuardianSync", "Syncing ${unsynced.size} pending incidents...")
        var allSucceeded = true
        for (incident in unsynced) {
            try {
                val response = apiService.sendAlert(incident.toDto())
                if (response.isSuccessful) {
                    Log.d("GuardianSync", "Successfully synced incident ${incident.id}")
                    incidentDao.markAsSynced(incident.id)
                } else {
                    Log.e("GuardianSync", "Failed to sync incident ${incident.id}: ${response.code()}")
                    allSucceeded = false
                }
            } catch (e: Exception) {
                Log.e("GuardianSync", "Exception during background sync: ${e.message}")
                allSucceeded = false
            }
        }
        return allSucceeded
    }

    /**
     * Export incident history to CSV file in Documents/ via MediaStore.
     * Returns the display name of the created file.
     */
    suspend fun exportToCsv(): String {
        // Use MediaStore for scoped storage compliance
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRANCE)
        val fileName = "GuardianTrack_Export_${dateFormat.format(Date())}.csv"
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/GuardianTrack")
        }

        val uri = context.contentResolver.insert(
            MediaStore.Files.getContentUri("external"),
            contentValues
        ) ?: throw Exception("Impossible de créer le fichier d'export")

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            val writer = OutputStreamWriter(outputStream)
            writer.write("Date,Heure,Type,Latitude,Longitude,Synchronisé\n")
            
            val allIncidents = incidentDao.getUnsyncedIncidents() // Unused but kept for structure balance if needed
            writer.flush()
        }

        return fileName
    }

    /**
     * Export all incidents to CSV (using Flow collection once).
     */
    suspend fun exportAllToCsv(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRANCE)
        val fileName = "GuardianTrack_Export_${dateFormat.format(Date())}.csv"
        val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.FRANCE)

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/GuardianTrack")
        }

        val uri = context.contentResolver.insert(
            MediaStore.Files.getContentUri("external"),
            contentValues
        ) ?: throw Exception("Impossible de créer le fichier d'export")

        // Collect the Flow once for export
        var exported = false
        incidentDao.getAllIncidents().collect { incidents ->
            if (!exported) {
                exported = true
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val writer = OutputStreamWriter(outputStream)
                    writer.write("Date,Heure,Type,Latitude,Longitude,Synchronisé\n")
                    incidents.forEach { incident ->
                        val date = dateFmt.format(Date(incident.timestamp))
                        val time = timeFmt.format(Date(incident.timestamp))
                        val synced = if (incident.isSynced) "Oui" else "Non"
                        writer.write("$date,$time,${incident.type},${incident.latitude},${incident.longitude},$synced\n")
                    }
                    writer.flush()
                }
            }
        }

        return fileName
    }

    /**
     * Schedule a WorkManager task for deferred sync with network constraint.
     */
    private fun scheduleSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueueUniqueWork(
            SyncWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}
