package io.github.beppe2880.opencodemobile.core.data

import com.russhwolf.settings.SharedPreferencesSettings
import io.github.beppe2880.opencodemobile.core.data.server.StoredServers
import io.github.beppe2880.opencodemobile.core.data.session.PreferencesUnreadSessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class UnreadSessionsTest {

    private val prefs = InMemoryPreferences()
    private val store = FakeServerStore(StoredServers(listOf(profile("s1", "http://a"), profile("s2", "http://b")), activeId = "s1"))

    private fun marks(activeId: String = "s1") =
        PreferencesUnreadSessionRepository(SharedPreferencesSettings(prefs), TestGraph(FakeServerStore(store.stored.copy(activeId = activeId))).clients)

    @Test
    fun `unread marks survive a restart and are kept per server`() {
        val marks = marks()
        assertEquals(emptySet<String>(), marks.unread())

        marks.setUnread("ses_1", true)
        marks.setUnread("ses_2", true)
        marks.setUnread("ses_1", true)
        assertEquals(setOf("ses_1", "ses_2"), marks().unread())

        // Another server has its own marks, even for the same session id.
        val other = marks("s2")
        assertEquals(emptySet<String>(), other.unread())
        other.setUnread("ses_1", true)
        other.setUnread("ses_9", true)
        assertEquals(setOf("ses_1", "ses_9"), marks("s2").unread())
        assertEquals(setOf("ses_1", "ses_2"), marks().unread())
    }

    @Test
    fun `clearing a mark forgets the session and the last mark removes the entry`() {
        val marks = marks()
        marks.setUnread("ses_1", true)
        marks.setUnread("ses_2", true)

        // Also how the mark of a deleted or opened session is cleaned up.
        marks.setUnread("ses_1", false)
        assertEquals(setOf("ses_2"), marks().unread())
        marks.setUnread("unknown", false)
        assertEquals(setOf("ses_2"), marks().unread())

        marks.setUnread("ses_2", false)
        assertEquals(emptySet<String>(), marks().unread())
        assertFalse(prefs.contains("unread_sessions/s1"))
    }

    @Test
    fun `removing a server clears only its unread marks`() {
        marks().setUnread("ses_1", true)
        marks("s2").setUnread("ses_2", true)

        PreferencesUnreadSessionRepository.clearServer(SharedPreferencesSettings(prefs), "s1")

        assertEquals(emptySet<String>(), marks().unread())
        assertEquals(setOf("ses_2"), marks("s2").unread())
    }

    @Test
    fun `unreadable marks read as none and no server keeps none`() {
        prefs.edit().putString("unread_sessions/s1", "not json").commit()
        assertEquals(emptySet<String>(), marks().unread())
        prefs.edit().putString("unread_sessions/s1", "[null]").commit()
        assertEquals(emptySet<String>(), marks().unread())

        marks().setUnread("ses_1", true)
        assertEquals(setOf("ses_1"), marks().unread())

        val nowhere = PreferencesUnreadSessionRepository(SharedPreferencesSettings(prefs), TestGraph(FakeServerStore()).clients)
        nowhere.setUnread("ses_3", true)
        assertEquals(emptySet<String>(), nowhere.unread())
        assertEquals(setOf("ses_1"), marks().unread())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `only real changes are announced`() = runTest(UnconfinedTestDispatcher()) {
        val marks = marks()
        var changes = 0
        val job = backgroundScope.launch { marks.changes.collect { changes++ } }

        marks.setUnread("ses_1", true)
        marks.setUnread("ses_1", true)
        marks.setUnread("ses_2", false)
        assertEquals(1, changes)
        marks.setUnread("ses_1", false)
        assertEquals(2, changes)
        job.cancel()
    }
}
