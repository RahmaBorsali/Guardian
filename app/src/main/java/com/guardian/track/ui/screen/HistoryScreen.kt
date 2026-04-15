package com.guardian.track.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.guardian.track.domain.model.Incident
import com.guardian.track.domain.model.IncidentType
import com.guardian.track.ui.components.GlassCard
import com.guardian.track.ui.components.GradientDivider
import com.guardian.track.ui.components.SectionHeader
import com.guardian.track.ui.theme.*
import com.guardian.track.ui.viewmodel.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState
) {
    val incidents by viewModel.incidents.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.exportResult, uiState.deleteMessage) {
        uiState.exportResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.deleteMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var incidentToDelete by remember { mutableStateOf<Incident?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Historique",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${incidents.size} incident${if (incidents.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SilverMist
                    )
                }
            }

            Row {
                // Export button
                IconButton(
                    onClick = { showExportDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Filled.FileDownload,
                        contentDescription = "Export",
                        tint = PlasmaGreen
                    )
                }

                // Delete all button
                IconButton(
                    onClick = { showDeleteAllDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteSweep,
                        contentDescription = "Tout supprimer",
                        tint = CriticalRed.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        GradientDivider()
        Spacer(modifier = Modifier.height(16.dp))

        if (incidents.isEmpty()) {
            // Empty state
            EmptyHistoryState()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = incidents,
                    key = { it.id }
                ) { incident ->
                    var isRemoved by remember { mutableStateOf(false) }
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                isRemoved = true
                                true
                            } else false
                        }
                    )

                    LaunchedEffect(isRemoved) {
                        if (isRemoved) {
                            viewModel.deleteIncident(incident.id)
                        }
                    }

                    AnimatedVisibility(
                        visible = !isRemoved,
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                SwipeDeleteBackground(dismissState)
                            },
                            content = {
                                IncidentCard(
                                    incident = incident,
                                    onDelete = { incidentToDelete = incident }
                                )
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Dialogs
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Purger l'historique") },
            text = { Text("Êtes-vous sûr de vouloir supprimer tous les incidents ? Cette action est irréversible.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAllDialog = false
                    viewModel.deleteAllIncidents()
                }) {
                    Text("Supprimer", color = CriticalRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("Annuler") }
            }
        )
    }

    if (incidentToDelete != null) {
        AlertDialog(
            onDismissRequest = { incidentToDelete = null },
            title = { Text("Supprimer l'incident") },
            text = { Text("Voulez-vous vraiment supprimer cet incident ?") },
            confirmButton = {
                TextButton(onClick = {
                    incidentToDelete?.let { viewModel.deleteIncident(it.id) }
                    incidentToDelete = null
                }) {
                    Text("Supprimer", color = CriticalRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { incidentToDelete = null }) { Text("Annuler") }
            }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Exporter") },
            text = { Text("Choisissez le format d'exportation pour l'historique :") },
            confirmButton = {
                TextButton(onClick = {
                    showExportDialog = false
                    viewModel.exportToCsv()
                }) {
                    Text("CSV", color = PlasmaGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExportDialog = false
                    viewModel.exportToText()
                }) {
                    Text("Texte (*.txt)", color = CyberCyan)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeDeleteBackground(dismissState: SwipeToDismissBoxState) {
    val color by animateColorAsState(
        targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
            CriticalRed.copy(alpha = 0.3f) else Color.Transparent,
        label = "swipeColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1.2f else 0.8f,
        label = "deleteIconScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = "Supprimer",
            tint = CriticalRed,
            modifier = Modifier.scale(scale)
        )
    }
}

@Composable
private fun IncidentCard(incident: Incident, onDelete: () -> Unit) {
    val (icon, iconColor) = when (incident.type) {
        IncidentType.FALL -> Icons.Filled.Warning to SolarAmber
        IncidentType.BATTERY -> Icons.Filled.BatteryAlert to CriticalRed
        IncidentType.MANUAL -> Icons.Filled.PanTool to ElectricBlue
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f))
                    .border(1.dp, iconColor.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = incident.type.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = incident.formattedDateTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = SilverMist
                )
                if (incident.latitude != 0.0 || incident.longitude != 0.0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = SilverMist,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${String.format("%.4f", incident.latitude)}, ${String.format("%.4f", incident.longitude)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = DimGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Sync status & Delete Action
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (incident.isSynced) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                    contentDescription = if (incident.isSynced) "Synchronisé" else "Non synchronisé",
                    tint = if (incident.isSynced) PlasmaGreen else DimGray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Supprimer",
                        tint = CriticalRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(CyberCyan.copy(alpha = 0.08f))
                    .border(1.dp, CyberCyan.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = CyberCyan.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Aucun Incident",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Votre historique est vide.\nLes incidents détectés apparaîtront ici.",
                style = MaterialTheme.typography.bodyMedium,
                color = SilverMist,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}
