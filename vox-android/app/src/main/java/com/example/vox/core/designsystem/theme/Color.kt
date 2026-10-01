package com.example.vox.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val VoxPurple = Color(0xFF795DE8)
val VoxInk = Color(0xFF1D2040)
val VoxMuted = Color(0xFF9295AA)
val VoxBackground = Color(0xFFF7F7FC)
val VoxNight = Color(0xFF111322)

internal val LightColors = lightColorScheme(
    primary = VoxPurple, onPrimary = Color.White,
    primaryContainer = Color(0xFFECE8FF), onPrimaryContainer = VoxInk,
    secondary = Color(0xFF6153BA), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7E7F5), onSecondaryContainer = VoxInk,
    tertiary = Color(0xFF56A4E9), onTertiary = Color.White,
    background = VoxBackground, onBackground = VoxInk,
    surface = VoxBackground, onSurface = VoxInk,
    surfaceVariant = Color.White, onSurfaceVariant = VoxMuted,
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFF0F0F8), surfaceContainerHigh = Color(0xFFEAEAF3),
    surfaceContainerHighest = Color(0xFFE3E3EF),
    outline = Color(0xFFE8E8F2), outlineVariant = Color(0xFFE8E8F2),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFF9E8BFF), onPrimary = Color.White,
    primaryContainer = Color(0xFF39305F), onPrimaryContainer = Color.White,
    secondary = Color(0xFFB5A7FF), onSecondary = VoxNight,
    secondaryContainer = Color(0xFF27293D), onSecondaryContainer = Color.White,
    tertiary = Color(0xFF80BFFF), onTertiary = VoxNight,
    background = VoxNight, onBackground = Color(0xFFF7F7FC),
    surface = VoxNight, onSurface = Color(0xFFF7F7FC),
    surfaceVariant = Color(0xFF1D2035), onSurfaceVariant = Color(0xFF999CB5),
    surfaceContainerLowest = Color(0xFF0D0F1D), surfaceContainerLow = Color(0xFF1C1F34),
    surfaceContainer = Color(0xFF22253C), surfaceContainerHigh = Color(0xFF292C45),
    surfaceContainerHighest = Color(0xFF30334E),
    outline = Color(0xFF353850), outlineVariant = Color(0xFF353850),
)
