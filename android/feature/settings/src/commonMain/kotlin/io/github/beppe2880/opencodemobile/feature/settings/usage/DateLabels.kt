package io.github.beppe2880.opencodemobile.feature.settings.usage

import kotlinx.datetime.LocalDate

/** Abbreviated weekday such as `Mon` in the device language. */
internal expect fun formatShortDayName(date: LocalDate): String
