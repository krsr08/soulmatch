package com.soulmatch.app.ui.theme
import android.graphics.Color.parseColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.soulmatch.app.data.models.ThemeConfig

private fun parseColorOr(hex: String?, fallback: Color): Color {
    return try {
        if (hex.isNullOrBlank()) fallback else Color(parseColor(hex))
    } catch (_: IllegalArgumentException) {
        fallback
    }
}

private fun lightColors(theme: ThemeConfig) = lightColorScheme(
    primary = parseColorOr(theme.primary, Primary),
    onPrimary = Color.White,
    secondary = parseColorOr(theme.secondary, Secondary),
    onSecondary = TextPrimary,
    background = parseColorOr(theme.background, Background),
    surface = parseColorOr(theme.surface, Surface),
    primaryContainer = SurfaceWarm,
    onPrimaryContainer = PrimaryDark,
    secondaryContainer = WarningSoft,
    onSecondaryContainer = TextPrimary,
    surfaceVariant = SurfaceSoft,
    onSurfaceVariant = TextSecondary,
    outline = Divider,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = Error
)

private fun darkColors(theme: ThemeConfig) = darkColorScheme(
    primary = parseColorOr(theme.primary, PrimaryLight),
    onPrimary = DarkTextPrimary,
    secondary = parseColorOr(theme.secondary, Secondary),
    background = DarkBackground,
    surface = DarkSurface,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = DarkTextPrimary,
    secondaryContainer = Color(0xFF3B3021),
    onSecondaryContainer = DarkTextPrimary,
    surfaceVariant = Color(0xFF2D2724),
    onSurfaceVariant = DarkTextSecondary,
    outline = Color(0xFF4A3E39),
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    error = Error
)

@Composable
fun SoulMatchTheme(
    themeConfig: ThemeConfig = ThemeConfig(),
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColors(themeConfig) else lightColors(themeConfig),
        typography = Typography,
        content = content
    )
}
