package com.freshsave.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.freshsave.app.R

// res/font/plus_jakarta_sans.ttf is a *variable* font: one file holds every weight.
// Each Font() below picks a weight from it with FontVariation.
private fun jakarta(weight: FontWeight) = Font(
    resId = R.font.plus_jakarta_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val PlusJakartaSans = FontFamily(
    jakarta(FontWeight.Normal),
    jakarta(FontWeight.Medium),
    jakarta(FontWeight.SemiBold),
    jakarta(FontWeight.Bold),
    jakarta(FontWeight.ExtraBold),
)

private fun style(size: Int, weight: FontWeight, lineHeight: Int, spacing: Double = 0.0) = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = spacing.em,
)

val FreshSaveTypography = Typography(
    displayLarge = style(48, FontWeight.ExtraBold, 54, -0.02),
    displayMedium = style(40, FontWeight.ExtraBold, 46, -0.02),
    displaySmall = style(34, FontWeight.ExtraBold, 40, -0.02),
    headlineLarge = style(30, FontWeight.ExtraBold, 36, -0.01),
    headlineMedium = style(26, FontWeight.Bold, 32, -0.01),
    headlineSmall = style(24, FontWeight.Bold, 30, -0.01),
    titleLarge = style(20, FontWeight.Bold, 26),
    titleMedium = style(16, FontWeight.Bold, 22),
    titleSmall = style(14, FontWeight.SemiBold, 20),
    bodyLarge = style(16, FontWeight.Normal, 24),
    bodyMedium = style(14, FontWeight.Normal, 20),
    bodySmall = style(12, FontWeight.Normal, 16),
    labelLarge = style(15, FontWeight.Bold, 20),
    labelMedium = style(12, FontWeight.SemiBold, 16),
    labelSmall = style(11, FontWeight.SemiBold, 14, 0.02),
)
