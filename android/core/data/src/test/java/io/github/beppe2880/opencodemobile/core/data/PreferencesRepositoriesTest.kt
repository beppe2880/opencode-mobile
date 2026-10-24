package io.github.beppe2880.opencodemobile.core.data

import io.github.beppe2880.opencodemobile.core.data.draft.PreferencesDraftRepository
import io.github.beppe2880.opencodemobile.core.data.server.StoredServers
import io.github.beppe2880.opencodemobile.core.data.settings.PreferencesSettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PreferencesRepositoriesTest {

    @Test
    fun `settings survive a restart and missing keys keep their defaults`() = runBlocking {
        val prefs = InMemoryPreferences()
        val keys = listOf(listOf("esc", "ctrl"), listOf("text:wq\u001f\u001b:wq\r"))
        val changed = AppSettings(
            theme = ThemeMode.Dark,
            showCost = false,
            groupActivity = false,
            terminalKeys = keys,
            outputPreviewLines = 24,
            liveOutputIntervalMs = 3_000,
            keepLiveStepsOpen = true,
            chatTextScale = 1.15f,
        )
        PreferencesSettingsRepository(prefs).update { changed }

        val reopened = PreferencesSettingsRepository(prefs).settings.value

        assertEquals(changed, reopened)
        assertEquals(AppSettings(), PreferencesSettingsRepository(InMemoryPreferences()).settings.value)
    }

    @Test
    fun `stored values outside the choices fall back to the defaults`() {
        val prefs = InMemoryPreferences()
        prefs.edit()
            .putInt("output_preview_lines", 7)
            .putLong("live_output_interval_ms", 1)
            .putFloat("chat_text_scale", 4f)
            .commit()

        val settings = PreferencesSettingsRepository(prefs).settings.value

        assertEquals(AppSettings(), settings)
    }

    @Test
    fun `drafts are kept per server and session and cleared when blank`() {
        val prefs = InMemoryPreferences()
        val store = FakeServerStore(StoredServers(listOf(profile("s1", "http://a"), profile("s2", "http://b")), activeId = "s1"))
        val drafts = PreferencesDraftRepository(prefs, TestGraph(store).clients)

        drafts.save("ses_1", "half a thought")
        assertEquals("half a thought", drafts.load("ses_1"))
        assertEquals("", drafts.load("ses_2"))

        // Another server never sees the first one's drafts.
        val other = PreferencesDraftRepository(
            prefs,
            TestGraph(FakeServerStore(store.stored.copy(activeId = "s2"))).clients,
        )
        assertEquals("", other.load("ses_1"))

        drafts.save("ses_1", "  ")
        assertFalse(prefs.values.keys.any { it.endsWith("ses_1") })
    }
}
