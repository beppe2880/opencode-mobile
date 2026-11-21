package io.github.beppe2880.opencodemobile.core.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionUnreadTest {

    private fun session(idleAt: Long?, viewedAt: Long?) = Session(
        id = "ses_1",
        parentId = null,
        projectId = "prj_1",
        title = "Title",
        directory = "/repo",
        agent = null,
        model = null,
        cost = 0.0,
        tokens = TokenUsage(),
        outcome = SessionOutcome.Succeeded,
        createdAt = 1,
        updatedAt = 2,
        archivedAt = null,
        idleAt = idleAt,
        viewedAt = viewedAt,
    )

    @Test
    fun `a finished session no client viewed is unread`() {
        assertTrue(session(idleAt = 10, viewedAt = null).isUnread)
    }

    @Test
    fun `an idle transition newer than the viewed one is unread`() {
        assertTrue(session(idleAt = 20, viewedAt = 10).isUnread)
    }

    @Test
    fun `an idle transition already viewed is read`() {
        assertFalse(session(idleAt = 10, viewedAt = 10).isUnread)
        assertFalse(session(idleAt = 10, viewedAt = 20).isUnread)
    }

    @Test
    fun `a session that has not finished a turn is read`() {
        assertFalse(session(idleAt = null, viewedAt = null).isUnread)
        assertFalse(session(idleAt = null, viewedAt = 5).isUnread)
    }
}
