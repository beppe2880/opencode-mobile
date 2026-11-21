package io.github.beppe2880.opencodemobile.feature.sessions

import io.github.beppe2880.opencodemobile.core.domain.Session
import io.github.beppe2880.opencodemobile.core.domain.UnreadSessions
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * The groups of the session list: the [pinned] sessions first, then the unread ones, then the
 * others by the day of their last update ([now] and the days are read in [zone]); most recently
 * updated first inside each group, except that pinned unread sessions come before pinned read ones
 * even when they are older. A pinned session is only in the pinned group, and an unread one only in
 * the unread group, and only when they are in [sessions]: the project filter and the search have
 * already been applied to them. The unread group is left out while the list is filtered to unread
 * sessions, so that list keeps its day grouping. A session in [waiting] stays in its day: it is
 * blocked on the user, which its badge already says. [unread] can turn the unread group off
 * ([UnreadSessions.Dot]) or unread state altogether ([UnreadSessions.Off]: pinned sessions are then
 * ordered by their last update only).
 */
internal fun groupSessions(
    sessions: List<Session>,
    pinned: Set<String>,
    now: Long,
    zone: TimeZone,
    filters: SessionFilters = SessionFilters(),
    waiting: Set<String> = emptySet(),
    unread: UnreadSessions = UnreadSessions.DotAndGroup,
): List<SessionGroup> {
    fun localDate(epochMillis: Long): LocalDate = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone).date
    val today = localDate(now)
    return sessions
        .filter { filters.matches(it, today, zone) }
        .distinctBy { it.id }
        .sortedWith(
            compareByDescending<Session> { unread != UnreadSessions.Off && it.id in pinned && it.isUnread }
                .thenByDescending { it.updatedAt },
        )
        .groupBy { session ->
            val day = localDate(session.updatedAt)
            when {
                session.id in pinned -> DayBucket.Pinned
                unread == UnreadSessions.DotAndGroup &&
                    filters.readState != SessionRead.Unread &&
                    session.isUnread &&
                    session.id !in waiting -> DayBucket.Unread
                day >= today -> DayBucket.Today
                day == today.minus(1, DateTimeUnit.DAY) -> DayBucket.Yesterday
                day > today.minus(7, DateTimeUnit.DAY) -> DayBucket.ThisWeek
                else -> DayBucket.Older
            }
        }
        .entries
        .sortedBy { it.key }
        .map { (bucket, items) -> SessionGroup(bucket, items) }
}

/** Whether [session] is in a list narrowed to [directory] (the project filter) and to titles containing [search]. */
internal fun Session.matches(search: String?, directory: String?): Boolean =
    (directory == null || this.directory == directory) && (search == null || title.contains(search, ignoreCase = true))
