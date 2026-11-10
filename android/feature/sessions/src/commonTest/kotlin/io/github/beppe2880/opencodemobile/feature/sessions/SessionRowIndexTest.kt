package io.github.beppe2880.opencodemobile.feature.sessions

import io.github.beppe2880.opencodemobile.core.ui.sample.SampleData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionRowIndexTest {

    @Test
    fun rowsCountTheListHeaderAndEachGroupHeader() {
        val (a, b, c) = SampleData.sessions
        val groups = listOf(SessionGroup(DayBucket.Today, listOf(a, b)), SessionGroup(DayBucket.Older, listOf(c)))
        // 0 list header, 1 Today, 2 a, 3 b, 4 Older, 5 c.
        assertEquals(2, sessionRowIndex(groups, a.id))
        assertEquals(3, sessionRowIndex(groups, b.id))
        assertEquals(5, sessionRowIndex(groups, c.id))
        assertNull(sessionRowIndex(groups, "missing"))
    }
}
