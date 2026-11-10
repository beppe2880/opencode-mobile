package io.github.beppe2880.opencodemobile.feature.sessions

import io.github.beppe2880.opencodemobile.core.domain.Agent
import io.github.beppe2880.opencodemobile.core.domain.AgentMode
import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.CatalogRepository
import io.github.beppe2880.opencodemobile.core.domain.ChatRepository
import io.github.beppe2880.opencodemobile.core.domain.ConnectionState
import io.github.beppe2880.opencodemobile.core.domain.CredentialKind
import io.github.beppe2880.opencodemobile.core.domain.EventRepository
import io.github.beppe2880.opencodemobile.core.domain.FolderListing
import io.github.beppe2880.opencodemobile.core.domain.FolderPreferences
import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.ModelInfo
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.NewChatModel
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.PendingShare
import io.github.beppe2880.opencodemobile.core.domain.PermissionDecision
import io.github.beppe2880.opencodemobile.core.domain.PermissionRequest
import io.github.beppe2880.opencodemobile.core.domain.Project
import io.github.beppe2880.opencodemobile.core.domain.PromptAttachment
import io.github.beppe2880.opencodemobile.core.domain.PromptDelivery
import io.github.beppe2880.opencodemobile.core.domain.RECENT_FOLDERS
import io.github.beppe2880.opencodemobile.core.domain.RecentModelRepository
import io.github.beppe2880.opencodemobile.core.domain.RunningCommand
import io.github.beppe2880.opencodemobile.core.domain.ServerEvent
import io.github.beppe2880.opencodemobile.core.domain.ServerInfo
import io.github.beppe2880.opencodemobile.core.domain.ServerProfile
import io.github.beppe2880.opencodemobile.core.domain.ServerRepository
import io.github.beppe2880.opencodemobile.core.domain.Session
import io.github.beppe2880.opencodemobile.core.domain.SessionOutcome
import io.github.beppe2880.opencodemobile.core.domain.SessionRepository
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.ShareOpens
import io.github.beppe2880.opencodemobile.core.domain.ShareRepository
import io.github.beppe2880.opencodemobile.core.domain.SharedContent
import io.github.beppe2880.opencodemobile.core.domain.TokenUsage
import io.github.beppe2880.opencodemobile.core.testing.MainDispatcherRule
import io.github.beppe2880.opencodemobile.core.ui.text.toUiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class SessionsViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val zone = TimeZone.UTC
    private val now = LocalDateTime.of(2026, 10, 13, 15, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    private val hour = 3_600_000L
    private val sessions = FakeSessionRepository(
        listOf(
            session("today", now - hour),
            session("yesterday", now - 20 * hour),
            session("week", now - 3 * 24 * hour),
            session("old", now - 30 * 24 * hour),
        ),
    )
    private val events = FakeEventRepository()
    private val chat = FakeChat()
    private val servers = FakeServers()
    private val settings = FakeSettings()
    private val recents = FakeRecents()
    private var catalog = FakeCatalog()
    private val shares = FakeShares()
    private val folders = FakeFolderPreferences()

    private fun vm() = SessionsViewModel(sessions, catalog, events, servers, chat, settings, recents, shares, folders, clock = { now }, zone = zone)

    private fun SessionsState.ids() = groups.flatMap { g -> g.sessions.map { it.id } }

    @Test
    fun `switching server shows its stored lists at once, then refreshes them`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(SessionsEvent.FilterChanged("/p/today"))
        advanceUntilIdle()
        sessions.storedLists["s2" to null] = listOf(session("stored", now - hour))
        sessions.storedProjects["s2"] = listOf(Project("q", "/q", null, null, 0))
        val gate = CompletableDeferred<Unit>()
        sessions.gate = gate

        servers.activate("s2")
        runCurrent()

        val shown = vm.state.value
        assertEquals("Laptop", shown.serverName)
        assertEquals(listOf("stored"), shown.ids())
        assertEquals(listOf("/q"), shown.projects.map { it.directory })
        assertEquals(null, shown.directoryFilter)
        assertEquals(emptySet<String>(), shown.running)
        assertFalse(shown.loading)

        sessions.listResult = Result.success(listOf(session("fresh", now)))
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf("fresh"), vm.state.value.ids())
        assertEquals(listOf("/p/today"), vm.state.value.projects.map { it.directory })

        // Going back restores the filter chosen on the first server.
        servers.activate("s1")
        advanceUntilIdle()
        assertEquals("/p/today", vm.state.value.directoryFilter)
        assertEquals("/p/today", sessions.lastDirectory)
    }

    @Test
    fun `an unreachable server keeps showing its stored list`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        sessions.storedLists["s2" to null] = listOf(session("stored", now - hour))
        sessions.listResult = Result.failure(OpenCodeError.Unreachable("down"))

        servers.activate("s2")
        advanceUntilIdle()

        assertEquals(listOf("stored"), vm.state.value.ids())
        assertEquals(null, vm.state.value.error)
        assertFalse(vm.state.value.loading)
        assertEquals(null, withTimeoutOrNull(1_000) { vm.effect.first() })
    }

    @Test
    fun `removing the server never shows its sessions under the next one`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        val gate = CompletableDeferred<Unit>()
        sessions.gate = gate

        servers.remove("s1")
        runCurrent()

        assertEquals("Laptop", vm.state.value.serverName)
        assertTrue(vm.state.value.groups.isEmpty())
        assertTrue(vm.state.value.loading)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.state.value.loading)
        assertEquals(4, vm.state.value.ids().size)
    }

    @Test
    fun `the most recent sessions get their last messages prefetched`() = runTest(main.testDispatcher) {
        sessions.listResult = Result.success((1..7).map { session("s$it", now - it * hour) }.shuffled())
        vm()
        advanceUntilIdle()

        assertEquals(listOf(listOf("s1", "s2", "s3", "s4", "s5") to 2), chat.prefetched)
    }

    @Test
    fun `groups sessions by day bucket`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.loading)
        assertEquals(listOf(DayBucket.Today, DayBucket.Yesterday, DayBucket.ThisWeek, DayBucket.Older), state.groups.map { it.bucket })
        assertEquals("Desk", state.serverName)
        assertEquals(setOf("today"), state.running)
    }

    @Test
    fun `execution events toggle the running indicator`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        events.flow.emit(ServerEvent.ExecutionStarted("old"))
        runCurrent()
        assertTrue("old" in vm.state.value.running)

        events.flow.emit(ServerEvent.ExecutionEnded("old", SessionOutcome.Succeeded))
        runCurrent()
        assertFalse("old" in vm.state.value.running)
    }

    @Test
    fun `search is debounced and sent to the repository`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(SessionsEvent.QueryChanged("a"))
        vm.onEvent(SessionsEvent.QueryChanged("ab"))
        advanceTimeBy(100)
        assertEquals(null, sessions.lastSearch)
        advanceUntilIdle()
        assertEquals("ab", sessions.lastSearch)
    }

    @Test
    fun `failed delete restores the session and reports an error`() = runTest(main.testDispatcher) {
        sessions.deleteResult = Result.failure(OpenCodeError.Unreachable("down"))
        val vm = vm()
        advanceUntilIdle()
        val target = vm.state.value.groups.first().sessions.first()

        vm.onEvent(SessionsEvent.RequestDelete(target))
        vm.onEvent(SessionsEvent.ConfirmDelete)
        assertTrue(vm.state.value.groups.none { g -> g.sessions.any { it.id == target.id } })
        advanceUntilIdle()

        assertTrue(vm.state.value.groups.any { g -> g.sessions.any { it.id == target.id } })
        assertTrue(vm.effect.first() is SessionsEffect.ShowMessage)
    }

    @Test
    fun `initial failure shows the error state`() = runTest(main.testDispatcher) {
        sessions.listResult = Result.failure(OpenCodeError.Unreachable("down"))
        val vm = vm()
        advanceUntilIdle()

        assertNotNull(vm.state.value.error)
        assertTrue(vm.state.value.groups.isEmpty())
    }

    @Test
    fun `a share opens the new session sheet and goes to the created chat`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        shares.offer(shared)
        advanceUntilIdle()
        val sheet = vm.state.value.newSession!!
        assertEquals(shared, sheet.shared)
        assertEquals("/p/today", sheet.directory)
        assertEquals("build", sheet.agent)
        assertEquals(null, vm.state.value.sharing)

        vm.onEvent(SessionsEvent.CreateSession)
        advanceUntilIdle()

        assertEquals("/p/today", sessions.created.single())
        assertEquals("new" to "s1", shares.pending.value!!.sessionId to shares.pending.value!!.serverId)
        assertEquals(SessionsEffect.OpenChat("new"), vm.effect.first())
        // Handed over here: no second chat opens for it.
        assertEquals(null, withTimeoutOrNull(1_000) { vm.effect.first() })
    }

    @Test
    fun `closing the sheet opened by a share discards it`() = runTest(main.testDispatcher) {
        shares.offer(shared)
        val vm = vm()
        advanceUntilIdle()
        assertEquals(shared, vm.state.value.newSession?.shared)

        vm.onEvent(SessionsEvent.DismissNewSession)
        advanceUntilIdle()

        assertEquals(null, shares.pending.value)
        assertEquals(null, vm.state.value.newSession)
        assertTrue(vm.effect.first() is SessionsEffect.ShowMessage)
    }

    @Test
    fun `a new session started from the button carries no share`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(SessionsEvent.StartNewSession)
        advanceUntilIdle()
        assertEquals(null, vm.state.value.newSession!!.shared)
    }

    @Test
    fun `choosing a chat for a share puts it in the tapped session or a new one`() = runTest(main.testDispatcher) {
        settings.settings.value = AppSettings(shareOpens = ShareOpens.ChooseChat)
        val vm = vm()
        advanceUntilIdle()
        shares.offer(shared)
        advanceUntilIdle()
        assertEquals(shared, vm.state.value.sharing)
        assertEquals(null, vm.state.value.newSession)

        // A new session sheet from the choice takes the share; closing it goes back to the choice.
        vm.onEvent(SessionsEvent.StartNewSession)
        advanceUntilIdle()
        assertEquals(shared, vm.state.value.newSession!!.shared)
        vm.onEvent(SessionsEvent.DismissNewSession)
        advanceUntilIdle()
        assertEquals(shared, vm.state.value.sharing)
        assertNotNull(shares.pending.value)

        vm.onEvent(SessionsEvent.Open("week"))
        advanceUntilIdle()
        assertEquals(SessionsEffect.OpenChat("week"), vm.effect.first())
        assertEquals("week", shares.pending.value!!.sessionId)
        assertEquals(null, vm.state.value.sharing)

        // Without a share, tapping a session only opens it.
        shares.discard()
        advanceUntilIdle()
        vm.onEvent(SessionsEvent.Open("old"))
        assertEquals(SessionsEffect.OpenChat("old"), vm.effect.first())
    }

    @Test
    fun `discarding the share closes the choice`() = runTest(main.testDispatcher) {
        settings.settings.value = AppSettings(shareOpens = ShareOpens.ChooseChat)
        shares.offer(shared)
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(SessionsEvent.DiscardShare)
        advanceUntilIdle()
        assertEquals(null, vm.state.value.sharing)
        assertEquals(null, shares.pending.value)
    }

    @Test
    fun `a share handed to a chat before a restart opens that chat`() = runTest(main.testDispatcher) {
        shares.offer(shared)
        shares.assign("week", "s1")
        val vm = vm()
        advanceUntilIdle()
        assertEquals(SessionsEffect.OpenChat("week"), vm.effect.first())
        assertEquals(null, vm.state.value.newSession)
    }

    @Test
    fun `a share waiting for a chat opens again on another server`() = runTest(main.testDispatcher) {
        shares.offer(shared)
        shares.assign("week", "s1")
        val vm = vm()
        advanceUntilIdle()
        vm.effect.first()

        servers.activate("s2")
        advanceUntilIdle()
        assertEquals(shared, vm.state.value.newSession!!.shared)
    }

    @Test
    fun `creating a session opens its chat`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(SessionsEvent.StartNewSession)
        advanceUntilIdle()
        val sheet = vm.state.value.newSession!!
        assertEquals("/p/today", sheet.directory)
        assertEquals("build", sheet.agent)
        assertEquals(listOf("build"), sheet.agents.map { it.id })

        vm.onEvent(SessionsEvent.NewSessionDirectory("/tmp/x", custom = true))
        vm.onEvent(SessionsEvent.CreateSession)
        advanceUntilIdle()

        assertEquals("/tmp/x", sessions.created.single())
        assertEquals(null, vm.state.value.newSession)
        assertEquals(SessionsEffect.OpenChat("new"), vm.effect.first())
    }

    @Test
    fun `a new session sheet opened before the list loads gets the directory of the latest session`() = runTest(main.testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        sessions.gate = gate
        val vm = vm()
        runCurrent()
        vm.onEvent(SessionsEvent.StartNewSession)
        runCurrent()
        assertEquals("", vm.state.value.newSession!!.directory)

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals("/p/today", vm.state.value.newSession!!.directory)
    }

    @Test
    fun `a directory typed before the list loads is kept`() = runTest(main.testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        sessions.gate = gate
        val vm = vm()
        runCurrent()
        vm.onEvent(SessionsEvent.StartNewSession)
        vm.onEvent(SessionsEvent.NewSessionDirectory("/tmp/x", custom = true))

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals("/tmp/x", vm.state.value.newSession!!.directory)
    }

    @Test
    fun `hiding and unhiding projects persists while keeping the complete project list`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        val projects = vm.state.value.projects

        vm.onEvent(SessionsEvent.HideProject("p"))

        assertEquals(setOf("p"), folders.hiddenProjects("s1"))
        assertEquals(setOf("p"), vm.state.value.hiddenProjects)
        assertEquals(projects, vm.state.value.projects)
        assertTrue(vm.state.value.visibleProjects.isEmpty())
        val restored = vm()
        advanceUntilIdle()
        assertEquals(setOf("p"), restored.state.value.hiddenProjects)

        vm.onEvent(SessionsEvent.UnhideProject("p"))

        assertEquals(emptySet<String>(), folders.hiddenProjects("s1"))
        assertEquals(emptySet<String>(), vm.state.value.hiddenProjects)
        assertEquals(projects, vm.state.value.visibleProjects)
    }

    @Test
    fun `hiding the filtered project clears the filter and reloads the current search`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(SessionsEvent.QueryChanged("work"))
        vm.onEvent(SessionsEvent.FilterChanged("/p/today"))
        advanceUntilIdle()
        assertEquals("/p/today", sessions.lastDirectory)

        vm.onEvent(SessionsEvent.HideProject("p"))
        advanceUntilIdle()

        assertEquals(null, vm.state.value.directoryFilter)
        assertEquals(null, sessions.lastDirectory)
        assertEquals("work", sessions.lastSearch)
        assertEquals("work", vm.state.value.query)
    }

    @Test
    fun `hiding the selected project clears the new session selection through reloads`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(SessionsEvent.FilterChanged("/p/today"))
        advanceUntilIdle()
        assertEquals("/p/today", newSession(vm).directory)

        vm.onEvent(SessionsEvent.HideProject("p"))
        advanceUntilIdle()

        assertEquals("", vm.state.value.newSession!!.directory)
        assertFalse(vm.state.value.newSession!!.customDirectory)
        vm.onEvent(SessionsEvent.Refresh)
        advanceUntilIdle()
        assertEquals("", vm.state.value.newSession!!.directory)
    }

    @Test
    fun `projects skip the global project and list each directory once, most recent first`() = runTest(main.testDispatcher) {
        sessions.serverProjects = listOf(
            Project("global", "/", null, null, 9),
            Project("old", "/same", null, null, 1),
            Project("new", "/same", null, null, 5),
            Project("p", "/p/today", null, null, 3),
        )
        val vm = vm()
        advanceUntilIdle()

        assertEquals(listOf("new", "p"), vm.state.value.projects.map { it.id })
    }

    @Test
    fun `opening a new session reloads projects`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        sessions.serverProjects = sessions.serverProjects + Project("bin", "/home/u/bin", null, null, 10)

        newSession(vm)

        assertEquals("bin", vm.state.value.projects.first().id)
    }

    @Test
    fun `a new session never defaults to the directory of a hidden project`() = runTest(main.testDispatcher) {
        folders.setProjectHidden("s1", "p", true)
        val vm = vm()
        advanceUntilIdle()

        vm.onEvent(SessionsEvent.StartNewSession)
        advanceUntilIdle()

        // Every session and project belongs to the hidden project "p": nothing is preselected.
        assertEquals("", vm.state.value.newSession!!.directory)
    }

    @Test
    fun `hiding a project keeps custom directories and other selected directories`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        newSession(vm)
        vm.onEvent(SessionsEvent.NewSessionDirectory("/p/today", custom = true))

        vm.onEvent(SessionsEvent.HideProject("p"))
        assertEquals("/p/today", vm.state.value.newSession!!.directory)
        assertTrue(vm.state.value.newSession!!.customDirectory)

        vm.onEvent(SessionsEvent.UnhideProject("p"))
        vm.onEvent(SessionsEvent.NewSessionDirectory("/other", custom = false))
        vm.onEvent(SessionsEvent.HideProject("p"))
        assertEquals("/other", vm.state.value.newSession!!.directory)
    }

    @Test
    fun `switching servers reloads hidden projects and keeps their preferences separate`() = runTest(main.testDispatcher) {
        folders.setProjectHidden("s1", "p", true)
        folders.setProjectHidden("s2", "q", true)
        val vm = vm()
        advanceUntilIdle()
        assertEquals(setOf("p"), vm.state.value.hiddenProjects)

        servers.activate("s2")
        advanceUntilIdle()
        assertEquals(setOf("q"), vm.state.value.hiddenProjects)
        assertEquals(listOf("p"), vm.state.value.visibleProjects.map { it.id })
        vm.onEvent(SessionsEvent.HideProject("p"))
        assertEquals(setOf("p", "q"), folders.hiddenProjects("s2"))
        assertEquals(setOf("p"), folders.hiddenProjects("s1"))
        vm.onEvent(SessionsEvent.UnhideProject("p"))
        assertEquals(setOf("q"), folders.hiddenProjects("s2"))

        servers.activate("s1")
        advanceUntilIdle()
        assertEquals(setOf("p"), vm.state.value.hiddenProjects)
    }

    @Test
    fun `the folder browser keeps current server recents while navigating`() = runTest(main.testDispatcher) {
        folders.folderPicked("s1", "/first")
        folders.folderPicked("s1", "/second")
        folders.folderPicked("s2", "/other")
        val vm = vm()
        advanceUntilIdle()
        newSession(vm)

        vm.onEvent(SessionsEvent.BrowseFolders)
        assertEquals(listOf("/second", "/first"), vm.state.value.newSession!!.browser!!.recent)
        advanceUntilIdle()
        vm.onEvent(SessionsEvent.BrowseTo("/another"))
        advanceUntilIdle()
        assertEquals(listOf("/second", "/first"), vm.state.value.newSession!!.browser!!.recent)
        assertEquals(listOf("/second", "/first"), folders.recentFolders("s1"))

        servers.activate("s2")
        advanceUntilIdle()
        newSession(vm)
        vm.onEvent(SessionsEvent.BrowseFolders)
        advanceUntilIdle()
        assertEquals(listOf("/other"), vm.state.value.newSession!!.browser!!.recent)
    }

    @Test
    fun `picking a browsed folder moves it to the current server recents`() = runTest(main.testDispatcher) {
        folders.folderPicked("s1", serverFolder.path)
        folders.folderPicked("s1", "/older")
        folders.folderPicked("s2", "/other")
        val vm = vm()
        advanceUntilIdle()
        newSession(vm)
        vm.onEvent(SessionsEvent.BrowseFolders)
        advanceUntilIdle()

        vm.onEvent(SessionsEvent.PickBrowsedFolder)

        assertEquals(null, vm.state.value.newSession!!.browser)
        assertEquals(listOf(serverFolder.path, "/older"), folders.recentFolders("s1"))
        assertEquals(listOf("/other"), folders.recentFolders("s2"))
        vm.onEvent(SessionsEvent.BrowseFolders)
        assertEquals(listOf(serverFolder.path, "/older"), vm.state.value.newSession!!.browser!!.recent)
        advanceUntilIdle()
    }

    @Test
    fun `without an active server folder preferences are unchanged and browser recents are empty`() = runTest(main.testDispatcher) {
        folders.setProjectHidden("s1", "p", true)
        folders.folderPicked("s1", "/work")
        servers.activate("missing")
        val vm = vm()
        advanceUntilIdle()
        newSession(vm)
        vm.onEvent(SessionsEvent.BrowseFolders)
        advanceUntilIdle()
        val new = vm.state.value.newSession
        assertEquals(emptyList<String>(), new!!.browser!!.recent)

        vm.onEvent(SessionsEvent.HideProject("q"))
        vm.onEvent(SessionsEvent.UnhideProject("p"))
        vm.onEvent(SessionsEvent.PickBrowsedFolder)

        assertEquals(emptySet<String>(), vm.state.value.hiddenProjects)
        val picked = vm.state.value.newSession!!
        assertEquals(new!!.browser!!.path, picked.directory)
        assertTrue(picked.customDirectory)
        assertEquals(null, picked.browser)
        assertEquals(setOf("p"), folders.hiddenProjects("s1"))
        assertEquals(listOf("/work"), folders.recentFolders("s1"))
    }

    @Test
    fun `the folder browser opens at home regardless of a typed absolute directory`() = runTest(main.testDispatcher) {
        val home = FolderListing("/home", "/", listOf("dev"))
        catalog.folderLoad = { Result.success(home) }
        val vm = vm()
        advanceUntilIdle()
        newSession(vm)
        vm.onEvent(SessionsEvent.NewSessionDirectory("  /home/dev  ", custom = true))

        vm.onEvent(SessionsEvent.BrowseFolders)
        assertEquals(FolderBrowserState(path = "/home"), vm.state.value.newSession!!.browser)
        advanceUntilIdle()

        assertEquals(listOf("/home"), catalog.folderRequests)
        assertEquals(FolderBrowserState("/home", "/", listOf("dev"), loading = false), vm.state.value.newSession!!.browser)
    }

    @Test
    fun `a relative directory starts browsing at home and falls back when home is unavailable`() = runTest(main.testDispatcher) {
        catalog.folderLoad = { path ->
            if (path != null) Result.failure(OpenCodeError.NotFound(path)) else Result.success(serverFolder)
        }
        val vm = vm()
        advanceUntilIdle()
        newSession(vm)
        vm.onEvent(SessionsEvent.NewSessionDirectory("projects/app", custom = true))

        vm.onEvent(SessionsEvent.BrowseFolders)
        assertEquals(FolderBrowserState(path = "/home"), vm.state.value.newSession!!.browser)
        advanceUntilIdle()

        assertEquals(listOf("/home", null), catalog.folderRequests)
        assertEquals("/home/dev", vm.state.value.newSession!!.browser!!.path)
    }

    @Test
    fun `an unavailable starting folder falls back to the server folder`() = runTest(main.testDispatcher) {
        catalog.folderLoad = { path ->
            if (path != null) Result.failure(OpenCodeError.NotFound(path)) else Result.success(serverFolder)
        }
        val vm = vm()
        advanceUntilIdle()
        newSession(vm)
        vm.onEvent(SessionsEvent.NewSessionDirectory("/missing", custom = true))

        vm.onEvent(SessionsEvent.BrowseFolders)
        advanceUntilIdle()

        assertEquals(listOf("/home", null), catalog.folderRequests)
        val browser = vm.state.value.newSession!!.browser!!
        assertEquals("/home/dev", browser.path)
        assertFalse(browser.loading)
        assertEquals(null, browser.error)
    }

    @Test
    fun `a failed fallback is shown after only one retry`() = runTest(main.testDispatcher) {
        val error = OpenCodeError.Unreachable("offline")
        catalog.folderLoad = { Result.failure(error) }
        val vm = vm()
        advanceUntilIdle()
        newSession(vm)

        vm.onEvent(SessionsEvent.BrowseFolders)
        advanceUntilIdle()

        assertEquals(listOf("/home", null), catalog.folderRequests)
        assertEquals(FolderBrowserState(path = "/home", loading = false, error = error.toUiText()), vm.state.value.newSession!!.browser)

        vm.onEvent(SessionsEvent.NewSessionDirectory("", custom = true))
        vm.onEvent(SessionsEvent.BrowseFolders)
        advanceUntilIdle()
        assertEquals(listOf("/home", null, "/home", null), catalog.folderRequests)
    }

    @Test
    fun `browsing to a folder keeps the current listing while loading then replaces it`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        newSession(vm)
        vm.onEvent(SessionsEvent.BrowseFolders)
        advanceUntilIdle()
        val before = vm.state.value.newSession!!.browser!!
        val listing = FolderListing("/home/dev/projects", "/home/dev", listOf("app", "website"))
        catalog.folderLoad = { Result.success(listing) }

        vm.onEvent(SessionsEvent.BrowseTo(listing.path))
        assertEquals(before.copy(loading = true), vm.state.value.newSession!!.browser)
        advanceUntilIdle()

        assertEquals(listing.path, catalog.folderRequests.last())
        assertEquals(FolderBrowserState(listing.path, listing.parent, listing.folders, loading = false), vm.state.value.newSession!!.browser)
    }

    @Test
    fun `a failed navigation keeps the listing and a new load clears its error`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        newSession(vm)
        vm.onEvent(SessionsEvent.BrowseFolders)
        advanceUntilIdle()
        val before = vm.state.value.newSession!!.browser!!
        val error = OpenCodeError.NotFound("/missing")
        catalog.folderLoad = { Result.failure(error) }

        vm.onEvent(SessionsEvent.BrowseTo("/missing"))
        advanceUntilIdle()

        assertEquals(before.copy(error = error.toUiText()), vm.state.value.newSession!!.browser)
        assertEquals(listOf("/home", "/missing"), catalog.folderRequests)

        catalog.folderLoad = { Result.success(serverFolder) }
        vm.onEvent(SessionsEvent.BrowseTo(null))
        assertEquals(before.copy(loading = true), vm.state.value.newSession!!.browser)
        advanceUntilIdle()
        assertEquals(null, catalog.folderRequests.last())
        assertEquals(before, vm.state.value.newSession!!.browser)
    }

    @Test
    fun `picking the browsed folder sets a custom directory and closes the browser`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        newSession(vm)
        vm.onEvent(SessionsEvent.BrowseFolders)
        advanceUntilIdle()
        sessions.listResult = Result.failure(OpenCodeError.Unreachable("offline"))
        vm.onEvent(SessionsEvent.CreateSession)
        advanceUntilIdle()
        assertNotNull(vm.state.value.newSession!!.error)

        vm.onEvent(SessionsEvent.PickBrowsedFolder)

        val new = vm.state.value.newSession!!
        assertEquals("/home/dev", new.directory)
        assertTrue(new.customDirectory)
        assertEquals(null, new.error)
        assertEquals(null, new.browser)
    }

    @Test
    fun `picking without a browser or with a blank path does nothing`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        val new = newSession(vm)
        vm.onEvent(SessionsEvent.PickBrowsedFolder)
        assertEquals(new, vm.state.value.newSession)
        vm.onEvent(SessionsEvent.NewSessionDirectory("", custom = true))
        catalog.folderLoad = { Result.success(FolderListing("", null, emptyList())) }
        vm.onEvent(SessionsEvent.BrowseFolders)
        advanceUntilIdle()
        val loading = vm.state.value.newSession

        vm.onEvent(SessionsEvent.PickBrowsedFolder)

        assertEquals(loading, vm.state.value.newSession)
        vm.onEvent(SessionsEvent.CloseFolderBrowser)
        advanceUntilIdle()
    }

    @Test
    fun `picking a recent folder sets the directory and moves it to the front`() = runTest(main.testDispatcher) {
        folders.folderPicked("s1", "/older")
        folders.folderPicked("s1", "/oldest")
        val vm = vm()
        advanceUntilIdle()
        newSession(vm)
        vm.onEvent(SessionsEvent.BrowseFolders)
        advanceUntilIdle()

        vm.onEvent(SessionsEvent.PickFolder("/older"))

        val new = vm.state.value.newSession!!
        assertEquals("/older", new.directory)
        assertTrue(new.customDirectory)
        assertEquals(null, new.error)
        assertEquals(null, new.browser)
        assertEquals(listOf("/older", "/oldest"), folders.recentFolders("s1"))

        vm.onEvent(SessionsEvent.BrowseFolders)
        advanceUntilIdle()
        assertEquals(listOf("/older", "/oldest"), vm.state.value.newSession!!.browser!!.recent)
    }

    @Test
    fun `picking a folder without a browser or with a blank path does nothing`() = runTest(main.testDispatcher) {
        folders.folderPicked("s1", "/work")
        val vm = vm()
        advanceUntilIdle()
        val new = newSession(vm)

        vm.onEvent(SessionsEvent.PickFolder("/elsewhere"))

        assertEquals(new, vm.state.value.newSession)

        vm.onEvent(SessionsEvent.BrowseFolders)
        advanceUntilIdle()
        val browsing = vm.state.value.newSession

        vm.onEvent(SessionsEvent.PickFolder("   "))

        assertEquals(browsing, vm.state.value.newSession)
        assertEquals(listOf("/work"), folders.recentFolders("s1"))
    }

    @Test
    fun `closing the browser keeps the directory and ignores navigation until reopened`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        val new = newSession(vm)
        vm.onEvent(SessionsEvent.BrowseFolders)
        advanceUntilIdle()

        vm.onEvent(SessionsEvent.CloseFolderBrowser)
        vm.onEvent(SessionsEvent.BrowseTo("/ignored"))
        advanceUntilIdle()

        assertEquals(new, vm.state.value.newSession)
        assertEquals(listOf("/home"), catalog.folderRequests)
    }

    @Test
    fun `new navigation and closing the browser or sheet cancel pending folder loads`() = runTest(main.testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        catalog.folderLoad = {
            gate.await()
            Result.success(serverFolder)
        }
        val vm = vm()
        advanceUntilIdle()
        newSession(vm)
        vm.onEvent(SessionsEvent.BrowseFolders)
        runCurrent()
        vm.onEvent(SessionsEvent.BrowseTo("/next"))
        runCurrent()
        assertEquals(listOf("/home"), catalog.folderCancellations)

        vm.onEvent(SessionsEvent.CloseFolderBrowser)
        runCurrent()
        assertEquals(listOf("/home", "/next"), catalog.folderCancellations)
        assertEquals(null, vm.state.value.newSession!!.browser)

        vm.onEvent(SessionsEvent.BrowseFolders)
        runCurrent()
        vm.onEvent(SessionsEvent.DismissNewSession)
        runCurrent()
        assertEquals(listOf("/home", "/next", "/home"), catalog.folderCancellations)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(null, vm.state.value.newSession)
    }

    private val haiku = ModelRef("haiku", "anthropic")
    private val sonnet = ModelRef("sonnet", "anthropic", "high")
    private val opus = ModelRef("opus", "anthropic")

    private val gpt = ModelRef("gpt", "openai", "low")

    private fun withModels(configured: Result<ModelRef?> = Result.success(null)) {
        catalog = FakeCatalog(
            configured = configured,
            agents = listOf(
                Agent("build", "build", null, AgentMode.Primary, hidden = false, color = null),
                Agent("plan", "plan", null, AgentMode.Primary, hidden = false, color = null, model = opus),
            ),
            models = listOf(haiku, sonnet, opus, gpt).map {
                ModelInfo(it.copy(variant = null), it.id, "anthropic", null, 1, true, emptyList(), 0, true, "active")
            },
        )
    }

    private fun TestScope.newSession(vm: SessionsViewModel): NewSessionState {
        vm.onEvent(SessionsEvent.StartNewSession)
        advanceUntilIdle()
        return vm.state.value.newSession!!
    }

    @Test
    fun `the model configured on the server comes before the last used one`() = runTest(main.testDispatcher) {
        withModels(configured = Result.success(gpt))
        recents.models += haiku
        val vm = vm()
        advanceUntilIdle()
        assertEquals(gpt, newSession(vm).defaultModel)
        vm.onEvent(SessionsEvent.CreateSession)
        advanceUntilIdle()
        assertEquals(gpt, sessions.createdModels.single())
    }

    @Test
    fun `the agent's configured model comes before the one configured on the server`() = runTest(main.testDispatcher) {
        withModels(configured = Result.success(gpt))
        val vm = vm()
        advanceUntilIdle()
        newSession(vm)
        vm.onEvent(SessionsEvent.NewSessionAgent("plan"))
        assertEquals(opus, vm.state.value.newSession!!.effectiveModel)
        vm.onEvent(SessionsEvent.NewSessionAgent("build"))
        assertEquals(gpt, vm.state.value.newSession!!.effectiveModel)
    }

    @Test
    fun `a configured model that is not available, or a config that cannot be read, is skipped`() = runTest(main.testDispatcher) {
        withModels(configured = Result.success(ModelRef("gone", "openai")))
        recents.models += haiku
        val vm = vm()
        advanceUntilIdle()
        assertEquals(haiku, newSession(vm).defaultModel)

        vm.onEvent(SessionsEvent.DismissNewSession)
        withModels(configured = Result.failure(OpenCodeError.Unreachable("down")))
        val failing = vm()
        advanceUntilIdle()
        assertEquals(haiku, newSession(failing).defaultModel)
        failing.onEvent(SessionsEvent.CreateSession)
        advanceUntilIdle()
        assertEquals(haiku, sessions.createdModels.single())
    }

    @Test
    fun `without a configured model or history the server decides`() = runTest(main.testDispatcher) {
        withModels()
        val vm = vm()
        advanceUntilIdle()
        assertEquals(null, newSession(vm).defaultModel)
    }

    @Test
    fun `a new chat starts with the last used model that is still available`() = runTest(main.testDispatcher) {
        withModels()
        recents.models += listOf(ModelRef("removed", "anthropic"), sonnet, haiku)
        val vm = vm()
        advanceUntilIdle()
        assertEquals(sonnet, newSession(vm).defaultModel)

        vm.onEvent(SessionsEvent.CreateSession)
        advanceUntilIdle()
        assertEquals(sonnet, sessions.createdModels.single())
    }

    @Test
    fun `the agent's configured model comes before the last used one, a picked model before both`() = runTest(main.testDispatcher) {
        withModels()
        recents.models += haiku
        val vm = vm()
        advanceUntilIdle()
        newSession(vm)
        vm.onEvent(SessionsEvent.NewSessionAgent("plan"))
        assertEquals(opus, vm.state.value.newSession!!.effectiveModel)
        vm.onEvent(SessionsEvent.NewSessionAgent("build"))
        assertEquals(haiku, vm.state.value.newSession!!.effectiveModel)

        vm.onEvent(SessionsEvent.NewSessionModel(sonnet))
        vm.onEvent(SessionsEvent.NewSessionAgent("plan"))
        vm.onEvent(SessionsEvent.CreateSession)
        advanceUntilIdle()
        assertEquals(sonnet, sessions.createdModels.single())
    }

    @Test
    fun `without history on this device the models of recent sessions are used, else the server decides`() = runTest(main.testDispatcher) {
        withModels()
        sessions.listResult = Result.success(listOf(session("a", now - hour).copy(model = haiku), session("b", now)))
        val vm = vm()
        advanceUntilIdle()
        assertEquals(haiku, newSession(vm).defaultModel)

        vm.onEvent(SessionsEvent.DismissNewSession)
        sessions.listResult = Result.success(listOf(session("b", now)))
        vm.onEvent(SessionsEvent.Refresh)
        advanceUntilIdle()
        assertEquals(null, newSession(vm).defaultModel)
        vm.onEvent(SessionsEvent.CreateSession)
        advanceUntilIdle()
        assertEquals(listOf<ModelRef?>(null), sessions.createdModels)
    }

    @Test
    fun `with the server default setting a new chat leaves the model to the server`() = runTest(main.testDispatcher) {
        withModels()
        settings.settings.value = AppSettings(newChatModel = NewChatModel.ServerDefault)
        recents.models += haiku
        val vm = vm()
        advanceUntilIdle()
        newSession(vm)
        vm.onEvent(SessionsEvent.NewSessionAgent("plan"))
        assertEquals(null, vm.state.value.newSession!!.effectiveModel)
        vm.onEvent(SessionsEvent.CreateSession)
        advanceUntilIdle()
        assertEquals(listOf<ModelRef?>(null), sessions.createdModels)
    }

    @Test
    fun `running work is counted every 15 s while visible without reading transcripts`() = runTest(main.testDispatcher) {
        chat.commands = listOf(RunningCommand("sh", "watch", "/p/today", 1, sessionId = "today"))
        val vm = vm()
        advanceUntilIdle()
        assertEquals(0, chat.shellReads)
        vm.onEvent(SessionsEvent.Visible(true))
        runCurrent()
        val first = chat.shellReads
        assertEquals(1, vm.state.value.work["today"]?.commands?.size)
        advanceTimeBy(14_000)
        runCurrent()
        assertEquals(first, chat.shellReads)
        advanceTimeBy(1_100)
        runCurrent()
        assertEquals(2 * first, chat.shellReads)
        assertEquals(0, chat.messageReads)

        vm.onEvent(SessionsEvent.Visible(false))
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(2 * first, chat.shellReads)
    }

    @Test
    fun `a project that cannot be read keeps its counts and the setting turns counting off`() = runTest(main.testDispatcher) {
        chat.commands = listOf(RunningCommand("sh", "watch", "/p/today", 1, sessionId = "today"))
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(SessionsEvent.Visible(true))
        runCurrent()
        assertEquals(1, vm.state.value.work["today"]?.commands?.size)

        chat.failing = setOf("/p/today")
        advanceTimeBy(15_100)
        runCurrent()
        assertEquals(1, vm.state.value.work["today"]?.commands?.size)

        settings.settings.value = AppSettings(showRunningWorkInList = false)
        runCurrent()
        assertFalse(vm.state.value.showRunningWork)
        assertTrue(vm.state.value.work.isEmpty())
        val reads = chat.shellReads
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(reads, chat.shellReads)
        vm.onEvent(SessionsEvent.Visible(false))
    }

    private fun session(id: String, updated: Long) = Session(
        id = id, parentId = null, projectId = "p", title = "Title $id", directory = "/p/$id",
        agent = "build", model = null, cost = 0.0, tokens = TokenUsage(), outcome = null,
        createdAt = updated, updatedAt = updated, archivedAt = null,
    )
}

private class FakeSessionRepository(initial: List<Session>) : SessionRepository {
    var listResult: Result<List<Session>> = Result.success(initial)
    var deleteResult: Result<Unit> = Result.success(Unit)
    var lastSearch: String? = null
    var lastDirectory: String? = null
    val created = mutableListOf<String>()
    val createdModels = mutableListOf<ModelRef?>()
    val storedLists = mutableMapOf<Pair<String, String?>, List<Session>>()
    val storedProjects = mutableMapOf<String, List<Project>>()

    /** When set, network loads wait for it. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun list(search: String?, directory: String?, limit: Int): Result<List<Session>> {
        gate?.await()
        lastSearch = search
        lastDirectory = directory
        return listResult
    }
    override suspend fun cachedList(serverId: String, directory: String?): List<Session>? = storedLists[serverId to directory]
    override suspend fun cachedProjects(serverId: String): List<Project>? = storedProjects[serverId]
    override suspend fun active(): Result<Set<String>> = Result.success(setOf("today"))
    override suspend fun get(sessionId: String): Result<Session> = Result.failure(OpenCodeError.NotFound(sessionId))
    override suspend fun create(directory: String, agent: String?, model: ModelRef?): Result<Session> {
        created += directory
        createdModels += model
        return listResult.map { it.first().copy(id = "new", directory = directory) }
    }
    override suspend fun rename(sessionId: String, title: String): Result<Unit> = Result.success(Unit)
    override suspend fun delete(sessionId: String): Result<Unit> = deleteResult
    override suspend fun fork(sessionId: String, beforeMessageId: String?): Result<Session> = Result.failure(UnsupportedOperationException())
    var serverProjects = listOf(Project("p", "/p/today", null, null, 0))
    override suspend fun projects(): Result<List<Project>> {
        gate?.await()
        return Result.success(serverProjects)
    }
}

private class FakeChat : ChatRepository {
    val prefetched = mutableListOf<Pair<List<String>, Int>>()

    override suspend fun prefetch(sessionIds: List<String>, lastMessages: Int) {
        prefetched += sessionIds to lastMessages
    }
    var commands: List<RunningCommand> = emptyList()
    var failing: Set<String> = emptySet()
    var shellReads = 0
    var messageReads = 0
    override suspend fun runningShells(directory: String): Result<List<RunningCommand>> {
        shellReads++
        if (directory in failing) return Result.failure(OpenCodeError.Unreachable("offline"))
        return Result.success(commands.filter { it.directory == directory })
    }
    override suspend fun messages(sessionId: String): Result<List<Message>> {
        messageReads++
        return Result.success(emptyList())
    }
    override suspend fun prompt(sessionId: String, text: String, delivery: PromptDelivery, attachments: List<PromptAttachment>) =
        Result.success(Unit)
    override suspend fun interrupt(sessionId: String) = Result.success(Unit)
    override suspend fun switchAgent(sessionId: String, agent: String) = Result.success(Unit)
    override suspend fun switchModel(sessionId: String, model: ModelRef) = Result.success(Unit)
    override suspend fun permissions(sessionId: String): Result<List<PermissionRequest>> = Result.success(emptyList())
    override suspend fun replyPermission(sessionId: String, requestId: String, decision: PermissionDecision, message: String?) =
        Result.success(Unit)
}

private class FakeEventRepository : EventRepository {
    override val connection: StateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Connected)
    val flow = MutableSharedFlow<ServerEvent>()
    override val events = flow
}

private class FakeCatalog(
    private val agents: List<Agent> = listOf(
        Agent("build", "build", null, AgentMode.Primary, hidden = false, color = null),
        Agent("explore", "explore", null, AgentMode.Subagent, hidden = false, color = null),
        Agent("title", "title", null, AgentMode.Primary, hidden = true, color = null),
    ),
    private val models: List<ModelInfo> = emptyList(),
    private val configured: Result<ModelRef?> = Result.success(null),
) : CatalogRepository {
    val folderRequests = mutableListOf<String?>()
    val folderCancellations = mutableListOf<String?>()
    var folderLoad: suspend (String?) -> Result<FolderListing> = { Result.success(serverFolder) }
    override suspend fun listFolders(path: String?): Result<FolderListing> {
        folderRequests += path
        return try {
            folderLoad(path)
        } catch (e: CancellationException) {
            folderCancellations += path
            throw e
        }
    }
    override suspend fun configuredModel(directory: String?): Result<ModelRef?> = configured
    override suspend fun agents(directory: String?): Result<List<Agent>> = Result.success(agents)
    override suspend fun models(directory: String?): Result<List<ModelInfo>> = Result.success(models)
}

private val serverFolder = FolderListing("/home/dev", "/home", listOf("projects"))

private val shared = SharedContent(
    text = "Crash on start\nhttps://github.com/o/r/issues/1",
    attachments = listOf(PromptAttachment("data:image/png;base64,AAAA", "shot.png", "image/png")),
)

private class FakeShares : ShareRepository {
    override val pending = MutableStateFlow<PendingShare?>(null)
    private var ids = 0L
    override suspend fun offer(content: SharedContent) {
        pending.value = PendingShare(++ids, content)
    }
    override suspend fun assign(sessionId: String, serverId: String?) {
        pending.value = pending.value?.copy(sessionId = sessionId, serverId = serverId)
    }
    override suspend fun take(sessionId: String): SharedContent? =
        pending.value?.takeIf { it.sessionId == sessionId }?.content?.also { pending.value = null }
    override suspend fun discard() {
        pending.value = null
    }
}

private class FakeSettings : SettingsRepository {
    override val settings = MutableStateFlow(AppSettings())
    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        settings.value = transform(settings.value)
    }
}

private class FakeRecents : RecentModelRepository {
    val models = mutableListOf<ModelRef>()
    override fun recent(): List<ModelRef> = models.toList()
    override fun used(model: ModelRef) {
        models.removeAll { it.key == model.key }
        models.add(0, model)
    }
}

private class FakeFolderPreferences : FolderPreferences {
    private val hidden = mutableMapOf<String, Set<String>>()
    private val recent = mutableMapOf<String, List<String>>()
    override fun hiddenProjects(serverId: String): Set<String> = hidden[serverId].orEmpty()
    override fun setProjectHidden(serverId: String, projectId: String, hidden: Boolean) {
        val current = hiddenProjects(serverId)
        this.hidden[serverId] = if (hidden) current + projectId else current - projectId
    }
    override fun recentFolders(serverId: String): List<String> = recent[serverId].orEmpty()
    override fun folderPicked(serverId: String, path: String) {
        recent[serverId] = (listOf(path) + recentFolders(serverId)).distinct().take(RECENT_FOLDERS)
    }
}

private class FakeServers : ServerRepository {
    private val profile = ServerProfile("s1", "Desk", "http://x", secret = "x", credential = CredentialKind.Password, addedAt = 0)
    private val other = ServerProfile("s2", "Laptop", "http://y", secret = "y", credential = CredentialKind.Password, addedAt = 1)
    override val servers = MutableStateFlow(listOf(profile, other))
    override val activeServer = MutableStateFlow<ServerProfile?>(profile)
    override suspend fun probe(baseUrl: String, password: String?): Result<ServerInfo> = Result.success(ServerInfo("1", emptyList()))
    override suspend fun pair(baseUrl: String, code: String, name: String?) = Result.success(profile)
    override suspend fun addWithPassword(baseUrl: String, password: String, name: String?) = Result.success(profile)
    override suspend fun activate(serverId: String) {
        activeServer.value = servers.value.firstOrNull { it.id == serverId }
    }
    override suspend fun rename(serverId: String, name: String) = Unit
    override suspend fun remove(serverId: String) {
        servers.value = servers.value.filterNot { it.id == serverId }
        if (activeServer.value?.id == serverId) activeServer.value = servers.value.firstOrNull()
    }
}
