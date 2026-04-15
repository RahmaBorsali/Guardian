package com.guardian.track.ui.viewmodel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guardian.track.data.local.PreferencesManager
import com.guardian.track.data.repository.IncidentRepository
import com.guardian.track.domain.model.IncidentType
import com.guardian.track.service.ACTION_SENSOR_UPDATE
import com.guardian.track.service.EXTRA_AX
import com.guardian.track.service.EXTRA_AY
import com.guardian.track.service.EXTRA_AZ
import com.guardian.track.service.EXTRA_MAGNITUDE
import com.guardian.track.service.SurveillanceService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Dashboard screen.
 * Exposes immutable StateFlow<DashboardUiState> to the UI.
 * Survives configuration changes (rotation) without data loss.
 */
data class DashboardUiState(
    val isServiceRunning: Boolean = false,
    val sensorMagnitude: Float = 9.81f,
    val sensorAx: Float = 0f,
    val sensorAy: Float = 0f,
    val sensorAz: Float = 9.81f,
    val batteryLevel: Int = 100,
    val isGpsEnabled: Boolean = false,
    val incidentCount: Int = 0,
    val isAlertSending: Boolean = false,
    val lastAlertMessage: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val incidentRepository: IncidentRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val isServiceEnabled = preferencesManager.isServiceEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var sensorReceiver: BroadcastReceiver? = null
    private var gpsReceiver: BroadcastReceiver? = null

    init {
        observeIncidentCount()
        updateBatteryLevel()
        checkInitialGpsStatus()
        registerSensorReceiver()
        registerGpsReceiver()
    }

    private fun observeIncidentCount() {
        viewModelScope.launch {
            incidentRepository.getIncidentCount().collect { count ->
                _uiState.value = _uiState.value.copy(incidentCount = count)
            }
        }
    }

    private fun updateBatteryLevel() {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        _uiState.value = _uiState.value.copy(batteryLevel = level)
    }

    private fun checkInitialGpsStatus() {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val isEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        _uiState.value = _uiState.value.copy(isGpsEnabled = isEnabled)
    }

    private fun registerGpsReceiver() {
        gpsReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == android.location.LocationManager.PROVIDERS_CHANGED_ACTION) {
                    checkInitialGpsStatus()
                }
            }
        }
        val filter = IntentFilter(android.location.LocationManager.PROVIDERS_CHANGED_ACTION)
        context.registerReceiver(gpsReceiver, filter)
    }

    private fun registerSensorReceiver() {
        sensorReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == ACTION_SENSOR_UPDATE) {
                    _uiState.value = _uiState.value.copy(
                        sensorMagnitude = intent.getFloatExtra(EXTRA_MAGNITUDE, 9.81f),
                        sensorAx = intent.getFloatExtra(EXTRA_AX, 0f),
                        sensorAy = intent.getFloatExtra(EXTRA_AY, 0f),
                        sensorAz = intent.getFloatExtra(EXTRA_AZ, 9.81f),
                        isServiceRunning = true
                    )
                }
            }
        }
        val filter = IntentFilter(ACTION_SENSOR_UPDATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(sensorReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(sensorReceiver, filter)
        }
    }

    fun toggleService(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setServiceEnabled(enabled)
            if (enabled) {
                SurveillanceService.startService(context)
                _uiState.value = _uiState.value.copy(isServiceRunning = true)
            } else {
                SurveillanceService.stopService(context)
                _uiState.value = _uiState.value.copy(isServiceRunning = false)
            }
        }
    }

    fun sendManualAlert(latitude: Double = 0.0, longitude: Double = 0.0) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAlertSending = true)
            try {
                incidentRepository.recordIncident(
                    type = IncidentType.MANUAL,
                    latitude = latitude,
                    longitude = longitude
                )
                _uiState.value = _uiState.value.copy(
                    isAlertSending = false,
                    lastAlertMessage = "Alerte enregistrée avec succès"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAlertSending = false,
                    lastAlertMessage = "Erreur: ${e.message}"
                )
            }
        }
    }

    fun clearAlertMessage() {
        _uiState.value = _uiState.value.copy(lastAlertMessage = null)
    }

    fun updateGpsStatus(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isGpsEnabled = enabled)
    }

    override fun onCleared() {
        super.onCleared()
        sensorReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (_: Exception) {}
        }
        gpsReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (_: Exception) {}
        }
    }
}
