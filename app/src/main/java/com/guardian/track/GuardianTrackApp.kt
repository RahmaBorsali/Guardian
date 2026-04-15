package com.guardian.track

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for GuardianTrack.
 * Initializes Hilt dependency injection and notification channels.
 */
@HiltAndroidApp
class GuardianTrackApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val surveillanceChannel = NotificationChannel(
            CHANNEL_SURVEILLANCE,
            "Surveillance Active",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notification persistante indiquant que la surveillance est active"
            setShowBadge(false)
        }

        val alertChannel = NotificationChannel(
            CHANNEL_ALERTS,
            "Alertes de Sécurité",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alertes critiques : chutes détectées, batterie faible"
            enableVibration(true)
            enableLights(true)
        }

        val smsSimChannel = NotificationChannel(
            CHANNEL_SMS_SIM,
            "SMS Simulé",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications simulant l'envoi de SMS en mode test"
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(surveillanceChannel)
        manager.createNotificationChannel(alertChannel)
        manager.createNotificationChannel(smsSimChannel)
    }

    companion object {
        const val CHANNEL_SURVEILLANCE = "surveillance_channel"
        const val CHANNEL_ALERTS = "alerts_channel"
        const val CHANNEL_SMS_SIM = "sms_simulation_channel"
    }
}
