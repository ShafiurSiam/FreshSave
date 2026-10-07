package com.freshsave.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * One shared "current time" that ticks every second.
 * Countdown timers and expiry checks all read from here, so they stay in sync and
 * re-render on their own without each screen running its own timer loop.
 */
class AppClock(scope: CoroutineScope) {
    /** When the app process started; anything newer than this happened "this session". */
    val sessionStartMillis: Long = System.currentTimeMillis()

    private val _now = MutableStateFlow(sessionStartMillis)
    val now: StateFlow<Long> = _now.asStateFlow()

    init {
        scope.launch {
            while (isActive) {
                // Wake up right on the next whole second so timers flip together.
                delay(1_000 - System.currentTimeMillis() % 1_000)
                _now.value = System.currentTimeMillis()
            }
        }
    }

    fun millis(): Long = System.currentTimeMillis()
}
