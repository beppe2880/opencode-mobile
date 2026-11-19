package io.github.beppe2880.opencodemobile.feature.sessions

import androidx.compose.runtime.Immutable
import io.github.beppe2880.opencodemobile.core.domain.Session
import io.github.beppe2880.opencodemobile.core.domain.SessionOutcome
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** The date range of the list, in the device's time zone; the list groups and filters by last update. */
sealed interface SessionDateFilter {
    data object AnyTime : SessionDateFilter
    data object Today : SessionDateFilter
    data object Yesterday : SessionDateFilter
    data object Last7Days : SessionDateFilter
    data object Last30Days : SessionDateFilter
    data class Custom(val from: LocalDate, val to: LocalDate) : SessionDateFilter
}

enum class SessionStatus { Active, Archived }

private val DefaultStatuses = setOf(SessionStatus.Active)

/**
 * The filters chosen in the sessions sheet. The archived status selects the server list; the date
 * and outcome filters narrow the loaded page. [activeCount] counts categories, not values.
 */
@Immutable
data class SessionFilters(
    /** Both can be selected: the list then shows active and archived sessions. */
    val statuses: Set<SessionStatus> = DefaultStatuses,
    val date: SessionDateFilter = SessionDateFilter.AnyTime,
    val outcomes: Set<SessionOutcome> = emptySet(),
) {
    /** Empty reads as the default active-only selection. */
    val effectiveStatuses: Set<SessionStatus> get() = statuses.ifEmpty { DefaultStatuses }

    val activeCount: Int
        get() = (if (effectiveStatuses != DefaultStatuses) 1 else 0) +
            (if (date != SessionDateFilter.AnyTime) 1 else 0) +
            (if (outcomes.isNotEmpty()) 1 else 0)

    val isActive: Boolean get() = activeCount > 0

    /** Adds or removes [status]; the last selected status stays. */
    fun toggleStatus(status: SessionStatus): SessionFilters {
        val current = effectiveStatuses
        val next = if (status in current) current - status else current + status
        return copy(statuses = next.ifEmpty { DefaultStatuses })
    }

    /** Whether [session] passes every filter; [today] is the device's local date. */
    fun matches(session: Session, today: LocalDate, zone: TimeZone): Boolean {
        val status = if (session.archivedAt != null) SessionStatus.Archived else SessionStatus.Active
        if (status !in effectiveStatuses) return false
        if (outcomes.isNotEmpty() && session.outcome !in outcomes) return false
        return date.matches(session.updatedAt, today, zone)
    }
}

private fun SessionDateFilter.matches(updatedAt: Long, today: LocalDate, zone: TimeZone): Boolean {
    if (this == SessionDateFilter.AnyTime) return true
    val updated = Instant.fromEpochMilliseconds(updatedAt).toLocalDateTime(zone).date
    return when (this) {
        SessionDateFilter.AnyTime -> true
        SessionDateFilter.Today -> updated == today
        SessionDateFilter.Yesterday -> updated == today.minus(1, DateTimeUnit.DAY)
        SessionDateFilter.Last7Days -> updated in today.minus(6, DateTimeUnit.DAY)..today
        SessionDateFilter.Last30Days -> updated in today.minus(29, DateTimeUnit.DAY)..today
        is SessionDateFilter.Custom -> from <= to && updated in from..to
    }
}
