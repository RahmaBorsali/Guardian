package com.guardian.track.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * GuardianTrack Material 3 Theme.
 * Supports both dark (default, cybersecurity aesthetic) and light modes.
 */

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = NightAbyss,
    primaryContainer = ElectricBlue,
    onPrimaryContainer = GhostWhite,
    secondary = NeonViolet,
    onSecondary = PureWhite,
    secondaryContainer = Color(0xFF2A1F5E),
    onSecondaryContainer = GhostWhite,
    tertiary = PlasmaGreen,
    onTertiary = NightAbyss,
    tertiaryContainer = Color(0xFF004D26),
    onTertiaryContainer = PlasmaGreen,
    error = CriticalRed,
    onError = PureWhite,
    errorContainer = Color(0xFF5C0011),
    onErrorContainer = CriticalRed,
    background = NightAbyss,
    onBackground = GhostWhite,
    surface = DarkNavy,
    onSurface = GhostWhite,
    surfaceVariant = MidnightSteel,
    onSurfaceVariant = SilverMist,
    outline = DimGray,
    outlineVariant = SlateArmor,
    inverseSurface = GhostWhite,
    inverseOnSurface = NightAbyss,
    scrim = Color(0x80000000)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3182CE), // Bleu beaucoup plus doux et mat
    onPrimary = PureWhite,
    primaryContainer = Color(0xFFEBF8FF),
    onPrimaryContainer = Color(0xFF2B6CB0),
    secondary = Color(0xFF805AD5), // Violet mat
    onSecondary = PureWhite,
    secondaryContainer = Color(0xFFFAF5FF),
    onSecondaryContainer = Color(0xFF6B46C1),
    tertiary = Color(0xFF38A169), // Vert mat
    onTertiary = PureWhite,
    tertiaryContainer = Color(0xFFF0FFF4),
    onTertiaryContainer = Color(0xFF2F855A),
    error = Color(0xFFE53E3E), // Rouge mat
    onError = PureWhite,
    errorContainer = Color(0xFFFFF5F5),
    onErrorContainer = Color(0xFFC53030),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightSecondaryText,
    outline = Color(0xFFCBD5E0), // Bordures très douces
    outlineVariant = Color(0xFFE2E8F0)
)

@Composable
fun GuardianTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GuardianTypography,
        content = content
    )
}
