package com.guardian.track.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guardian.track.data.local.PreferencesManager
import com.guardian.track.data.repository.ContactRepository
import com.guardian.track.domain.model.EmergencyContact
import com.guardian.track.security.SecurePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Settings screen.
 * Manages user preferences and emergency contacts.
 */
data class SettingsUiState(
    val sensitivityThreshold: Float = PreferencesManager.DEFAULT_SENSITIVITY_THRESHOLD,
    val isDarkMode: Boolean = PreferencesManager.DEFAULT_DARK_MODE,
    val emergencyNumber: String = "",
    val isServiceEnabled: Boolean = PreferencesManager.DEFAULT_SERVICE_ENABLED,
    val contacts: List<EmergencyContact> = emptyList(),
    val showAddContactDialog: Boolean = false,
    val toastMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val securePreferences: SecurePreferences,
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val contacts = contactRepository.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            launch {
                preferencesManager.sensitivityThreshold.collect { value ->
                    _uiState.value = _uiState.value.copy(sensitivityThreshold = value)
                }
            }
            launch {
                preferencesManager.isDarkMode.collect { value ->
                    _uiState.value = _uiState.value.copy(isDarkMode = value)
                }
            }
            launch {
                preferencesManager.emergencyNumber.collect { value ->
                    _uiState.value = _uiState.value.copy(emergencyNumber = value)
                }
            }

            launch {
                preferencesManager.isServiceEnabled.collect { value ->
                    _uiState.value = _uiState.value.copy(isServiceEnabled = value)
                }
            }
        }
    }

    fun updateSensitivity(value: Float) {
        viewModelScope.launch {
            preferencesManager.setSensitivityThreshold(value)
        }
    }

    fun updateDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDarkMode(enabled)
        }
    }

    fun updateEmergencyNumber(number: String) {
        viewModelScope.launch {
            preferencesManager.setEmergencyNumber(number)
            // Also store encrypted copy
            securePreferences.setEmergencyNumber(number)
        }
    }


    fun addContact(name: String, phoneNumber: String) {
        viewModelScope.launch {
            try {
                contactRepository.addContact(
                    EmergencyContact(name = name, phoneNumber = phoneNumber)
                )
                _uiState.value = _uiState.value.copy(
                    showAddContactDialog = false,
                    toastMessage = "Contact ajouté"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    toastMessage = "Erreur: ${e.message}"
                )
            }
        }
    }

    fun deleteContact(id: Long) {
        viewModelScope.launch {
            contactRepository.deleteContact(id)
        }
    }

    fun showAddContactDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAddContactDialog = show)
    }

    fun clearToast() {
        _uiState.value = _uiState.value.copy(toastMessage = null)
    }
}
