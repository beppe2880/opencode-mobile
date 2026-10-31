package io.github.beppe2880.opencodemobile.core.data

import com.russhwolf.settings.SharedPreferencesSettings
import io.github.beppe2880.opencodemobile.core.data.catalog.PreferencesRecentModelRepository
import io.github.beppe2880.opencodemobile.core.data.draft.PreferencesDraftRepository
import io.github.beppe2880.opencodemobile.core.data.server.StoredServers
import io.github.beppe2880.opencodemobile.core.data.settings.PreferencesSettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.ChangesScope
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderLine
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderStyle
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.NewChatModel
import io.github.beppe2880.opencodemobile.core.domain.SelectionApply
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
            chatHeaderLine = ChatHeaderLine.BranchFiles,
            chatHeaderStyle = ChatHeaderStyle.Full,
            headerOpens = ChangesScope.Session,
            groupActivity = false,
            questionsCollapsed = true,
            permissionsCollapsed = true,
            terminalKeys = keys,
            outputPreviewLines = 24,
            liveOutputIntervalMs = 3_000,
            keepLiveStepsOpen = true,
            chatTextScale = 1.15f,
            terminalOpenKeyboard = false,
            terminalFullscreenByDefault = true,
            splitView = false,
            listPaneFraction = 0.35f,
            listPaneHandle = false,
            selectionApply = SelectionApply.Immediately,
            newChatModel = NewChatModel.ServerDefault,
            checkForUpdates = false,
            backShowsList = false,
        )
        PreferencesSettingsRepository(SharedPreferencesSettings(prefs)).update { changed }

        val reopened = PreferencesSettingsRepository(SharedPreferencesSettings(prefs)).settings.value

        assertEquals(changed, reopened)
        assertEquals(AppSettings(), PreferencesSettingsRepository(SharedPreferencesSettings(InMemoryPreferences())).settings.value)
    }

    @Test
    fun `stored values outside the choices fall back to the defaults`() {
        val prefs = InMemoryPreferences()
        prefs.edit()
            .putInt("output_preview_lines", 7)
            .putLong("live_output_interval_ms", 1)
            .putFloat("chat_text_scale", 4f)
            .putString("selection_apply", "Later")
            .putString("chat_header_line", "Everything")
            .putString("chat_header_style", "Emoji")
            // Only the folder and the session can be opened from the header.
            .putString("chat_header_opens", "LastTurn")
            .commit()

        val settings = PreferencesSettingsRepository(SharedPreferencesSettings(prefs)).settings.value

        assertEquals(AppSettings(), settings)
    }

    @Test
    fun `drafts are kept per server and session and cleared when blank`() {
        val prefs = InMemoryPreferences()
        val store = FakeServerStore(StoredServers(listOf(profile("s1", "http://a"), profile("s2", "http://b")), activeId = "s1"))
        val drafts = PreferencesDraftRepository(SharedPreferencesSettings(prefs), TestGraph(store).clients)

        drafts.save("ses_1", "half a thought")
        assertEquals("half a thought", drafts.load("ses_1"))
        assertEquals("", drafts.load("ses_2"))

        // Another server never sees the first one's drafts.
        val other = PreferencesDraftRepository(
            SharedPreferencesSettings(prefs),
            TestGraph(FakeServerStore(store.stored.copy(activeId = "s2"))).clients,
        )
        assertEquals("", other.load("ses_1"))

        drafts.save("ses_1", "  ")
        assertFalse(prefs.values.keys.any { it.endsWith("ses_1") })
    }

    @Test
    fun `recent models are kept per server, newest first, and cleared with the server`() {
        val prefs = InMemoryPreferences()
        val store = FakeServerStore(StoredServers(listOf(profile("s1", "http://a"), profile("s2", "http://b")), activeId = "s1"))
        val recents = PreferencesRecentModelRepository(SharedPreferencesSettings(prefs), TestGraph(store).clients)
        val haiku = ModelRef("haiku", "anthropic")
        val sonnet = ModelRef("sonnet", "anthropic", "high")

        recents.used(haiku)
        recents.used(sonnet)
        recents.used(haiku.copy(variant = "low"))
        assertEquals(listOf(haiku.copy(variant = "low"), sonnet), recents.recent())

        val other = PreferencesRecentModelRepository(
            SharedPreferencesSettings(prefs),
            TestGraph(FakeServerStore(store.stored.copy(activeId = "s2"))).clients,
        )
        assertEquals(emptyList<ModelRef>(), other.recent())

        PreferencesRecentModelRepository.clearServer(SharedPreferencesSettings(prefs), "s1")
        assertEquals(emptyList<ModelRef>(), recents.recent())
    }

    @Test
    fun `corrupt or malformed recent models read as none`() {
        val prefs = InMemoryPreferences()
        val store = FakeServerStore(StoredServers(listOf(profile("s1", "http://a")), activeId = "s1"))
        val recents = PreferencesRecentModelRepository(SharedPreferencesSettings(prefs), TestGraph(store).clients)

        prefs.edit().putString("recent_models/s1", "not json").commit()
        assertEquals(emptyList<ModelRef>(), recents.recent())

        prefs.edit().putString("recent_models/s1", "[[\"anthropic\"],[null,\"x\",null],[\"anthropic\",\"haiku\",null]]").commit()
        assertEquals(listOf(ModelRef("haiku", "anthropic")), recents.recent())

        recents.used(ModelRef("opus", "anthropic"))
        assertEquals(listOf(ModelRef("opus", "anthropic"), ModelRef("haiku", "anthropic")), recents.recent())
    }
}
