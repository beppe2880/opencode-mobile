package io.github.beppe2880.opencodemobile.core.data

import com.russhwolf.settings.SharedPreferencesSettings
import io.github.beppe2880.opencodemobile.core.data.server.StoredServers
import io.github.beppe2880.opencodemobile.core.data.session.PreferencesPinnedSessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PinnedSessionsTest {

    private val prefs = InMemoryPreferences()
    private val store = FakeServerStore(StoredServers(listOf(profile("s1", "http://a"), profile("s2", "http://b")), activeId = "s1"))

    private fun pins(activeId: String = "s1") =
        PreferencesPinnedSessionRepository(SharedPreferencesSettings(prefs), TestGraph(FakeServerStore(store.stored.copy(activeId = activeId))).clients)

    @Test
    fun `pins survive a restart and are kept per server`() {
        val pins = pins()
        assertEquals(emptySet<String>(), pins.pinned())

        pins.setPinned("ses_1", true)
        pins.setPinned("ses_2", true)
        pins.setPinned("ses_1", true)
        assertEquals(setOf("ses_1", "ses_2"), pins().pinned())

        // Another server has its own pins, even for the same session id.
        val other = pins("s2")
        assertEquals(emptySet<String>(), other.pinned())
        other.setPinned("ses_1", true)
        other.setPinned("ses_9", true)
        assertEquals(setOf("ses_1", "ses_9"), pins("s2").pinned())
        assertEquals(setOf("ses_1", "ses_2"), pins().pinned())
    }

    @Test
    fun `unpinning forgets the session and the last pin removes the entry`() {
        val pins = pins()
        pins.setPinned("ses_1", true)
        pins.setPinned("ses_2", true)

        // Also how the pin of a deleted session is cleaned up.
        pins.setPinned("ses_1", false)
        assertEquals(setOf("ses_2"), pins().pinned())
        pins.setPinned("unknown", false)
        assertEquals(setOf("ses_2"), pins().pinned())

        pins.setPinned("ses_2", false)
        assertEquals(emptySet<String>(), pins().pinned())
        assertFalse(prefs.contains("pinned_sessions/s1"))
    }

    @Test
    fun `removing a server clears only its pins`() {
        pins().setPinned("ses_1", true)
        pins("s2").setPinned("ses_2", true)

        PreferencesPinnedSessionRepository.clearServer(SharedPreferencesSettings(prefs), "s1")

        assertEquals(emptySet<String>(), pins().pinned())
        assertEquals(setOf("ses_2"), pins("s2").pinned())
    }

    @Test
    fun `unreadable pins read as none and no server keeps none`() {
        prefs.edit().putString("pinned_sessions/s1", "not json").commit()
        assertEquals(emptySet<String>(), pins().pinned())
        prefs.edit().putString("pinned_sessions/s1", "[null]").commit()
        assertEquals(emptySet<String>(), pins().pinned())

        pins().setPinned("ses_1", true)
        assertEquals(setOf("ses_1"), pins().pinned())

        val nowhere = PreferencesPinnedSessionRepository(SharedPreferencesSettings(prefs), TestGraph(FakeServerStore()).clients)
        nowhere.setPinned("ses_3", true)
        assertEquals(emptySet<String>(), nowhere.pinned())
        assertEquals(setOf("ses_1"), pins().pinned())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `only real changes are announced`() = runTest(UnconfinedTestDispatcher()) {
        val pins = pins()
        var changes = 0
        val job = backgroundScope.launch { pins.changes.collect { changes++ } }

        pins.setPinned("ses_1", true)
        pins.setPinned("ses_1", true)
        pins.setPinned("ses_2", false)
        assertEquals(1, changes)
        pins.setPinned("ses_1", false)
        assertEquals(2, changes)
        job.cancel()
    }
}
