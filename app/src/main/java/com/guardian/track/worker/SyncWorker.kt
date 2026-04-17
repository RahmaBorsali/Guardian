package com.guardian.track.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.core.app.NotificationCompat
import com.guardian.track.GuardianTrackApp
import com.guardian.track.R
import com.guardian.track.data.repository.IncidentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject


@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val incidentRepository: IncidentRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "guardian_sync_worker"
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, GuardianTrackApp.CHANNEL_SURVEILLANCE)
            .setContentTitle("Synchronisation en cours")
            .setContentText("Envoi des incidents en attente...")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setSilent(true)
            .build()
        return ForegroundInfo(3001, notification)
    }

    override suspend fun doWork(): Result {
        return try {
            val success = incidentRepository.syncUnsyncedIncidents()
            if (success) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
