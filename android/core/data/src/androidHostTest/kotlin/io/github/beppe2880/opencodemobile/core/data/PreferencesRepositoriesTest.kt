package io.github.beppe2880.opencodemobile.core.data

import com.russhwolf.settings.SharedPreferencesSettings
import io.github.beppe2880.opencodemobile.core.data.catalog.PreferencesFolderPreferences
import io.github.beppe2880.opencodemobile.core.data.catalog.PreferencesRecentModelRepository
import io.github.beppe2880.opencodemobile.core.data.draft.PreferencesDraftRepository
import io.github.beppe2880.opencodemobile.core.data.server.StoredServers
import io.github.beppe2880.opencodemobile.core.data.settings.PreferencesSettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.ChangesScope
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderLine
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderStyle
import io.github.beppe2880.opencodemobile.core.domain.ChatSwipe
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.NewChatModel
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeThemeIds
import io.github.beppe2880.opencodemobile.core.domain.SelectionApply
import io.github.beppe2880.opencodemobile.core.domain.SettingsStyle
import io.github.beppe2880.opencodemobile.core.domain.ShareOpens
import io.github.beppe2880.opencodemobile.core.domain.SttProvider
import io.github.beppe2880.opencodemobile.core.domain.ThemeMode
import io.github.beppe2880.opencodemobile.core.domain.ToolMediaSize
import io.github.beppe2880.opencodemobile.core.domain.UpdateNoticeStyle
import io.github.beppe2880.opencodemobile.core.domain.VoiceEngine
import io.github.beppe2880.opencodemobile.core.domain.VoiceRecordingStyle
import io.github.beppe2880.opencodemobile.core.domain.sttModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PreferencesRepositoriesTest {

    /** The themes this test pretends to ship. */
    private val knownThemes = OpenCodeThemeIds { it in setOf("opencode", "dracula", "tokyonight") }

    @Test
    fun `voice input settings keep system dictation until changed and drop unknown values`() = runBlocking {
        // An install from before cloud voice input has only the switch.
        val old = InMemoryPreferences()
        old.edit().putBoolean("dictation", false).commit()
        val upgraded = PreferencesSettingsRepository(SharedPreferencesSettings(old)).settings.value
        assertEquals(false, upgraded.dictation)
        assertEquals(VoiceEngine.System, upgraded.voiceEngine)
        assertEquals(SttProvider.OpenRouter, upgraded.sttProvider)
        assertEquals("openai/whisper-large-v3-turbo", upgraded.sttModel())

        val prefs = InMemoryPreferences()
        prefs.edit()
            .putString("voice_engine", "Neural")
            .putString("stt_provider", "Deepgram")
            .putString("stt_model_OpenAI", "   ")
            .putString("stt_model_Groq", "whisper-large-v3")
            .putString("voice_recording_style", "Bars")
            .commit()
        val read = PreferencesSettingsRepository(SharedPreferencesSettings(prefs)).settings.value
        assertEquals(VoiceEngine.System, read.voiceEngine)
        assertEquals(SttProvider.OpenRouter, read.sttProvider)
        assertEquals(mapOf(SttProvider.Groq to "whisper-large-v3"), read.sttModels)
        assertEquals("gpt-4o-mini-transcribe", read.sttModel(SttProvider.OpenAI))
        // The waveform recorder without hold to record, unless chosen.
        assertEquals(VoiceRecordingStyle.Waveform, read.voiceRecordingStyle)
        assertFalse(read.voiceHoldToRecord)
        assertEquals(VoiceRecordingStyle.Waveform, upgraded.voiceRecordingStyle)

        // Going back to a provider's default removes its stored model.
        val repository = PreferencesSettingsRepository(SharedPreferencesSettings(prefs))
        repository.update { it.copy(sttModels = emptyMap()) }
        assertFalse(prefs.contains("stt_model_Groq"))
    }

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
            groupThoughts = false,
            showRunningWorkInList = false,
            confirmStopCommand = false,
            runningChips = false,
            questionsCollapsed = true,
            permissionsCollapsed = true,
            terminalKeys = keys,
            outputPreviewLines = 24,
            liveOutputIntervalMs = 3_000,
            keepLiveStepsOpen = true,
            chatTextScale = 1.15f,
            openCodeTheme = "tokyonight",
            terminalOpenKeyboard = false,
            terminalFullscreenByDefault = true,
            splitView = false,
            listPaneFraction = 0.35f,
            listPaneHandle = false,
            selectionApply = SelectionApply.Immediately,
            newChatModel = NewChatModel.ServerDefault,
            updateNotice = UpdateNoticeStyle.Banner,
            showInShareMenu = false,
            shareOpens = ShareOpens.ChooseChat,
            backShowsList = false,
            showEditMarks = false,
            settingsStyle = SettingsStyle.Preview,
            settingsPreviewFraction = 0.25f,
            voiceEngine = VoiceEngine.Cloud,
            sttProvider = SttProvider.Custom,
            sttModels = mapOf(SttProvider.Groq to "whisper-large-v3", SttProvider.Custom to "Systran/faster-whisper-small"),
            sttBaseUrl = "http://192.168.1.10:8000/v1",
            sttLanguageHint = false,
            voiceRecordingStyle = VoiceRecordingStyle.Compact,
            voiceHoldToRecord = true,
            toolMediaSize = ToolMediaSize.Large,
            chatSwipe = ChatSwipe.SessionDrawer,
        )
        PreferencesSettingsRepository(SharedPreferencesSettings(prefs)).update { changed }

        val reopened = PreferencesSettingsRepository(SharedPreferencesSettings(prefs)).settings.value

        assertEquals(changed, reopened)
        assertEquals(false, prefs.values["running_chips"])
        assertEquals(AppSettings(), PreferencesSettingsRepository(SharedPreferencesSettings(InMemoryPreferences())).settings.value)
    }

    @Test
    fun `the OpenCode theme starts as none and is kept once chosen`() = runBlocking {
        val prefs = InMemoryPreferences()
        fun theme() = PreferencesSettingsRepository(SharedPreferencesSettings(prefs), knownThemes).settings.value.openCodeTheme
        assertNull(theme())

        PreferencesSettingsRepository(SharedPreferencesSettings(prefs), knownThemes).update { it.copy(openCodeTheme = "dracula") }
        assertEquals("dracula", prefs.values["opencode_theme"])
        assertEquals("dracula", theme())

        PreferencesSettingsRepository(SharedPreferencesSettings(prefs), knownThemes).update { it.copy(openCodeTheme = null) }
        assertFalse(prefs.contains("opencode_theme"))
        assertNull(theme())
    }

    @Test
    fun `the former colored markdown switch becomes the OpenCode theme`() = runBlocking {
        fun themeWith(edit: (android.content.SharedPreferences.Editor) -> Unit): String? {
            val prefs = InMemoryPreferences()
            prefs.edit().also(edit).commit()
            return PreferencesSettingsRepository(SharedPreferencesSettings(prefs), knownThemes).settings.value.openCodeTheme
        }
        assertEquals("opencode", themeWith { it.putBoolean("colored_markdown", true) })
        assertNull(themeWith { it.putBoolean("colored_markdown", false) })
        // Once chosen, the theme wins over the former switch.
        assertEquals("dracula", themeWith { it.putBoolean("colored_markdown", true).putString("opencode_theme", "dracula") })

        // The first write drops the switch: choosing None then stays None.
        val prefs = InMemoryPreferences()
        prefs.edit().putBoolean("colored_markdown", true).commit()
        PreferencesSettingsRepository(SharedPreferencesSettings(prefs), knownThemes).update { it.copy(openCodeTheme = null) }
        assertFalse(prefs.contains("colored_markdown"))
        assertNull(PreferencesSettingsRepository(SharedPreferencesSettings(prefs), knownThemes).settings.value.openCodeTheme)
    }

    @Test
    fun `images read by the agent start at medium and keep every choice`() = runBlocking {
        val prefs = InMemoryPreferences()
        fun size() = PreferencesSettingsRepository(SharedPreferencesSettings(prefs)).settings.value.toolMediaSize
        assertEquals(ToolMediaSize.Medium, size())

        for (choice in ToolMediaSize.entries) {
            PreferencesSettingsRepository(SharedPreferencesSettings(prefs)).update { it.copy(toolMediaSize = choice) }
            assertEquals(choice.name, prefs.values["tool_media_size"])
            assertEquals(choice, size())
        }

        prefs.edit().putString("tool_media_size", "Huge").commit()
        assertEquals(ToolMediaSize.Medium, size())
    }

    @Test
    fun `a theme no longer shipped reads as none`() {
        val prefs = InMemoryPreferences()
        prefs.edit().putString("opencode_theme", "retired").commit()
        assertNull(PreferencesSettingsRepository(SharedPreferencesSettings(prefs), knownThemes).settings.value.openCodeTheme)
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
            .putString("settings_style", "Cards")
            .putString("share_opens", "Ask")
            .putFloat("settings_preview_fraction", 3f)
            .putString("update_notice", "Popup")
            .putString("tool_media_size", "Huge")
            .commit()

        val settings = PreferencesSettingsRepository(SharedPreferencesSettings(prefs)).settings.value

        assertEquals(AppSettings(), settings)
    }

    @Test
    fun `the former update switch becomes the update notice`() = runBlocking {
        fun noticeWith(edit: (android.content.SharedPreferences.Editor) -> Unit): UpdateNoticeStyle {
            val prefs = InMemoryPreferences()
            prefs.edit().also(edit).commit()
            return PreferencesSettingsRepository(SharedPreferencesSettings(prefs)).settings.value.updateNotice
        }
        assertEquals(UpdateNoticeStyle.Off, noticeWith { it.putBoolean("check_for_updates", false) })
        assertEquals(UpdateNoticeStyle.Window, noticeWith { it.putBoolean("check_for_updates", true) })
        assertEquals(UpdateNoticeStyle.Window, noticeWith { })
        // Once chosen, the choice wins over the former switch.
        assertEquals(UpdateNoticeStyle.Banner, noticeWith { it.putBoolean("check_for_updates", false).putString("update_notice", "Banner") })

        // Older versions of the app still read the switch.
        val prefs = InMemoryPreferences()
        PreferencesSettingsRepository(SharedPreferencesSettings(prefs)).update { it.copy(updateNotice = UpdateNoticeStyle.Off) }
        assertEquals(false, prefs.values["check_for_updates"])
        PreferencesSettingsRepository(SharedPreferencesSettings(prefs)).update { it.copy(updateNotice = UpdateNoticeStyle.Banner) }
        assertEquals(true, prefs.values["check_for_updates"])
    }

    @Test
    fun `the former swipe switch becomes the swipe choice`() = runBlocking {
        fun swipeWith(edit: (android.content.SharedPreferences.Editor) -> Unit): ChatSwipe {
            val prefs = InMemoryPreferences()
            prefs.edit().also(edit).commit()
            return PreferencesSettingsRepository(SharedPreferencesSettings(prefs)).settings.value.chatSwipe
        }
        // The chat list is the default, also for whoever had the switch on; off stays off.
        assertEquals(ChatSwipe.SessionDrawer, swipeWith { it.putBoolean("swipe_between_chats", true) })
        assertEquals(ChatSwipe.Off, swipeWith { it.putBoolean("swipe_between_chats", false) })
        assertEquals(ChatSwipe.SessionDrawer, swipeWith { })
        // Once chosen, the choice wins over the former switch; an unknown one falls back.
        assertEquals(ChatSwipe.SwitchChats, swipeWith { it.putBoolean("swipe_between_chats", false).putString("chat_swipe", "SwitchChats") })
        assertEquals(ChatSwipe.SessionDrawer, swipeWith { it.putString("chat_swipe", "Pager") })

        val prefs = InMemoryPreferences()
        for (choice in ChatSwipe.entries) {
            PreferencesSettingsRepository(SharedPreferencesSettings(prefs)).update { it.copy(chatSwipe = choice) }
            assertEquals(choice.name, prefs.values["chat_swipe"])
            // Older versions of the app still read the switch.
            assertEquals(choice != ChatSwipe.Off, prefs.values["swipe_between_chats"])
            assertEquals(choice, PreferencesSettingsRepository(SharedPreferencesSettings(prefs)).settings.value.chatSwipe)
        }
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
    fun `hidden project directories persist per server and can be unhidden`() {
        val prefs = SharedPreferencesSettings(InMemoryPreferences())
        val folders = PreferencesFolderPreferences(prefs)
        assertEquals(emptySet<String>(), folders.hiddenDirectories("s1"))

        folders.setDirectoryHidden("s1", "/p", true)
        folders.setDirectoryHidden("s1", "/q", true)
        folders.setDirectoryHidden("s1", "/p", true)
        folders.setDirectoryHidden("s2", "/p", true)
        val restored = PreferencesFolderPreferences(prefs)
        assertEquals(setOf("/p", "/q"), restored.hiddenDirectories("s1"))
        restored.setDirectoryHidden("s1", "/p", false)
        assertEquals(setOf("/q"), folders.hiddenDirectories("s1"))
        assertEquals(setOf("/p"), folders.hiddenDirectories("s2"))
    }

    @Test
    fun `picked folders persist per server with deduplication and a limit of five`() {
        val prefs = SharedPreferencesSettings(InMemoryPreferences())
        val folders = PreferencesFolderPreferences(prefs)
        assertEquals(emptyList<String>(), folders.recentFolders("s1"))
        (1..6).forEach { folders.folderPicked("s1", "/folder/$it") }
        folders.folderPicked("s1", "/folder/3")
        folders.folderPicked("s2", "/other")

        val restored = PreferencesFolderPreferences(prefs)
        assertEquals(listOf("/folder/3", "/folder/6", "/folder/5", "/folder/4", "/folder/2"), restored.recentFolders("s1"))
        assertEquals(listOf("/other"), restored.recentFolders("s2"))
    }

    @Test
    fun `malformed folder preferences read as empty and oversized recent lists are bounded`() {
        val prefs = SharedPreferencesSettings(InMemoryPreferences())
        val folders = PreferencesFolderPreferences(prefs)
        prefs.putString("hidden_projects/s1", "not json")
        prefs.putString("recent_folders/s1", "[null]")
        assertEquals(emptySet<String>(), folders.hiddenDirectories("s1"))
        assertEquals(emptyList<String>(), folders.recentFolders("s1"))
        folders.setDirectoryHidden("s1", "/p", true)
        folders.folderPicked("s1", "/work")
        assertEquals(setOf("/p"), folders.hiddenDirectories("s1"))
        assertEquals(listOf("/work"), folders.recentFolders("s1"))

        prefs.putString("recent_folders/s1", """["/a","/b","/a","/c","/d","/e","/f"]""")
        assertEquals(listOf("/a", "/b", "/c", "/d", "/e"), folders.recentFolders("s1"))
    }

    @Test
    fun `removing a server clears only its folder preferences`() {
        val prefs = SharedPreferencesSettings(InMemoryPreferences())
        val folders = PreferencesFolderPreferences(prefs)
        listOf("s1", "s2").forEach {
            folders.setDirectoryHidden(it, "/p", true)
            folders.folderPicked(it, "/work")
        }

        PreferencesFolderPreferences.clearServer(prefs, "s1")

        assertEquals(emptySet<String>(), folders.hiddenDirectories("s1"))
        assertEquals(emptyList<String>(), folders.recentFolders("s1"))
        assertEquals(setOf("/p"), folders.hiddenDirectories("s2"))
        assertEquals(listOf("/work"), folders.recentFolders("s2"))
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
