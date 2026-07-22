package top.etta.aerie.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF146C5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2F1E8),
    onPrimaryContainer = Color(0xFF073C32),
    secondary = Color(0xFF9C4D42),
    secondaryContainer = Color(0xFFFFDAD4),
    onSecondaryContainer = Color(0xFF3E0B06),
    tertiary = Color(0xFF49647A),
    background = Color(0xFFF7F8F7),
    surface = Color(0xFFF7F8F7),
    surfaceVariant = Color(0xFFE3E7E4),
    outline = Color(0xFF727874),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF97D5C5),
    onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF005143),
    secondary = Color(0xFFFFB4A8),
    secondaryContainer = Color(0xFF7D2E25),
    tertiary = Color(0xFFB1C9E0),
    background = Color(0xFF111412),
    surface = Color(0xFF111412),
    surfaceVariant = Color(0xFF404844),
    outline = Color(0xFF8B938F),
)

@Composable
fun AerieTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = androidx.compose.material3.Typography(),
        content = content,
    )
}
