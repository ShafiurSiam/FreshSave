package com.freshsave.app.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

// Small, pure formatting helpers. Kept outside Compose so fakes and ViewModels can use them too.

private val groupedNumber = NumberFormat.getIntegerInstance(Locale.US)

/** 4280 -> "৳4,280" (Bangladeshi Taka). */
fun formatTaka(amount: Int): String = "৳" + groupedNumber.format(amount)

/** 1.0 -> "1 km", 0.6 -> "0.6 km". */
fun formatDistance(km: Double): String =
    if (km % 1.0 == 0.0) "${km.toInt()} km" else "%.1f km".format(Locale.US, km)

/** "8:00 pm" — lower-case am/pm like the mock-ups. */
fun formatClockTime(millis: Long): String =
    SimpleDateFormat("h:mm a", Locale.US).format(Date(millis)).lowercase(Locale.US)

private fun sameDay(a: Long, b: Long): Boolean {
    val ca = Calendar.getInstance().apply { timeInMillis = a }
    val cb = Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

fun isToday(millis: Long, now: Long) = sameDay(millis, now)
fun isTomorrow(millis: Long, now: Long) = sameDay(millis, now + 24 * 60 * 60_000L)

/** "today", "tomorrow" or "Wed 23 Jul". */
fun formatDayWord(millis: Long, now: Long): String = when {
    isToday(millis, now) -> "today"
    isTomorrow(millis, now) -> "tomorrow"
    else -> SimpleDateFormat("EEE d MMM", Locale.US).format(Date(millis))
}

/** "6:30–8:00 pm" — drops the first am/pm when both ends share it. */
fun formatTimeRange(start: Long, end: Long): String {
    val s = formatClockTime(start)
    val e = formatClockTime(end)
    return if (s.takeLast(2) == e.takeLast(2)) "${s.dropLast(3)}–$e" else "$s–$e"
}

/** "Wed 23 Jul" for the business dashboard header. */
fun formatShortDate(millis: Long): String = SimpleDateFormat("EEE d MMM", Locale.US).format(Date(millis))

/**
 * Remaining time for countdown badges:
 * over an hour -> "1h 20m", over ten minutes -> "42 min", under ten -> "9:58" (ticks every second).
 */
fun formatTimeLeft(remainingMillis: Long): String {
    val totalSeconds = max(remainingMillis, 0) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes >= 10 -> "$minutes min"
        else -> "%d:%02d".format(minutes, seconds)
    }
}

/** "2m", "3h", "1d" for notification timestamps. */
fun formatAgo(millis: Long, now: Long): String {
    val minutes = max(now - millis, 0) / 60_000
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        minutes < 24 * 60 -> "${minutes / 60}h"
        else -> "${minutes / (24 * 60)}d"
    }
}
