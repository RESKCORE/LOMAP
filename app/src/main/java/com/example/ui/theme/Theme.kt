package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.model.AppThemeMode

private val LomapSignatureColorScheme = lightColorScheme(
    primary = ElectricLime,
    onPrimary = Color.White,
    primaryContainer = ElectricLimeSoft,
    onPrimaryContainer = Color(0xFF2C4A08),
    secondary = EmeraldGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1F2E4),
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = UberAccent,
    onTertiary = Color.White,
    background = LomapBg,
    onBackground = TextPrimary,
    surface = LomapSurface,
    onSurface = TextPrimary,
    surfaceVariant = LomapSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = LomapCardBorder,
    outlineVariant = Color(0xFFE4EDE0),
    error = StatusDanger,
    onError = Color.White
)

val RidePilotShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
@Suppress("UNUSED_PARAMETER")
fun RidePilotTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    // LOMAP enforces a single unified signature light mobility theme across the entire app.
    MaterialTheme(
        colorScheme = LomapSignatureColorScheme,
        typography = Typography,
        shapes = RidePilotShapes,
        content = content
    )
}
