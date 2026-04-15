package com.guardian.track.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.guardian.track.ui.components.ArcGauge
import com.guardian.track.ui.components.GlassCard
import com.guardian.track.ui.components.GradientDivider
import com.guardian.track.ui.components.PulseIndicator
import com.guardian.track.ui.components.SectionHeader
import com.guardian.track.ui.components.StatusBadge
import com.guardian.track.ui.theme.*
import com.guardian.track.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState
) {
    val uiState by viewModel.uiState.collectAsState()
    val isServiceEnabled by viewModel.isServiceEnabled.collectAsState()

    LaunchedEffect(uiState.lastAlertMessage) {
        uiState.lastAlertMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearAlertMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Hero Section: Shield + Status ──
        ShieldHeroSection(
            isActive = isServiceEnabled,
            onToggle = { viewModel.toggleService(it) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Status Badges Row ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatusBadge(
                label = if (isServiceEnabled) "Actif" else "Inactif",
                isActive = isServiceEnabled
            )
            StatusBadge(
                label = if (uiState.isGpsEnabled) "GPS" else "GPS Off",
                isActive = uiState.isGpsEnabled
            )
            StatusBadge(
                label = "${uiState.incidentCount} incidents",
                isActive = uiState.incidentCount > 0,
                activeColor = SolarAmber,
                inactiveColor = PlasmaGreen
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        GradientDivider()
        Spacer(modifier = Modifier.height(20.dp))

        // ── Sensor Data Cards ──
        SectionHeader(title = "Capteurs en Temps Réel")
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Battery Gauge
            GlassCard(modifier = Modifier.weight(1f)) {
                ArcGauge(
                    value = uiState.batteryLevel / 100f,
                    label = "Batterie",
                    displayValue = "${uiState.batteryLevel}%",
                    primaryColor = when {
                        uiState.batteryLevel > 50 -> PlasmaGreen
                        uiState.batteryLevel > 20 -> SolarAmber
                        else -> CriticalRed
                    },
                    size = 100.dp
                )
            }

            // Magnitude Gauge
            GlassCard(modifier = Modifier.weight(1f)) {
                ArcGauge(
                    value = (uiState.sensorMagnitude / 30f).coerceIn(0f, 1f),
                    label = "Magnitude",
                    displayValue = String.format("%.1f", uiState.sensorMagnitude),
                    primaryColor = when {
                        uiState.sensorMagnitude < 3f -> CriticalRed
                        uiState.sensorMagnitude > 15f -> SolarAmber
                        else -> CyberCyan
                    },
                    size = 100.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Accelerometer axes
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Memory,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Accéléromètre",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AxisValue("X", uiState.sensorAx, CyberCyan)
                    AxisValue("Y", uiState.sensorAy, ElectricBlue)
                    AxisValue("Z", uiState.sensorAz, NeonViolet)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        GradientDivider()
        Spacer(modifier = Modifier.height(20.dp))

        // ── Manual Alert Button ──
        SectionHeader(title = "Alerte d'Urgence")
        Spacer(modifier = Modifier.height(16.dp))

        ManualAlertButton(
            isLoading = uiState.isAlertSending,
            onClick = { viewModel.sendManualAlert() }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ShieldHeroSection(
    isActive: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shieldPulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer glow rings
            if (isActive) {
                Canvas(modifier = Modifier
                    .size(160.dp)
                    .scale(ringScale)
                ) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                CyberCyan.copy(alpha = glowAlpha * 0.3f),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension / 2
                    )
                    drawCircle(
                        color = CyberCyan.copy(alpha = glowAlpha * 0.5f),
                        radius = size.minDimension / 2.5f,
                        style = Stroke(width = 2f)
                    )
                }
            }

            // Shield icon container
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = if (isActive)
                                listOf(CyberCyan.copy(alpha = 0.15f), ElectricBlue.copy(alpha = 0.1f))
                            else
                                listOf(DimGray.copy(alpha = 0.15f), DimGray.copy(alpha = 0.1f))
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = if (isActive)
                                listOf(CyberCyan, ElectricBlue)
                            else
                                listOf(DimGray, DimGray.copy(alpha = 0.5f))
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = "Shield",
                    modifier = Modifier.size(48.dp),
                    tint = if (isActive) CyberCyan else DimGray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isActive) "PROTECTION ACTIVE" else "PROTECTION INACTIVE",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (isActive) CyberCyan else DimGray,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Service toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PowerSettingsNew,
                contentDescription = null,
                tint = if (isActive) CyberCyan else DimGray,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Surveillance",
                style = MaterialTheme.typography.bodyMedium,
                color = SilverMist
            )
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = isActive,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CyberCyan,
                    checkedTrackColor = CyberCyan.copy(alpha = 0.3f),
                    uncheckedThumbColor = DimGray,
                    uncheckedTrackColor = SlateArmor
                )
            )
        }
    }
}

@Composable
private fun AxisValue(axis: String, value: Float, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = axis,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = String.format("%.2f", value),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "m/s²",
            style = MaterialTheme.typography.labelSmall,
            color = SilverMist
        )
    }
}

@Composable
private fun ManualAlertButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(150),
        label = "alertScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "alertPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alertPulseAlpha"
    )

    Box(
        modifier = Modifier
            .size(140.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CriticalRed.copy(alpha = 0.2f),
                        CriticalRed.copy(alpha = 0.05f),
                        Color.Transparent
                    )
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulse ring
        Canvas(modifier = Modifier.size(140.dp)) {
            drawCircle(
                color = CriticalRed.copy(alpha = pulseAlpha * 0.3f),
                radius = size.minDimension / 2,
                style = Stroke(width = 3f)
            )
        }

        // Inner button
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(CriticalRed, CriticalRed.copy(alpha = 0.7f))
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            CriticalRed.copy(alpha = 0.8f),
                            SolarAmber.copy(alpha = 0.5f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Emergency Alert",
                    modifier = Modifier.size(32.dp),
                    tint = PureWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "SOS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = PureWhite,
                    letterSpacing = 3.sp
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Appuyez pour envoyer une alerte manuelle",
        style = MaterialTheme.typography.bodySmall,
        color = SilverMist,
        textAlign = TextAlign.Center
    )
}
