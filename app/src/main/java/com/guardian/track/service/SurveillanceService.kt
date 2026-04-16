package com.guardian.track.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.IBinder
import android.os.PowerManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.guardian.track.GuardianTrackApp
import com.guardian.track.R
import com.guardian.track.data.local.PreferencesManager
import com.guardian.track.data.local.dao.IncidentDao
import com.guardian.track.data.local.entity.IncidentEntity
import com.guardian.track.data.repository.IncidentRepository
import com.guardian.track.domain.model.IncidentType
import com.guardian.track.security.SecurePreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sqrt

/**
 * Foreground Service that continuously monitors accelerometer data
 * and implements fall detection via free-fall + impact algorithm.
 *
 * Algorithm:
 * Phase 1 (Free-fall): magnitude < 3 m/s² for > 100ms
 * Phase 2 (Impact): magnitude > threshold (default 15 m/s²) within 200ms window
 *
 * Sensor callbacks run on Dispatchers.Default to avoid blocking the main thread.
 */
@AndroidEntryPoint
class SurveillanceService : Service(), SensorEventListener {

    @Inject lateinit var incidentRepository: IncidentRepository
    @Inject lateinit var incidentDao: IncidentDao
    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var securePreferences: SecurePreferences
    @Inject lateinit var fusedLocationClient: FusedLocationProviderClient

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // Fall detection state machine
    private var isInFreeFall = false
    private var freeFallStartTime = 0L
    private var freeFallConfirmed = false
    private var impactWindowStart = 0L

    // Configurable threshold
    private var impactThreshold = PreferencesManager.DEFAULT_SENSITIVITY_THRESHOLD

    // Prevent rapid duplicate detections
    private var lastDetectionTime = 0L
    private val detectionCooldownMs = 5000L // 5 seconds between detections

    companion object {
        private const val TAG = "SurveillanceService"
        private const val NOTIFICATION_ID = 1001
        private const val FREE_FALL_THRESHOLD = 3.0f   // m/s²
        private const val FREE_FALL_DURATION_MS = 100L  // 100ms minimum free-fall
        private const val IMPACT_WINDOW_MS = 200L       // 200ms window after free-fall

        fun startService(context: Context) {
            val intent = Intent(context, SurveillanceService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, SurveillanceService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        // Acquire partial wake lock to keep sensors running
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "GuardianTrack::SurveillanceWakeLock"
        ).apply {
            acquire(10 * 60 * 1000L) // 10 minutes max
        }

        // Initialize sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Register listener with SENSOR_DELAY_GAME for responsive detection
        accelerometer?.let { sensor ->
            sensorManager?.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

        // Observe threshold changes from settings
        serviceScope.launch {
            preferencesManager.sensitivityThreshold.collect { threshold ->
                impactThreshold = threshold
                Log.d(TAG, "Updated impact threshold: $threshold m/s²")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "Foreground service started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.unregisterListener(this)
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed")
    }

    // ── SensorEventListener ──────────────────────────────────────

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]
        val magnitude = sqrt((ax * ax + ay * ay + az * az).toDouble()).toFloat()
        val currentTime = System.currentTimeMillis()

        // Broadcast sensor data for UI display
        sendSensorBroadcast(magnitude, ax, ay, az)

        // ── Phase 1: Free-fall detection ──
        if (magnitude < FREE_FALL_THRESHOLD) {
            if (!isInFreeFall) {
                isInFreeFall = true
                freeFallStartTime = currentTime
                freeFallConfirmed = false
            } else if (!freeFallConfirmed && (currentTime - freeFallStartTime) > FREE_FALL_DURATION_MS) {
                freeFallConfirmed = true
                impactWindowStart = currentTime
                Log.d(TAG, "Free-fall confirmed (${currentTime - freeFallStartTime}ms)")
            }
        } else {
            // ── Phase 2: Impact detection ──
            if (freeFallConfirmed) {
                val timeSinceFreeFall = currentTime - (freeFallStartTime + FREE_FALL_DURATION_MS)
                if (timeSinceFreeFall <= IMPACT_WINDOW_MS && magnitude > impactThreshold) {
                    // Cooldown check
                    if (currentTime - lastDetectionTime > detectionCooldownMs) {
                        lastDetectionTime = currentTime
                        Log.w(TAG, "🚨 FALL DETECTED! Magnitude: $magnitude m/s²")
                        onFallDetected()
                    }
                }
            }
            // Reset state
            isInFreeFall = false
            freeFallConfirmed = false
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for fall detection
    }

    // ── Fall detection response ──────────────────────────────────

    private fun onFallDetected() {
        serviceScope.launch {
            try {
                // Get current location
                val location = getCurrentLocation()
                val lat = location?.latitude ?: 0.0
                val lng = location?.longitude ?: 0.0

                // Record incident via repository (offline-first)
                incidentRepository.recordIncident(
                    type = IncidentType.FALL,
                    latitude = lat,
                    longitude = lng
                )

                // Send emergency notification
                sendAlertNotification("Chute détectée", "Une chute a été détectée à ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.FRANCE).format(java.util.Date())}")

                // Send SMS (or simulate)
                sendEmergencySms("ALERTE GuardianTrack: Chute détectée! Position: $lat, $lng")

                Log.d(TAG, "Fall incident recorded: lat=$lat, lng=$lng")
            } catch (e: Exception) {
                Log.e(TAG, "Error recording fall incident", e)
            }
        }
    }

    private suspend fun sendEmergencySms(message: String) {
        val emergencyNumber = securePreferences.getEmergencyNumber()

        if (emergencyNumber.isBlank()) {
            Log.w(TAG, "No emergency number configured")
            return
        }

        // Real mode: send actual SMS
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    getSystemService(android.telephony.SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                smsManager?.sendTextMessage(emergencyNumber, null, message, null, null)
                Log.d(TAG, "SMS envoyé à $emergencyNumber")
                
                // Add notification that SMS was sent
                val maskedNumber = if (emergencyNumber.length > 4) {
                    "*****" + emergencyNumber.substring(emergencyNumber.length - 4) // Example: *****1234
                } else {
                    "*****"
                }
                sendAlertNotification("SMS Envoyé", "Un SMS d'alerte a été envoyé au numéro $maskedNumber")
            } catch (e: Exception) {
                Log.e(TAG, "Erreur envoi SMS", e)
            }
        } else {
            Log.w(TAG, "Permission SMS non accordée")
        }
    }

    // ── Location ─────────────────────────────────────────────────

    private suspend fun getCurrentLocation(): Location? {
        return try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                val locationManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            } else {
                Log.w(TAG, "Location permission not granted, using sentinel values")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location", e)
            null
        }
    }

    // ── Notifications ────────────────────────────────────────────

    private fun createForegroundNotification(): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, GuardianTrackApp.CHANNEL_SURVEILLANCE)
            .setContentTitle("GuardianTrack Actif")
            .setContentText("Surveillance en cours — Protection active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun sendAlertNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(this, GuardianTrackApp.CHANNEL_ALERTS)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }



    // ── Sensor data broadcast for UI ─────────────────────────────

    private fun sendSensorBroadcast(magnitude: Float, ax: Float, ay: Float, az: Float) {
        val intent = Intent(ACTION_SENSOR_UPDATE).apply {
            putExtra(EXTRA_MAGNITUDE, magnitude)
            putExtra(EXTRA_AX, ax)
            putExtra(EXTRA_AY, ay)
            putExtra(EXTRA_AZ, az)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    object SensorActions {
        const val ACTION_SENSOR_UPDATE = "com.guardian.track.SENSOR_UPDATE"
        const val EXTRA_MAGNITUDE = "magnitude"
        const val EXTRA_AX = "ax"
        const val EXTRA_AY = "ay"
        const val EXTRA_AZ = "az"
    }
}

// Top-level constants for broadcast actions
const val ACTION_SENSOR_UPDATE = "com.guardian.track.SENSOR_UPDATE"
const val EXTRA_MAGNITUDE = "magnitude"
const val EXTRA_AX = "ax"
const val EXTRA_AY = "ay"
const val EXTRA_AZ = "az"
