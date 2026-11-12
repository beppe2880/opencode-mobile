package io.github.beppe2880.opencodemobile.feature.sessions

import io.github.beppe2880.opencodemobile.core.domain.Session
import io.github.beppe2880.opencodemobile.core.ui.sample.SampleData
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionGroupsTest {

    /** 2027-01-15 08:00 UTC. */
    private val now = 1_800_000_000_000L
    private val hour = 3_600_000L
    private val zone = TimeZone.UTC

    private fun session(id: String, updatedAt: Long, title: String = "Title $id", directory: String = "/p/$id"): Session =
        SampleData.sessions.first().copy(id = id, title = title, directory = directory, updatedAt = updatedAt)

    private val today = session("today", now - hour)
    private val yesterday = session("yesterday", now - 20 * hour)
    private val week = session("week", now - 3 * 24 * hour)
    private val old = session("old", now - 30 * 24 * hour)
    private val all = listOf(old, today, week, yesterday)

    private fun List<SessionGroup>.ids() = map { group -> group.bucket to group.sessions.map { it.id } }

    @Test
    fun withoutPinsSessionsAreGroupedByDay() {
        assertEquals(
            listOf(
                DayBucket.Today to listOf("today"),
                DayBucket.Yesterday to listOf("yesterday"),
                DayBucket.ThisWeek to listOf("week"),
                DayBucket.Older to listOf("old"),
            ),
            groupSessions(all, emptySet(), now, zone).ids(),
        )
    }

    @Test
    fun pinnedSessionsComeFirstMostRecentFirstAndAreNotRepeatedUnderTheirDay() {
        val groups = groupSessions(all, setOf("old", "today", "week"), now, zone)

        assertEquals(
            listOf(
                DayBucket.Pinned to listOf("today", "week", "old"),
                DayBucket.Yesterday to listOf("yesterday"),
            ),
            groups.ids(),
        )
        val shown = groups.flatMap { it.sessions }.map { it.id }
        assertEquals(shown.distinct(), shown)
    }

    @Test
    fun aPinnedSessionOutsideTheFilteredListIsNotShown() {
        // The list is what the project filter and the search left.
        val groups = groupSessions(listOf(today, yesterday), setOf("old", "yesterday"), now, zone)

        assertEquals(listOf(DayBucket.Pinned to listOf("yesterday"), DayBucket.Today to listOf("today")), groups.ids())
    }

    @Test
    fun aSessionListedTwiceShowsOnce() {
        val groups = groupSessions(listOf(old, today, old), setOf("old"), now, zone)

        assertEquals(listOf(DayBucket.Pinned to listOf("old"), DayBucket.Today to listOf("today")), groups.ids())
    }

    @Test
    fun theProjectFilterAndTheSearchApplyToSessionsReadForTheirPin() {
        val pinned = session("pinned", now, title = "Release Notes", directory = "/work/app")

        assertTrue(pinned.matches(search = null, directory = null))
        assertTrue(pinned.matches(search = "release", directory = "/work/app"))
        assertFalse(pinned.matches(search = "deploy", directory = null))
        assertFalse(pinned.matches(search = null, directory = "/work/other"))
    }

    @Test
    fun rowIndexCountsThePinnedGroup() {
        val groups = groupSessions(all, setOf("old"), now, zone)
        // 0 list header, 1 Pinned, 2 old, 3 Today, 4 today.
        assertEquals(2, sessionRowIndex(groups, "old"))
        assertEquals(4, sessionRowIndex(groups, "today"))
    }
}
