package com.guardian.track.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.guardian.track.GuardianTrackApp
import com.guardian.track.R
import com.guardian.track.data.local.GuardianDatabase
import com.guardian.track.data.local.entity.IncidentEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Static BroadcastReceiver registered in manifest for ACTION_BATTERY_LOW.
 * When battery is critically low:
 * 1. Records a BATTERY incident in Room database
 * 2. Sends a last-resort notification to the user
 */
class BatteryReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BatteryReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BATTERY_LOW) return

        Log.w(TAG, "🔋 Battery critically low — recording incident")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get Room database instance directly (receiver has no Hilt injection)
                val database = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    GuardianDatabase::class.java,
                    GuardianDatabase.DATABASE_NAME
                ).build()

                val incident = IncidentEntity(
                    timestamp = System.currentTimeMillis(),
                    type = "BATTERY",
                    latitude = 0.0,
                    longitude = 0.0,
                    isSynced = false
                )

                database.incidentDao().insertIncident(incident)
                Log.d(TAG, "Battery critical incident saved to Room")

                // Send last-resort notification
                sendBatteryNotification(context)

            } catch (e: Exception) {
                Log.e(TAG, "Error recording battery incident", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun sendBatteryNotification(context: Context) {
        val notification = NotificationCompat.Builder(context, GuardianTrackApp.CHANNEL_ALERTS)
            .setContentTitle("⚠️ Batterie Critique")
            .setContentText("Niveau de batterie très faible. Incident enregistré. Rechargez votre appareil immédiatement.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(2001, notification)
    }
}
