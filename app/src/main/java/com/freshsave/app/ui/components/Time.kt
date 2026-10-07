package com.freshsave.app.ui.components

import androidx.compose.runtime.compositionLocalOf

/**
 * The current time, refreshed every second by AppClock and provided at the root of the UI.
 * Any composable that reads `LocalNow.current` redraws on each tick, which is how
 * countdowns stay live without every screen running its own timer.
 */
val LocalNow = compositionLocalOf { System.currentTimeMillis() }
