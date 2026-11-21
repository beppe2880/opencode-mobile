package io.github.beppe2880.opencodemobile.core.ui.text

import kotlinx.datetime.LocalDate

/** `Nov 9, 2026` in the device language (medium date style). */
expect fun formatMediumDate(date: LocalDate): String
