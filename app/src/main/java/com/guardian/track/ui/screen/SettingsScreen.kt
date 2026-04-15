package com.guardian.track.ui.screen

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.guardian.track.domain.model.EmergencyContact
import com.guardian.track.ui.components.GlassCard
import com.guardian.track.ui.components.GradientDivider
import com.guardian.track.ui.components.SectionHeader
import com.guardian.track.ui.theme.*
import com.guardian.track.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState,
    onDarkModeChange: (Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val contacts by viewModel.contacts.collectAsState()

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Paramètres",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Detection Settings ──
        SectionHeader(title = "Détection de Chute")
        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Speed,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Seuil de sensibilité",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = String.format("%.1f m/s²", uiState.sensitivityThreshold),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = uiState.sensitivityThreshold,
                    onValueChange = { viewModel.updateSensitivity(it) },
                    valueRange = 5f..30f,
                    steps = 24,
                    colors = SliderDefaults.colors(
                        thumbColor = CyberCyan,
                        activeTrackColor = CyberCyan,
                        inactiveTrackColor = SlateArmor
                    )
                )
                Text(
                    text = "Valeur plus basse = détection plus sensible",
                    style = MaterialTheme.typography.labelSmall,
                    color = DimGray
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        GradientDivider()
        Spacer(modifier = Modifier.height(20.dp))

        // ── Appearance ──
        SectionHeader(title = "Apparence")
        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            SettingToggleRow(
                icon = Icons.Filled.DarkMode,
                title = "Mode Sombre",
                subtitle = "Interface sombre pour réduire la fatigue oculaire",
                isChecked = uiState.isDarkMode,
                onCheckedChange = {
                    viewModel.updateDarkMode(it)
                    onDarkModeChange(it)
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        GradientDivider()
        Spacer(modifier = Modifier.height(20.dp))

        // ── Communication ──
        SectionHeader(title = "Communication d'Urgence")
        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                // Emergency number input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Phone,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Numéro d'urgence",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                var numberText by remember(uiState.emergencyNumber) {
                    mutableStateOf(uiState.emergencyNumber)
                }

                OutlinedTextField(
                    value = numberText,
                    onValueChange = {
                        numberText = it
                        viewModel.updateEmergencyNumber(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Ex: +21612345678", color = DimGray)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = SlateArmor,
                        cursorColor = CyberCyan,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    leadingIcon = {
                        Icon(
                            Icons.Filled.SimCard,
                            contentDescription = null,
                            tint = DimGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🔒 Stocké de manière chiffrée (AES-256)",
                    style = MaterialTheme.typography.labelSmall,
                    color = PlasmaGreen.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // SMS Simulation toggle
                SettingToggleRow(
                    icon = Icons.Filled.Sms,
                    title = "Mode Simulation SMS",
                    subtitle = "Remplace les SMS réels par des notifications (mode test)",
                    isChecked = uiState.isSmsSimulation,
                    onCheckedChange = { viewModel.updateSmsSimulation(it) },
                    accentColor = SolarAmber
                )

                if (!uiState.isSmsSimulation) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CriticalRed.copy(alpha = 0.1f))
                            .border(1.dp, CriticalRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️",
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mode réel activé ! Les SMS seront envoyés au numéro configuré.",
                            style = MaterialTheme.typography.labelSmall,
                            color = CriticalRed
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        GradientDivider()
        Spacer(modifier = Modifier.height(20.dp))

        // ── Emergency Contacts ──
        SectionHeader(title = "Contacts d'Urgence")
        Spacer(modifier = Modifier.height(12.dp))

        // Add contact button
        Button(
            onClick = { viewModel.showAddContactDialog(true) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyberCyan.copy(alpha = 0.15f),
                contentColor = CyberCyan
            )
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ajouter un contact d'urgence")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Contact list
        contacts.forEach { contact ->
            ContactCard(
                contact = contact,
                onDelete = { viewModel.deleteContact(contact.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (contacts.isEmpty()) {
            Text(
                text = "Aucun contact d'urgence configuré",
                style = MaterialTheme.typography.bodySmall,
                color = DimGray,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        GradientDivider()
        Spacer(modifier = Modifier.height(20.dp))

        // ── Security Info ──
        SectionHeader(title = "Sécurité")
        Spacer(modifier = Modifier.height(12.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SecurityInfoRow("ContentProvider", "Signature protégé")
                Spacer(modifier = Modifier.height(8.dp))
                SecurityInfoRow("Numéro d'urgence", "AES-256 GCM chiffré")
                Spacer(modifier = Modifier.height(8.dp))
                SecurityInfoRow("Clé API", "local.properties (non versionné)")
                Spacer(modifier = Modifier.height(8.dp))
                SecurityInfoRow("Permissions", "Demandées dynamiquement")
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }

    // Add Contact Dialog
    if (uiState.showAddContactDialog) {
        AddContactDialog(
            onDismiss = { viewModel.showAddContactDialog(false) },
            onConfirm = { name, phone -> viewModel.addContact(name, phone) }
        )
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color = CyberCyan
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = DimGray
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = accentColor,
                checkedTrackColor = accentColor.copy(alpha = 0.3f),
                uncheckedThumbColor = DimGray,
                uncheckedTrackColor = SlateArmor
            )
        )
    }
}

@Composable
private fun ContactCard(
    contact: EmergencyContact,
    onDelete: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ElectricBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ContactPhone,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = SilverMist
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Supprimer",
                    tint = CriticalRed.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SecurityInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = SilverMist
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = PlasmaGreen
        )
    }
}

@Composable
private fun AddContactDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkNavy,
        titleContentColor = GhostWhite,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nouveau Contact d'Urgence")
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom", color = SilverMist) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = SlateArmor,
                        cursorColor = CyberCyan,
                        focusedTextColor = GhostWhite,
                        unfocusedTextColor = GhostWhite
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Numéro de téléphone", color = SilverMist) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = SlateArmor,
                        cursorColor = CyberCyan,
                        focusedTextColor = GhostWhite,
                        unfocusedTextColor = GhostWhite
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onConfirm(name, phone)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Ajouter", color = NightAbyss, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = SilverMist)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
