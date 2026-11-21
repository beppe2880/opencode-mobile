package io.github.beppe2880.opencodemobile.feature.sessions

import io.github.beppe2880.opencodemobile.core.domain.Session
import io.github.beppe2880.opencodemobile.core.domain.SessionOutcome
import io.github.beppe2880.opencodemobile.core.ui.sample.SampleData
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionFiltersTest {

    /** 2027-01-15 08:00 UTC. */
    private val now = 1_800_000_000_000L
    private val hour = 3_600_000L
    private val day = 24 * hour
    private val zone = TimeZone.UTC
    private val today = LocalDate(2027, 1, 15)

    private fun at(date: LocalDate, hour: Int = 8): Long =
        LocalDateTime(date.year, date.month, date.day, hour, 0).toInstant(zone).toEpochMilliseconds()

    private fun session(
        id: String,
        updatedAt: Long,
        outcome: SessionOutcome? = null,
        archived: Boolean = false,
        idleAt: Long? = null,
        viewedAt: Long? = null,
    ): Session = SampleData.sessions.first().copy(
        id = id,
        updatedAt = updatedAt,
        createdAt = updatedAt,
        outcome = outcome,
        archivedAt = if (archived) now else null,
        idleAt = idleAt,
        viewedAt = viewedAt,
    )

    private fun matches(date: SessionDateFilter = SessionDateFilter.AnyTime, session: Session): Boolean =
        SessionFilters(date = date).matches(session, today, zone)

    @Test
    fun emptyFiltersMatchEveryActiveSession() {
        val filters = SessionFilters()

        assertTrue(filters.matches(session("active", now), today, zone))
        assertFalse(filters.matches(session("archived", now, archived = true), today, zone))
    }

    @Test
    fun todayMatchesOnlySessionsUpdatedToday() {
        val todaySession = session("today", now - hour)
        val yesterdaySession = session("yesterday", now - 20 * hour)

        assertTrue(matches(SessionDateFilter.Today, todaySession))
        assertFalse(matches(SessionDateFilter.Today, yesterdaySession))
    }

    @Test
    fun yesterdayMatchesOnlySessionsUpdatedYesterday() {
        val todaySession = session("today", now - hour)
        val yesterdaySession = session("yesterday", now - 20 * hour)
        val twoDaysAgo = session("two-days", now - 2 * day)

        assertTrue(matches(SessionDateFilter.Yesterday, yesterdaySession))
        assertFalse(matches(SessionDateFilter.Yesterday, todaySession))
        assertFalse(matches(SessionDateFilter.Yesterday, twoDaysAgo))
    }

    @Test
    fun lastSevenDaysIncludesTheSixthDayAndExcludesTheSeventh() {
        val sixDaysAgo = session("six", at(today.minus(6, kotlinx.datetime.DateTimeUnit.DAY)))
        val sevenDaysAgo = session("seven", at(today.minus(7, kotlinx.datetime.DateTimeUnit.DAY)))

        assertTrue(matches(SessionDateFilter.Last7Days, sixDaysAgo))
        assertFalse(matches(SessionDateFilter.Last7Days, sevenDaysAgo))
    }

    @Test
    fun lastThirtyDaysIncludesTheTwentyNinthDayAndExcludesTheThirtieth() {
        val twentyNine = session("29", at(today.minus(29, kotlinx.datetime.DateTimeUnit.DAY)))
        val thirty = session("30", at(today.minus(30, kotlinx.datetime.DateTimeUnit.DAY)))

        assertTrue(matches(SessionDateFilter.Last30Days, twentyNine))
        assertFalse(matches(SessionDateFilter.Last30Days, thirty))
    }

    @Test
    fun customRangeIncludesBothBoundaryDays() {
        val from = LocalDate(2027, 1, 12)
        val to = LocalDate(2027, 1, 14)
        val filter = SessionDateFilter.Custom(from, to)

        assertTrue(matches(filter, session("from", at(from, hour = 0))))
        assertTrue(matches(filter, session("to", at(to, hour = 23))))
        assertFalse(matches(filter, session("before", at(from.minus(1, kotlinx.datetime.DateTimeUnit.DAY), hour = 23))))
        assertFalse(matches(filter, session("after", at(to.plus(1, kotlinx.datetime.DateTimeUnit.DAY), hour = 0))))
    }

    @Test
    fun emptyOutcomeSetMatchesEveryOutcome() {
        val failed = session("failed", now, outcome = SessionOutcome.Failed)

        assertTrue(SessionFilters().matches(failed, today, zone))
    }

    @Test
    fun dateFiltersStartAtTheLocalMidnightOfTheirFirstDay() {
        assertEquals(at(today, hour = 0), SessionDateFilter.Today.lowerBound(today, zone))
        assertEquals(at(today.minus(1, kotlinx.datetime.DateTimeUnit.DAY), hour = 0), SessionDateFilter.Yesterday.lowerBound(today, zone))
        assertEquals(at(today.minus(6, kotlinx.datetime.DateTimeUnit.DAY), hour = 0), SessionDateFilter.Last7Days.lowerBound(today, zone))
        assertEquals(at(today.minus(29, kotlinx.datetime.DateTimeUnit.DAY), hour = 0), SessionDateFilter.Last30Days.lowerBound(today, zone))
        val from = today.minus(10, kotlinx.datetime.DateTimeUnit.DAY)
        assertEquals(at(from, hour = 0), SessionDateFilter.Custom(from, today).lowerBound(today, zone))
        assertNull(SessionDateFilter.AnyTime.lowerBound(today, zone))
    }

    @Test
    fun outcomeFilterMatchesOnlySelectedOutcomes() {
        val filters = SessionFilters(outcomes = setOf(SessionOutcome.Failed, SessionOutcome.Interrupted))

        assertTrue(filters.matches(session("failed", now, outcome = SessionOutcome.Failed), today, zone))
        assertTrue(filters.matches(session("interrupted", now, outcome = SessionOutcome.Interrupted), today, zone))
        assertFalse(filters.matches(session("succeeded", now, outcome = SessionOutcome.Succeeded), today, zone))
        assertFalse(filters.matches(session("running", now), today, zone))
    }

    @Test
    fun archivedStatusMatchesTheSelectedDataset() {
        val active = session("active", now)
        val archived = session("archived", now, archived = true)

        assertTrue(SessionFilters(statuses = setOf(SessionStatus.Active)).matches(active, today, zone))
        assertFalse(SessionFilters(statuses = setOf(SessionStatus.Active)).matches(archived, today, zone))
        assertTrue(SessionFilters(statuses = setOf(SessionStatus.Archived)).matches(archived, today, zone))
        assertFalse(SessionFilters(statuses = setOf(SessionStatus.Archived)).matches(active, today, zone))
        assertTrue(SessionFilters(statuses = SessionStatus.entries.toSet()).matches(active, today, zone))
        assertTrue(SessionFilters(statuses = SessionStatus.entries.toSet()).matches(archived, today, zone))
    }

    @Test
    fun togglingStatusesAddsAndRemovesButKeepsTheLastOne() {
        assertEquals(
            setOf(SessionStatus.Active, SessionStatus.Archived),
            SessionFilters().toggleStatus(SessionStatus.Archived).effectiveStatuses,
        )
        assertEquals(
            setOf(SessionStatus.Archived),
            SessionFilters(statuses = setOf(SessionStatus.Active, SessionStatus.Archived))
                .toggleStatus(SessionStatus.Active)
                .effectiveStatuses,
        )
        assertEquals(
            setOf(SessionStatus.Active),
            SessionFilters(statuses = setOf(SessionStatus.Active))
                .toggleStatus(SessionStatus.Active)
                .effectiveStatuses,
        )
    }

    @Test
    fun filtersCombineWithAnd() {
        val matching = session("matching", now - hour, outcome = SessionOutcome.Failed)
        val wrongOutcome = session("wrong-outcome", now - hour, outcome = SessionOutcome.Succeeded)
        val yesterday = session("yesterday", now - 20 * hour, outcome = SessionOutcome.Failed)
        val archived = session("archived", now - hour, outcome = SessionOutcome.Failed, archived = true)
        val filters = SessionFilters(
            statuses = setOf(SessionStatus.Active),
            date = SessionDateFilter.Today,
            outcomes = setOf(SessionOutcome.Failed),
        )

        assertTrue(filters.matches(matching, today, zone))
        assertFalse(filters.matches(wrongOutcome, today, zone))
        assertFalse(filters.matches(yesterday, today, zone))
        assertFalse(filters.matches(archived, today, zone))
    }

    @Test
    fun activeCountCountsFilterCategories() {
        assertEquals(0, SessionFilters().activeCount)
        assertEquals(1, SessionFilters(statuses = setOf(SessionStatus.Archived)).activeCount)
        assertEquals(1, SessionFilters(statuses = SessionStatus.entries.toSet()).activeCount)
        assertEquals(1, SessionFilters(date = SessionDateFilter.Today).activeCount)
        assertEquals(1, SessionFilters(outcomes = setOf(SessionOutcome.Failed, SessionOutcome.Interrupted)).activeCount)
        assertEquals(1, SessionFilters(readState = SessionRead.Unread).activeCount)
        assertEquals(1, SessionFilters(readState = SessionRead.Read).activeCount)
        assertEquals(
            3,
            SessionFilters(
                statuses = setOf(SessionStatus.Archived),
                date = SessionDateFilter.Last7Days,
                outcomes = setOf(SessionOutcome.Failed),
            ).activeCount,
        )
    }

    @Test
    fun readStateNarrowsTheListToUnreadOrReadSessions() {
        val unread = session("unread", now, idleAt = 10, viewedAt = 5)
        val neverViewed = session("never-viewed", now, idleAt = 10)
        val read = session("read", now, idleAt = 10, viewedAt = 10)
        val running = session("running", now)

        val onlyUnread = SessionFilters(readState = SessionRead.Unread)
        assertTrue(onlyUnread.matches(unread, today, zone))
        assertTrue(onlyUnread.matches(neverViewed, today, zone))
        assertFalse(onlyUnread.matches(read, today, zone))
        assertFalse(onlyUnread.matches(running, today, zone))

        val onlyRead = SessionFilters(readState = SessionRead.Read)
        assertTrue(onlyRead.matches(read, today, zone))
        assertTrue(onlyRead.matches(running, today, zone))
        assertFalse(onlyRead.matches(unread, today, zone))

        assertTrue(SessionFilters().matches(read, today, zone))
        assertTrue(SessionFilters().matches(unread, today, zone))
    }

    @Test
    fun togglingAReadStateSelectsItThenTheOtherThenAll() {
        val unread = SessionFilters().toggleRead(SessionRead.Unread)
        assertEquals(SessionRead.Unread, unread.readState)
        assertEquals(SessionRead.Read, unread.toggleRead(SessionRead.Read).readState)
        assertNull(unread.toggleRead(SessionRead.Unread).readState)
    }

    @Test
    fun readStateCombinesWithTheOtherFilters() {
        val filters = SessionFilters(readState = SessionRead.Unread, outcomes = setOf(SessionOutcome.Failed))

        assertTrue(filters.matches(session("failed", now, outcome = SessionOutcome.Failed, idleAt = 10), today, zone))
        assertFalse(filters.matches(session("succeeded", now, outcome = SessionOutcome.Succeeded, idleAt = 10), today, zone))
        assertFalse(filters.matches(session("archived", now, archived = true, idleAt = 10), today, zone))
        assertFalse(filters.matches(session("read-failed", now, outcome = SessionOutcome.Failed, idleAt = 10, viewedAt = 10), today, zone))
    }
}
