package io.github.beppe2880.opencodemobile.feature.sessions

import io.github.beppe2880.opencodemobile.core.domain.Session
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * The groups of the session list: the [pinned] sessions first, then the others by the day of
 * their last update ([now] and the days are read in [zone]); most recently updated first inside
 * each group. A pinned session is only in the pinned group, and only when it is in [sessions]:
 * the project filter and the search have already been applied to them.
 */
internal fun groupSessions(sessions: List<Session>, pinned: Set<String>, now: Long, zone: TimeZone): List<SessionGroup> {
    fun localDate(epochMillis: Long): LocalDate = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone).date
    val today = localDate(now)
    return sessions
        .distinctBy { it.id }
        .sortedByDescending { it.updatedAt }
        .groupBy { session ->
            val day = localDate(session.updatedAt)
            when {
                session.id in pinned -> DayBucket.Pinned
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
