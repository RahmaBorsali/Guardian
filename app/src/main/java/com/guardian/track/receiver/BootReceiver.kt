package com.guardian.track.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import com.guardian.track.service.SurveillanceService
import com.guardian.track.worker.ServiceRestartWorker

/**
 * Static BroadcastReceiver registered in manifest for ACTION_BOOT_COMPLETED.
 * Restarts the SurveillanceService after device reboot.
 *
 * Android 12+ Compatibility:
 * Since Android 12 (API 31), apps cannot start foreground services from background
 * (BroadcastReceiver) except in specific exemptions. BOOT_COMPLETED is an allowed
 * exemption, but as a robust fallback, we also use WorkManager with setExpedited()
 * which is the recommended approach for API 31+.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d(TAG, "Boot completed — restarting surveillance service")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ : Use WorkManager with setExpedited() as fallback
            // BOOT_COMPLETED is an exemption but WorkManager is more robust
            Log.d(TAG, "API ${Build.VERSION.SDK_INT} — Using WorkManager for service restart")

            val workRequest = OneTimeWorkRequestBuilder<ServiceRestartWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            androidx.work.WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    ServiceRestartWorker.WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
        } else {
            // Pre-Android 12: Direct service start is allowed
            try {
                SurveillanceService.startService(context)
                Log.d(TAG, "Service started directly (pre-API 31)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service directly, falling back to WorkManager", e)
                val workRequest = OneTimeWorkRequestBuilder<ServiceRestartWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()

                androidx.work.WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                        ServiceRestartWorker.WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        workRequest
                    )
            }
        }
    }
}
