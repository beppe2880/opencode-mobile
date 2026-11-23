package io.github.beppe2880.opencodemobile.feature.settings.usage

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.format.TextStyle
import java.util.Locale

internal actual fun formatShortDayName(date: LocalDate): String =
    date.toJavaLocalDate().dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
