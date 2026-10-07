package com.freshsave.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val FreshSaveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp), // cards
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private fun FreshSaveColors.toMaterial() = if (isDark) {
    darkColorScheme(
        primary = green, onPrimary = onGreen, primaryContainer = greenTint, onPrimaryContainer = textPrimary,
        secondary = orange, onSecondary = Color.White, secondaryContainer = orangeTint,
        background = background, onBackground = textPrimary,
        surface = card, onSurface = textPrimary, surfaceVariant = surfaceMuted, onSurfaceVariant = textMuted,
        surfaceContainer = card, surfaceContainerHigh = card, surfaceContainerLow = background,
        outline = outline, outlineVariant = outline, error = danger,
    )
} else {
    lightColorScheme(
        primary = green, onPrimary = onGreen, primaryContainer = greenTint, onPrimaryContainer = greenDark,
        secondary = orange, onSecondary = Color.White, secondaryContainer = orangeTint,
        background = background, onBackground = textPrimary,
        surface = card, onSurface = textPrimary, surfaceVariant = surfaceMuted, onSurfaceVariant = textMuted,
        surfaceContainer = card, surfaceContainerHigh = card, surfaceContainerLow = background,
        outline = outline, outlineVariant = outline, error = danger,
    )
}

@Composable
fun FreshSaveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkFreshSaveColors else LightFreshSaveColors
    CompositionLocalProvider(LocalFreshSaveColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toMaterial(),
            typography = FreshSaveTypography,
            shapes = FreshSaveShapes,
            content = content,
        )
    }
}

/** Shortcut so screens can write `FreshSaveTheme.colors.orange`. */
object FreshSaveTheme {
    val colors: FreshSaveColors
        @Composable @ReadOnlyComposable get() = LocalFreshSaveColors.current
}

/**
 * Sets the status-bar icon colour while this screen is visible.
 * Pass `lightIcons = true` on screens with a green header (splash, confirmation, impact).
 */
@Composable
fun StatusBarIcons(lightIcons: Boolean) {
    val view = LocalView.current
    val dark = FreshSaveTheme.colors.isDark
    if (view.isInEditMode) return
    DisposableEffect(lightIcons, dark) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.isAppearanceLightStatusBars = !(lightIcons || dark)
        onDispose { controller?.isAppearanceLightStatusBars = !dark }
    }
}
