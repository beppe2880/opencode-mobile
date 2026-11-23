package io.github.beppe2880.opencodemobile.core.ui.text

import kotlinx.datetime.LocalDate
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle

actual fun formatMediumDate(date: LocalDate): String {
    val formatter = NSDateFormatter().apply {
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterNoStyle
    }
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
