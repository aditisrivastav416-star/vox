package com.example.vox.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import com.example.vox.R

private val Base = Typography()
@OptIn(ExperimentalTextApi::class)
val VoxDisplayFont = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)
val VoxBodyFont = FontFamily(Font(R.font.dm_sans))

/** Material 3 type scale with heavier display/headline weights for amounts. */
internal val VoxTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = VoxDisplayFont, fontWeight = FontWeight.SemiBold),
    displayMedium = Base.displayMedium.copy(fontFamily = VoxDisplayFont, fontWeight = FontWeight.SemiBold),
    displaySmall = Base.displaySmall.copy(fontFamily = VoxDisplayFont, fontWeight = FontWeight.SemiBold),
    headlineLarge = Base.headlineLarge.copy(fontFamily = VoxDisplayFont, fontWeight = FontWeight.SemiBold),
    headlineMedium = Base.headlineMedium.copy(fontFamily = VoxDisplayFont, fontWeight = FontWeight.SemiBold),
    headlineSmall = Base.headlineSmall.copy(fontFamily = VoxDisplayFont, fontWeight = FontWeight.SemiBold),
    titleLarge = Base.titleLarge.copy(fontFamily = VoxDisplayFont, fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontFamily = VoxDisplayFont, fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.copy(fontFamily = VoxBodyFont),
    bodyMedium = Base.bodyMedium.copy(fontFamily = VoxBodyFont),
    bodySmall = Base.bodySmall.copy(fontFamily = VoxBodyFont),
    labelLarge = Base.labelLarge.copy(fontFamily = VoxBodyFont),
    labelMedium = Base.labelMedium.copy(fontFamily = VoxBodyFont),
    labelSmall = Base.labelSmall.copy(fontFamily = VoxBodyFont),
)
