package com.guardian.track.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.core.app.NotificationCompat
import com.guardian.track.GuardianTrackApp
import com.guardian.track.R
import com.guardian.track.service.SurveillanceService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager Worker to restart SurveillanceService after boot on Android 12+.
 * Used as the recommended solution for background service start restrictions
 * introduced in API 31. setExpedited() ensures fast execution while complying
 * with the new restrictions.
 */
@HiltWorker
class ServiceRestartWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "guardian_service_restart"
        private const val TAG = "ServiceRestartWorker"
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, GuardianTrackApp.CHANNEL_SURVEILLANCE)
            .setContentTitle("GuardianTrack")
            .setContentText("Redémarrage du service de surveillance...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setSilent(true)
            .build()
        return ForegroundInfo(3002, notification)
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Restarting SurveillanceService via WorkManager")
            SurveillanceService.startService(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restart service", e)
            Result.retry()
        }
    }
}
