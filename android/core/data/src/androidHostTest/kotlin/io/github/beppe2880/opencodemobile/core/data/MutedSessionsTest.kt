package io.github.beppe2880.opencodemobile.core.data

import com.russhwolf.settings.SharedPreferencesSettings
import io.github.beppe2880.opencodemobile.core.data.server.StoredServers
import io.github.beppe2880.opencodemobile.core.data.session.PreferencesMutedSessionRepository
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MutedSessionsTest {
    private val raw = InMemoryPreferences()
    private val prefs = SharedPreferencesSettings(raw)
    private val store = FakeServerStore(StoredServers(listOf(profile("s1", "http://a"), profile("s2", "http://b")), activeId = "s1"))

    @Test
    fun `mute persists across restart and is isolated by saved server`() = runTest {
        val servers = TestGraph(store).servers
        val mutes = PreferencesMutedSessionRepository(prefs, servers, backgroundScope)
        runCurrent()
        mutes.setMuted("same-id", true)
        assertEquals(setOf("same-id"), PreferencesMutedSessionRepository(prefs, servers, backgroundScope).muted.value)
        servers.activate("s2")
        runCurrent()
        assertEquals(emptySet<String>(), mutes.muted.value)
        mutes.setMuted("other", true)
        servers.activate("s1")
        runCurrent()
        assertEquals(setOf("same-id"), mutes.muted.value)
        mutes.setMuted("same-id", false)
        assertFalse(raw.contains("muted_sessions/s1"))
        servers.activate("s2")
        runCurrent()
        assertEquals(setOf("other"), mutes.muted.value)
    }

    @Test
    fun `temporary mute survives restart and only a later idle clears it`() = runTest {
        val servers = TestGraph(store).servers
        val mutes = PreferencesMutedSessionRepository(prefs, servers, backgroundScope)
        runCurrent()
        mutes.setMuted("temporary", true, untilIdleAfter = 12)
        mutes.setMuted("manual", true)

        val restored = PreferencesMutedSessionRepository(prefs, servers, backgroundScope)
        assertEquals(mapOf("temporary" to 12L), restored.temporary.value)
        restored.finishTemporary("temporary", 12)
        restored.finishTemporary("manual", 24)
        assertEquals(setOf("temporary", "manual"), restored.muted.value)
        restored.finishTemporary("temporary", 24)
        assertEquals(setOf("manual"), restored.muted.value)
    }

    @Test
    fun `server snapshot includes saved mutes before the active server observer runs`() = runTest {
        raw.edit().putString("muted_sessions/s1", "[\"manual\"]")
            .putString("muted_sessions_until_idle/s1", "{\"temporary\":12}")
            .putString("muted_sessions/s2", "[\"other\"]").commit()
        val servers = TestGraph(store, StandardTestDispatcher(testScheduler)).servers
        val mutes = PreferencesMutedSessionRepository(prefs, servers, backgroundScope)

        assertEquals(emptySet<String>(), mutes.muted.value)
        assertEquals(setOf("manual", "temporary"), mutes.mutedForServer("s1"))
        assertEquals(setOf("other"), mutes.mutedForServer("s2"))
    }

    @Test
    fun `corrupt data recovers and removing a server clears only its mutes`() = runTest {
        raw.edit().putString("muted_sessions/s1", "[null]").putString("muted_sessions/s2", "[\"kept\"]").commit()
        val mutes = PreferencesMutedSessionRepository(prefs, TestGraph(store).servers, backgroundScope)
        runCurrent()
        assertEquals(emptySet<String>(), mutes.muted.value)
        mutes.setMuted("valid", true)
        assertEquals(setOf("valid"), mutes.muted.value)
        PreferencesMutedSessionRepository.clearServer(prefs, "s1")
        assertFalse(raw.contains("muted_sessions/s1"))
        assertEquals("[\"kept\"]", raw.getString("muted_sessions/s2", null))
    }
}
