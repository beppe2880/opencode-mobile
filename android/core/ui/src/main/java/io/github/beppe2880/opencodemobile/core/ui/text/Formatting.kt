package io.github.beppe2880.opencodemobile.core.ui.text

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.beppe2880.opencodemobile.core.ui.R
import java.text.NumberFormat
import java.util.Locale

/** Compact duration such as `850ms`, `3s`, `1m 20s`, `2h 5m`. */
fun formatDuration(millis: Long): String {
    if (millis < 1_000) return "${millis.coerceAtLeast(0)}ms"
    val seconds = millis / 1_000
    if (seconds < 60) return "${seconds}s"
    val minutes = seconds / 60
    if (minutes < 60) return if (seconds % 60 == 0L) "${minutes}m" else "${minutes}m ${seconds % 60}s"
    val hours = minutes / 60
    return if (minutes % 60 == 0L) "${hours}h" else "${hours}h ${minutes % 60}m"
}

/** `$0.0042`, `$1.23`; empty for zero. */
fun formatCost(cost: Double): String = when {
    cost <= 0.0 -> ""
    cost < 0.01 -> String.format(Locale.US, "$%.4f", cost)
    else -> String.format(Locale.US, "$%.2f", cost)
}

/** `81.5 tok/s`, one decimal like the OpenCode TUI. */
fun formatTokensPerSecond(rate: Double): String = String.format(Locale.US, "%.1f tok/s", rate)

/** Exact count with locale grouping, like the TUI: `19,952 tok` (`19.952 tok` in Italian). */
fun formatTokenTotal(count: Long): String = NumberFormat.getIntegerInstance().format(count) + " tok"

/** `1.2k`, `35k`, `1.4M`, `1.8B` tokens. */
fun formatTokens(count: Long): String = when {
    count < 1_000 -> count.toString()
    count < 10_000 -> String.format(Locale.US, "%.1fk", count / 1_000.0)
    count < 1_000_000 -> "${count / 1_000}k"
    count < 1_000_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
    else -> String.format(Locale.US, "%.1fB", count / 1_000_000_000.0)
}

/** Localized relative time, e.g. "5 min ago"; "just now" under a minute. */
@Composable
fun relativeTime(timestamp: Long, now: Long): String {
    if (now - timestamp < DateUtils.MINUTE_IN_MILLIS) return stringResource(R.string.oc_just_now)
    return DateUtils.getRelativeTimeSpanString(
        timestamp,
        now,
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
}
