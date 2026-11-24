package io.github.beppe2880.opencodemobile.feature.sessions

import androidx.compose.runtime.Immutable
import io.github.beppe2880.opencodemobile.core.domain.Session
import io.github.beppe2880.opencodemobile.core.domain.SessionOutcome
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
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

/** Whether the latest turn of a session has been viewed. */
enum class SessionRead { Read, Unread }

private val DefaultStatuses = setOf(SessionStatus.Active)

/**
 * The filters chosen in the sessions sheet. The archived status selects the server list; the date,
 * outcome and read filters narrow the loaded page. [activeCount] counts categories, not values.
 */
@Immutable
data class SessionFilters(
    /** Both can be selected: the list then shows active and archived sessions. */
    val statuses: Set<SessionStatus> = DefaultStatuses,
    val date: SessionDateFilter = SessionDateFilter.AnyTime,
    val outcomes: Set<SessionOutcome> = emptySet(),
    /** Null shows read and unread sessions; [SessionRead.Read] or [SessionRead.Unread] narrows it. */
    val readState: SessionRead? = null,
) {
    /** Empty reads as the default active-only selection. */
    val effectiveStatuses: Set<SessionStatus> get() = statuses.ifEmpty { DefaultStatuses }

    val activeCount: Int
        get() = (if (effectiveStatuses != DefaultStatuses) 1 else 0) +
            (if (date != SessionDateFilter.AnyTime) 1 else 0) +
            (if (outcomes.isNotEmpty()) 1 else 0) +
            (if (readState != null) 1 else 0)

    val isActive: Boolean get() = activeCount > 0

    /** Adds or removes [status]; the last selected status stays. */
    fun toggleStatus(status: SessionStatus): SessionFilters {
        val current = effectiveStatuses
        val next = if (status in current) current - status else current + status
        return copy(statuses = next.ifEmpty { DefaultStatuses })
    }

    /** Picks [state], or clears the filter when it is already picked. */
    fun toggleRead(state: SessionRead): SessionFilters = copy(readState = if (readState == state) null else state)

    /**
     * Whether [session] passes every filter; [today] is the device's local date. [manualUnread]
     * holds the sessions marked unread on this device, which the read filter counts as unread.
     */
    fun matches(session: Session, today: LocalDate, zone: TimeZone, manualUnread: Set<String> = emptySet()): Boolean {
        val status = if (session.archivedAt != null) SessionStatus.Archived else SessionStatus.Active
        if (status !in effectiveStatuses) return false
        if (readState != null && ((session.isUnread || session.id in manualUnread) != (readState == SessionRead.Unread))) return false
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

/**
 * The oldest instant [this] can match (epoch millis), used to stop reading older pages once the
 * range is covered; null when the filter has no lower bound.
 */
internal fun SessionDateFilter.lowerBound(today: LocalDate, zone: TimeZone): Long? = when (this) {
    SessionDateFilter.AnyTime -> null
    SessionDateFilter.Today -> today.startOfDay(zone)
    SessionDateFilter.Yesterday -> today.minus(1, DateTimeUnit.DAY).startOfDay(zone)
    SessionDateFilter.Last7Days -> today.minus(6, DateTimeUnit.DAY).startOfDay(zone)
    SessionDateFilter.Last30Days -> today.minus(29, DateTimeUnit.DAY).startOfDay(zone)
    is SessionDateFilter.Custom -> from.startOfDay(zone)
}

private fun LocalDate.startOfDay(zone: TimeZone): Long = atStartOfDayIn(zone).toEpochMilliseconds()
