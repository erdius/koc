package com.erdman.kofc6650.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val KofcNavy = Color(0xFF1A2F5E)
val KofcNavyLight = Color(0xFF2A4374)
val KofcGold = Color(0xFFC9A84C)
val KofcGoldMuted = Color(0xFF9A7A2C)
val KofcBackground = Color(0xFFF0F2F5)
val KofcCard = Color(0xFFFFFFFF)

private val KofcLightColors = lightColorScheme(
    primary = KofcNavy,
    onPrimary = KofcGold,
    secondary = KofcGold,
    onSecondary = KofcNavy,
    background = KofcBackground,
    onBackground = Color(0xFF222222),
    surface = KofcCard,
    onSurface = Color(0xFF222222),
    surfaceVariant = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFF666666),
)

private val KofcDarkColors = darkColorScheme(
    primary = KofcNavyLight,
    onPrimary = KofcGold,
    secondary = KofcGold,
    onSecondary = KofcNavy,
    background = Color(0xFF14192B),
    onBackground = Color(0xFFEAEAEA),
    surface = Color(0xFF1E2540),
    onSurface = Color(0xFFEAEAEA),
    surfaceVariant = Color(0xFF2A3150),
    onSurfaceVariant = Color(0xFFB0B6C8),
)

@Composable
fun KofC6650Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) KofcDarkColors else KofcLightColors,
        content = content,
    )
}
