package com.freshsave.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Raw brand colours sampled from the App.pdf mock-ups.
val Green = Color(0xFF1B7A4A)
val GreenDark = Color(0xFF0F5132)
val GreenTint = Color(0xFFE3EFE7)
val Orange = Color(0xFFF07F2D)
val OrangeTint = Color(0xFFFDEBDD)
val Cream = Color(0xFFF8F5EF)
val CardWhite = Color(0xFFFFFFFF)
val DarkCard = Color(0xFF1A2620)
val TextDark = Color(0xFF1A1F1C)
val TextMuted = Color(0xFF6B6F6C)
val Beige = Color(0xFFF1EBE1)
val BeigeOutline = Color(0xFFE7E0D3)
val Danger = Color(0xFFD64545)
val DangerTint = Color(0xFFFBE4E1)

/**
 * Material 3's colour scheme has no slot for things like "discount orange" or "dark card",
 * so the app keeps its own set of named colours and exposes them through a CompositionLocal.
 * Read them with `FreshSaveTheme.colors.orange` anywhere inside [FreshSaveTheme].
 */
@Immutable
data class FreshSaveColors(
    val green: Color,
    val greenDark: Color,
    val greenTint: Color,
    val onGreen: Color,
    val orange: Color,
    val orangeTint: Color,
    val background: Color,
    val card: Color,
    val darkCard: Color,
    val onDarkCard: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val surfaceMuted: Color,
    val outline: Color,
    val danger: Color,
    val dangerTint: Color,
    val isDark: Boolean,
)

val LightFreshSaveColors = FreshSaveColors(
    green = Green,
    greenDark = GreenDark,
    greenTint = GreenTint,
    onGreen = Color.White,
    orange = Orange,
    orangeTint = OrangeTint,
    background = Cream,
    card = CardWhite,
    darkCard = DarkCard,
    onDarkCard = Color.White,
    textPrimary = TextDark,
    textMuted = TextMuted,
    surfaceMuted = Beige,
    outline = BeigeOutline,
    danger = Danger,
    dangerTint = DangerTint,
    isDark = false,
)

// Dark mode keeps the same brand feel: deep green-black surfaces, slightly brighter accents.
val DarkFreshSaveColors = FreshSaveColors(
    green = Color(0xFF2E9A62),
    greenDark = Color(0xFF14653D),
    greenTint = Color(0xFF1E3428),
    onGreen = Color.White,
    orange = Color(0xFFF58D40),
    orangeTint = Color(0xFF3B2718),
    background = Color(0xFF0F1512),
    card = Color(0xFF19221D),
    darkCard = Color(0xFF233129),
    onDarkCard = Color.White,
    textPrimary = Color(0xFFECEFEC),
    textMuted = Color(0xFFA2AAA5),
    surfaceMuted = Color(0xFF232D27),
    outline = Color(0xFF2E3A33),
    danger = Color(0xFFEF6B6B),
    dangerTint = Color(0xFF3A1F1F),
    isDark = true,
)

val LocalFreshSaveColors = staticCompositionLocalOf { LightFreshSaveColors }
