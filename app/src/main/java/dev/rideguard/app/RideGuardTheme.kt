package dev.rideguard.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Navy, electric blue and aqua inspired by the RideGuard shield icon. */
private val RideGuardColors = darkColorScheme(
    primary = Color(0xFF5FE7E8),
    onPrimary = Color(0xFF00363E),
    primaryContainer = Color(0xFF0D5366),
    onPrimaryContainer = Color(0xFFC9F6FF),
    secondary = Color(0xFF74B8FC),
    onSecondary = Color(0xFF092B53),
    secondaryContainer = Color(0xFF1E466E),
    onSecondaryContainer = Color(0xFFD9EAFF),
    tertiary = Color(0xFF42D9C7),
    onTertiary = Color(0xFF003830),
    tertiaryContainer = Color(0xFF0B5D55),
    onTertiaryContainer = Color(0xFFB8F6EA),
    background = Color(0xFF091A30),
    onBackground = Color(0xFFEAF4FB),
    surface = Color(0xFF102640),
    onSurface = Color(0xFFEAF4FB),
    surfaceVariant = Color(0xFF1B3954),
    onSurfaceVariant = Color(0xFFBBD0DF),
    surfaceContainerLowest = Color(0xFF091A30),
    surfaceContainerLow = Color(0xFF122D49),
    surfaceContainer = Color(0xFF173451),
    surfaceContainerHigh = Color(0xFF1D3C5B),
    surfaceContainerHighest = Color(0xFF244563),
    outline = Color(0xFF7494AC),
    outlineVariant = Color(0xFF3A5871),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun RideGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = RideGuardColors, content = content)
}
