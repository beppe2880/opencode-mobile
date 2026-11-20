package io.github.beppe2880.opencodemobile.feature.settings.usage

import kotlinx.datetime.LocalDate
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
internal actual fun formatShortDayName(date: LocalDate): String {
    val formatter = NSDateFormatter().apply { setLocalizedDateFormatFromTemplate("EEE") }
    return formatter.stringFromDate(date.toNSDate())
}

/** Local noon of [this] day, so no time zone shift can move it to another day. */
private fun LocalDate.toNSDate(): NSDate {
    val components = NSDateComponents().apply {
        year = this@toNSDate.year.toLong()
        month = this@toNSDate.month.ordinal.toLong() + 1
        day = this@toNSDate.day.toLong()
        hour = 12
    }
    return NSCalendar.currentCalendar.dateFromComponents(components) ?: NSDate()
}
