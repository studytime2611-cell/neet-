package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val IgrisColorScheme = darkColorScheme(
    primary = IgrisRed,
    onPrimary = Color.White,
    primaryContainer = IgrisSoftRed,
    onPrimaryContainer = Color(0xFFF2A0A8),
    secondary = IgrisGold,
    onSecondary = Color(0xFF1A1407),
    secondaryContainer = IgrisGoldSurface,
    onSecondaryContainer = IgrisGold,
    tertiary = IgrisBlue,
    onTertiary = Color(0xFF0A1424),
    tertiaryContainer = IgrisBlueSurface,
    onTertiaryContainer = IgrisBlue,
    background = IgrisBg,
    onBackground = IgrisText,
    surface = IgrisPanel,
    onSurface = IgrisText,
    surfaceVariant = IgrisPanelAlt,
    onSurfaceVariant = IgrisMuted,
    outline = IgrisLine,
    outlineVariant = IgrisBorderSubtle,
    error = Color(0xFFEF4444),
    onError = Color.White,
    errorContainer = Color(0xFF4A1019),
    onErrorContainer = Color(0xFFFECACA)
)

private val IgrisShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(15.dp)
)

@Composable
fun IgrisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = IgrisColorScheme,
        typography = IgrisTypography,
        shapes = IgrisShapes,
        content = content
    )
}
