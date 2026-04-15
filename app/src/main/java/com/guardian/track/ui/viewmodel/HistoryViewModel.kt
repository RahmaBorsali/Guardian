package com.guardian.track.ui.viewmodel

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guardian.track.data.local.dao.IncidentDao
import com.guardian.track.data.mapper.toDomainList
import com.guardian.track.domain.model.Incident
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for the History screen.
 * Provides paginated incident list, delete, and CSV export functionality.
 */
data class HistoryUiState(
    val isExporting: Boolean = false,
    val exportResult: String? = null,
    val deleteMessage: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val incidentDao: IncidentDao
) : ViewModel() {

    val incidents: StateFlow<List<Incident>> = incidentDao.getAllIncidents()
        .map { it.toDomainList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    fun deleteIncident(id: Long) {
        viewModelScope.launch {
            try {
                incidentDao.deleteIncident(id)
                _uiState.value = _uiState.value.copy(deleteMessage = "Incident supprimé")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(deleteMessage = "Erreur: ${e.message}")
            }
        }
    }

    fun deleteAllIncidents() {
        viewModelScope.launch {
            try {
                incidentDao.deleteAllIncidents()
                _uiState.value = _uiState.value.copy(deleteMessage = "Historique effacé")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(deleteMessage = "Erreur: ${e.message}")
            }
        }
    }

    /**
     * Export incident history to CSV file in Documents/GuardianTrack via MediaStore.
     * File contains: Date, Heure, Type, Latitude, Longitude, Synchronisé
     */
    fun exportToCsv() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            try {
                val allIncidents = incidentDao.getAllIncidents().first()
                
                if (allIncidents.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        exportResult = "Aucun incident à exporter"
                    )
                    return@launch
                }

                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRANCE)
                val fileName = "GuardianTrack_${dateFormat.format(Date())}.csv"
                val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.FRANCE)

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOCUMENTS + "/GuardianTrack"
                    )
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Files.getContentUri("external"),
                    contentValues
                ) ?: throw Exception("Impossible de créer le fichier")

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val writer = OutputStreamWriter(outputStream)
                    writer.write("Date,Heure,Type,Latitude,Longitude,Synchronisé\n")
                    allIncidents.forEach { incident ->
                        val date = dateFmt.format(Date(incident.timestamp))
                        val time = timeFmt.format(Date(incident.timestamp))
                        val synced = if (incident.isSynced) "Oui" else "Non"
                        writer.write("$date,$time,${incident.type}," +
                                "${incident.latitude},${incident.longitude},$synced\n")
                    }
                    writer.flush()
                }

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportResult = "Export complet: $fileName"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportResult = "Erreur d'export: ${e.message}"
                )
            }
        }
    }

    fun exportToText() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            try {
                val allIncidents = incidentDao.getAllIncidents().first()
                if (allIncidents.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        exportResult = "Aucun incident à exporter"
                    )
                    return@launch
                }

                val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRANCE)
                val fileName = "GuardianTrack_${dateFormat.format(Date())}.txt"
                val dateTimeFmt = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRANCE)

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOCUMENTS + "/GuardianTrack"
                    )
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Files.getContentUri("external"),
                    contentValues
                ) ?: throw Exception("Impossible de créer le fichier")

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val writer = OutputStreamWriter(outputStream)
                    writer.write("--- RAPPORT GUARDIAN TRACK ---\n\n")
                    allIncidents.forEach { incident ->
                        val dateTemp = dateTimeFmt.format(Date(incident.timestamp))
                        val synced = if (incident.isSynced) "Oui" else "Non"
                        writer.write("Date: $dateTemp\n")
                        writer.write("Type: ${incident.type}\n")
                        writer.write("Position: ${incident.latitude}, ${incident.longitude}\n")
                        writer.write("Synchronisé: $synced\n")
                        writer.write("------------------------------\n")
                    }
                    writer.flush()
                }

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportResult = "Export complet: $fileName"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportResult = "Erreur d'export: ${e.message}"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(exportResult = null, deleteMessage = null)
    }
}
