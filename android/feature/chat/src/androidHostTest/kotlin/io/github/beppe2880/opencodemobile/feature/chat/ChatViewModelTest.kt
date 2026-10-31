package io.github.beppe2880.opencodemobile.feature.chat

import io.github.beppe2880.opencodemobile.core.domain.Agent
import io.github.beppe2880.opencodemobile.core.domain.AgentMode
import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.CatalogRepository
import io.github.beppe2880.opencodemobile.core.domain.ChangesScope
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderLine
import io.github.beppe2880.opencodemobile.core.domain.ChatRepository
import io.github.beppe2880.opencodemobile.core.domain.Command
import io.github.beppe2880.opencodemobile.core.domain.ConnectionState
import io.github.beppe2880.opencodemobile.core.domain.DraftRepository
import io.github.beppe2880.opencodemobile.core.domain.EventRepository
import io.github.beppe2880.opencodemobile.core.domain.FileDiff
import io.github.beppe2880.opencodemobile.core.domain.Form
import io.github.beppe2880.opencodemobile.core.domain.FormAnswer
import io.github.beppe2880.opencodemobile.core.domain.FormCondition
import io.github.beppe2880.opencodemobile.core.domain.FormField
import io.github.beppe2880.opencodemobile.core.domain.FormOption
import io.github.beppe2880.opencodemobile.core.domain.ImageAttachment
import io.github.beppe2880.opencodemobile.core.domain.McpRepository
import io.github.beppe2880.opencodemobile.core.domain.McpServer
import io.github.beppe2880.opencodemobile.core.domain.McpStatus
import io.github.beppe2880.opencodemobile.core.domain.Message
import io.github.beppe2880.opencodemobile.core.domain.ModelInfo
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.domain.Part
import io.github.beppe2880.opencodemobile.core.domain.PermissionDecision
import io.github.beppe2880.opencodemobile.core.domain.PermissionRequest
import io.github.beppe2880.opencodemobile.core.domain.Project
import io.github.beppe2880.opencodemobile.core.domain.PromptAttachment
import io.github.beppe2880.opencodemobile.core.domain.PromptDelivery
import io.github.beppe2880.opencodemobile.core.domain.QueuedPrompt
import io.github.beppe2880.opencodemobile.core.domain.RecentModelRepository
import io.github.beppe2880.opencodemobile.core.domain.SelectionApply
import io.github.beppe2880.opencodemobile.core.domain.ServerEvent
import io.github.beppe2880.opencodemobile.core.domain.Session
import io.github.beppe2880.opencodemobile.core.domain.SessionOutcome
import io.github.beppe2880.opencodemobile.core.domain.SessionRepository
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.ShellOutput
import io.github.beppe2880.opencodemobile.core.domain.TokenUsage
import io.github.beppe2880.opencodemobile.core.domain.ToolStatus
import io.github.beppe2880.opencodemobile.core.domain.VcsBranch
import io.github.beppe2880.opencodemobile.core.domain.VcsRepository
import io.github.beppe2880.opencodemobile.core.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val model = ModelRef("haiku", "anthropic")
    private val chat = FakeChat(listOf(Message.User("u1", 0, "hello")))
    private val events = FakeEvents()
    private val settings = FakeSettings()
    private val drafts = FakeDrafts()
    private val scroll = ScrollMemory()
    private val mcps = FakeMcps()
    private val recents = FakeRecents()
    private val vcs = FakeVcs()

    @Test
    fun `MCP menu and slash open the same project panel without prompting`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        assertTrue(mcps.directories.isEmpty())
        vm.onEvent(ChatEvent.OpenMcps)
        advanceUntilIdle()
        assertEquals(listOf("/repo"), mcps.directories)
        assertEquals(mcps.servers, vm.state.value.mcps?.servers)
        vm.onEvent(ChatEvent.CloseMcps)
        assertNull(vm.state.value.mcps)
        vm.onEvent(ChatEvent.DraftChanged("/mcps"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        assertEquals(mcps.servers, vm.state.value.mcps?.servers)
        assertEquals("", vm.state.value.draft)
        assertTrue(chat.prompts.isEmpty())
        assertTrue(chat.commands.isEmpty())
        vm.onEvent(ChatEvent.CloseMcps)
        vm.onEvent(ChatEvent.DraftChanged("/mc"))
        vm.onEvent(ChatEvent.PickSuggestion(vm.state.value.suggestions.single()))
        advanceUntilIdle()
        assertEquals(mcps.servers, vm.state.value.mcps?.servers)
    }

    @Test
    fun `MCP connection is reloaded and duplicate requests are ignored while pending`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(ChatEvent.OpenMcps)
        advanceUntilIdle()
        val gate = CompletableDeferred<Unit>()
        mcps.gate = gate
        vm.onEvent(ChatEvent.SetMcpConnected("test", false))
        vm.onEvent(ChatEvent.SetMcpConnected("test", false))
        runCurrent()
        assertEquals(setOf("test"), vm.state.value.mcps?.changing)
        assertEquals(listOf(Triple("/repo", "test", false)), mcps.changes)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(McpStatus.Disabled, vm.state.value.mcps?.servers?.single()?.status)
        assertEquals(emptySet<String>(), vm.state.value.mcps?.changing)
        vm.onEvent(ChatEvent.SetMcpConnected("test", true))
        advanceUntilIdle()
        assertEquals(McpStatus.Connected, vm.state.value.mcps?.servers?.single()?.status)
    }

    @Test
    fun `MCP refresh failure retains content and can be retried`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        mcps.listFailure = OpenCodeError.Unreachable("offline")
        vm.onEvent(ChatEvent.OpenMcps)
        advanceUntilIdle()
        assertTrue(vm.state.value.mcps?.error != null)
        assertFalse(vm.state.value.mcps!!.loading)
        mcps.listFailure = null
        vm.onEvent(ChatEvent.RefreshMcps)
        advanceUntilIdle()
        assertNull(vm.state.value.mcps?.error)
        mcps.listFailure = OpenCodeError.Unreachable("offline")
        vm.onEvent(ChatEvent.RefreshMcps)
        advanceUntilIdle()
        assertEquals(mcps.servers, vm.state.value.mcps?.servers)
        assertTrue(vm.state.value.mcps?.error != null)
    }

    @Test
    fun `MCP connection failure does not fake success and reports the error`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(ChatEvent.OpenMcps)
        advanceUntilIdle()
        mcps.changeFailure = OpenCodeError.Unreachable("offline")
        vm.onEvent(ChatEvent.SetMcpConnected("test", false))
        advanceUntilIdle()
        assertTrue(vm.effect.first() is ChatEffect.ShowMessage)
        assertEquals(McpStatus.Connected, vm.state.value.mcps?.servers?.single()?.status)
        assertTrue(vm.state.value.mcps!!.changing.isEmpty())
    }

    @Test
    fun `MCP status events update an open panel but do not fetch when closed`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(ChatEvent.OpenMcps)
        advanceUntilIdle()
        mcps.servers = listOf(McpServer("test", McpStatus.NeedsAuth))
        events.flow.emit(ServerEvent.McpsChanged)
        advanceUntilIdle()
        assertEquals(McpStatus.NeedsAuth, vm.state.value.mcps?.servers?.single()?.status)
        vm.onEvent(ChatEvent.CloseMcps)
        val requests = mcps.directories.size
        events.flow.emit(ServerEvent.McpsChanged)
        advanceUntilIdle()
        assertEquals(requests, mcps.directories.size)
    }

    @Test
    fun `closing the MCP panel during a change does not reopen it`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(ChatEvent.OpenMcps)
        advanceUntilIdle()
        val gate = CompletableDeferred<Unit>()
        mcps.gate = gate
        vm.onEvent(ChatEvent.SetMcpConnected("test", false))
        runCurrent()
        vm.onEvent(ChatEvent.CloseMcps)
        gate.complete(Unit)
        advanceUntilIdle()
        assertNull(vm.state.value.mcps)
        vm.onEvent(ChatEvent.OpenMcps)
        advanceUntilIdle()
        assertEquals(McpStatus.Disabled, vm.state.value.mcps?.servers?.single()?.status)
    }

    @Test
    fun `MCP panel opened before the session directory is known loads once it is`() = runTest(main.testDispatcher) {
        val sessions = FakeSessions(model).apply {
            uncached = true
            loaded = CompletableDeferred()
        }
        val vm = vm(sessions)
        runCurrent()
        vm.onEvent(ChatEvent.DraftChanged("/mcps"))
        vm.onEvent(ChatEvent.Send)
        runCurrent()
        assertTrue(mcps.directories.isEmpty())
        sessions.loaded!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf("/repo"), mcps.directories)
        assertEquals(mcps.servers, vm.state.value.mcps?.servers)
        assertFalse(vm.state.value.mcps!!.loading)
    }

    @Test
    fun `opening MCPs from the menu keeps the unsent prompt and attachments`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        val attachment = PromptAttachment("data:text/plain;base64,QQ==", "note.txt", "text/plain")
        vm.onEvent(ChatEvent.DraftChanged("unfinished prompt"))
        vm.onEvent(ChatEvent.AddAttachments(listOf(attachment)))
        mcps.servers = emptyList()
        vm.onEvent(ChatEvent.OpenMcps)
        advanceUntilIdle()
        assertEquals(emptyList<McpServer>(), vm.state.value.mcps?.servers)
        assertEquals("unfinished prompt", vm.state.value.draft)
        assertEquals(listOf(attachment), vm.state.value.attachments)
        assertFalse(vm.state.value.mcps!!.loading)
    }

    @Test
    fun `the reading position is kept for the next opening of the chat`() = runTest(main.testDispatcher) {
        vm().onEvent(ChatEvent.ScrollMoved(ScrollMemory.Anchor("u1", 12)))
        assertEquals(ScrollMemory.Anchor("u1", 12), vm().state.value.savedScroll)

        vm().onEvent(ChatEvent.ScrollMoved(null))
        assertEquals(null, vm().state.value.savedScroll)
    }

    private fun vm(sessions: FakeSessions = FakeSessions(model)) =
        ChatViewModel("s1", sessions, chat, FakeCatalog(model), events, settings, drafts, ImageMemory(), scroll, mcps, recents, vcs, clock = { 42 })

    @Test
    fun `the header line shows folder, branch and changed files and follows step ends`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        assertEquals(VcsLine("repo", "main", 2), vm.state.value.vcs)
        assertTrue(vm.state.value.folderChanges)
        assertEquals(listOf("/repo"), vcs.branchRequests)

        vcs.files = listOf("a.kt")
        events.flow.emit(ServerEvent.StepEnded("s1", "m1", "stop", 0.0, TokenUsage(), 0))
        events.flow.emit(ServerEvent.ExecutionEnded("s1", SessionOutcome.Succeeded))
        advanceUntilIdle()
        assertEquals(VcsLine("repo", "main", 1), vm.state.value.vcs)
        // Both events settle into one read.
        assertEquals(2, vcs.statusRequests)
    }

    @Test
    fun `tapping the header line opens the folder changes, which match its count`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        val diffs = listOf(
            FileDiff("a.kt", "modified", 1, 1, "@@ -1 +1 @@"),
            FileDiff("new.kt", "added", 2, 0, "@@ -0,0 +1,2 @@"),
            FileDiff("c.kt", "deleted", 0, 3, ""),
        )
        vcs.diffs = diffs
        vm.onEvent(ChatEvent.OpenHeaderChanges)
        advanceUntilIdle()
        assertEquals(ChangesPanel(ChangesScope.Folder, diffs, loading = false), vm.state.value.changes)
        assertEquals(listOf("/repo"), vcs.diffRequests)
        assertTrue(chat.changesRequests.isEmpty())
        assertEquals(3, vm.state.value.vcs?.files)

        // Session and turn scopes are unchanged.
        vm.onEvent(ChatEvent.LoadChanges(ChangesScope.Session))
        advanceUntilIdle()
        assertEquals(listOf(ChangesScope.Session), chat.changesRequests)

        // A step reloads the open folder scope.
        vm.onEvent(ChatEvent.LoadChanges(ChangesScope.Folder))
        advanceUntilIdle()
        events.flow.emit(ServerEvent.StepEnded("s1", "m1", "stop", 0.0, TokenUsage(), 0))
        advanceUntilIdle()
        assertEquals(3, vcs.diffRequests.size)
    }

    @Test
    fun `the header line can open the session changes instead`() = runTest(main.testDispatcher) {
        settings.settings.value = AppSettings(headerOpens = ChangesScope.Session)
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(ChatEvent.OpenHeaderChanges)
        advanceUntilIdle()
        assertEquals(ChangesScope.Session, vm.state.value.changes?.scope)
        assertTrue(vcs.diffRequests.isEmpty())
    }

    @Test
    fun `branch switches of this project arrive from the event stream`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        events.flow.emit(ServerEvent.BranchChanged("/other", "feature"))
        advanceUntilIdle()
        assertEquals("main", vm.state.value.vcs?.branch)

        vcs.files = emptyList()
        events.flow.emit(ServerEvent.BranchChanged("/repo", "feature"))
        advanceUntilIdle()
        assertEquals(VcsLine("repo", "feature", 0), vm.state.value.vcs)
        assertEquals(1, vcs.branchRequests.size)
    }

    @Test
    fun `outside a repository only files the agent edited are counted and the session opens`() = runTest(main.testDispatcher) {
        vcs.branch = null
        val edit = Part.Tool(
            "c1", "edit", ToolStatus.Completed, "{}",
            diffs = listOf(FileDiff("a.kt", null, 1, 0, ""), FileDiff("b.kt", null, 1, 0, "")),
        )
        val again = Part.Tool("c2", "write", ToolStatus.Completed, "{}", diffs = listOf(FileDiff("a.kt", null, 1, 0, "")))
        val vm = vm()
        advanceUntilIdle()
        assertNull(vm.state.value.vcs)
        assertFalse(vm.state.value.folderChanges)
        assertEquals(0, vcs.statusRequests)

        chat.transcript = chat.transcript + Message.Assistant("a1", 5, "build", model, listOf(edit, again), completedAt = 6)
        events.flow.emit(ServerEvent.StepEnded("s1", "a1", "stop", 0.0, TokenUsage(), 0))
        advanceUntilIdle()
        assertEquals(VcsLine("repo", null, 2, agentEdits = true), vm.state.value.vcs)
        assertEquals(0, vcs.statusRequests)

        vm.onEvent(ChatEvent.OpenHeaderChanges)
        advanceUntilIdle()
        assertEquals(ChangesScope.Session, vm.state.value.changes?.scope)
    }

    @Test
    fun `the header line content follows the setting`() = runTest(main.testDispatcher) {
        settings.settings.value = AppSettings(chatHeaderLine = ChatHeaderLine.Folder)
        val vm = vm()
        advanceUntilIdle()
        assertNull(vm.state.value.vcs)
        assertTrue(vcs.branchRequests.isEmpty())

        settings.settings.value = AppSettings(chatHeaderLine = ChatHeaderLine.BranchFiles)
        advanceUntilIdle()
        assertEquals(VcsLine(null, "main", 2), vm.state.value.vcs)

        settings.settings.value = AppSettings(chatHeaderLine = ChatHeaderLine.FolderBranchFiles)
        advanceUntilIdle()
        assertEquals(VcsLine("repo", "main", 2), vm.state.value.vcs)

        settings.settings.value = AppSettings(chatHeaderLine = ChatHeaderLine.Folder)
        advanceUntilIdle()
        assertNull(vm.state.value.vcs)
    }

    @Test
    fun `the changes button finds out whether the folder can be shown even without the header line`() = runTest(main.testDispatcher) {
        settings.settings.value = AppSettings(chatHeaderLine = ChatHeaderLine.Folder)
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(ChatEvent.OpenChanges)
        advanceUntilIdle()
        assertTrue(vm.state.value.folderChanges)
        assertNull(vm.state.value.vcs)
        assertEquals(0, vcs.statusRequests)
    }

    @Test
    fun `the microphone follows the voice input setting`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        assertTrue(vm.state.value.dictation)

        settings.settings.value = AppSettings(dictation = false)
        advanceUntilIdle()
        assertFalse(vm.state.value.dictation)
    }

    @Test
    fun `the draft is restored, kept while typing and cleared once sent`() = runTest(main.testDispatcher) {
        drafts.save("s1", "half a thought")
        val vm = vm()
        advanceUntilIdle()
        assertEquals("half a thought", vm.state.value.draft)

        vm.onEvent(ChatEvent.DraftChanged("half a thought, finished"))
        advanceUntilIdle()
        assertEquals("half a thought, finished", drafts.load("s1"))

        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        assertEquals("", drafts.load("s1"))
    }

    @Test
    fun `neighbors follow the session list order and swiping switches session`() = runTest(main.testDispatcher) {
        val sessions = FakeSessions(model, order = listOf("s0", "s1", "s2"), titles = mapOf("s0" to "Newer", "s2" to "Older"))
        val vm = vm(sessions)
        advanceUntilIdle()
        assertEquals(SessionPeek("s0", "Newer"), vm.state.value.previousSession)
        assertEquals(SessionPeek("s2", "Older"), vm.state.value.nextSession)

        vm.onEvent(ChatEvent.SwitchSession("s2"))
        assertEquals(ChatEffect.SwitchSession("s2"), vm.effect.first())

        // Opened outside the list (e.g. a subagent): nothing to swipe to.
        val alone = vm(FakeSessions(model, order = listOf("x")))
        assertNull(alone.state.value.previousSession)
        assertNull(alone.state.value.nextSession)
    }

    @Test
    fun `queued prompts show above the composer and can be sent now, edited or removed`() = runTest(main.testDispatcher) {
        chat.inbox = listOf(
            QueuedPrompt("q1", "add tests", PromptDelivery.Queue, 1),
            QueuedPrompt("q2", "then lint", PromptDelivery.Queue, 2),
            QueuedPrompt("q3", "steering", PromptDelivery.Steer, 3),
        )
        val vm = vm()
        advanceUntilIdle()
        assertEquals(listOf("q1", "q2"), vm.state.value.queued.map { it.id })

        // The enqueued prompt is not repeated as a pending bubble in the transcript.
        events.flow.emit(ServerEvent.InboxEnqueued("s1", "q1", "add tests", 1))
        advanceUntilIdle()
        assertFalse(vm.state.value.items.any { it.key == "q1" })

        vm.onEvent(ChatEvent.SteerQueued("q1"))
        advanceUntilIdle()
        assertEquals(listOf("q1"), chat.steered)
        assertEquals(listOf("q2"), vm.state.value.queued.map { it.id })
        assertTrue(vm.state.value.items.any { it.key == "q1" })

        vm.onEvent(ChatEvent.DraftChanged("and docs"))
        vm.onEvent(ChatEvent.EditQueued("q2"))
        advanceUntilIdle()
        assertEquals(listOf("q2"), chat.cancelled)
        assertEquals("then lint\n\nand docs", vm.state.value.draft)
        assertTrue(vm.state.value.queued.isEmpty())
    }

    @Test
    fun `a bang draft runs in the shell instead of going to the agent`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()

        vm.onEvent(ChatEvent.DraftChanged("!"))
        assertTrue(vm.state.value.isShell)
        assertFalse(vm.state.value.canSend)

        vm.onEvent(ChatEvent.DraftChanged("!git status"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        assertEquals(listOf("git status"), chat.shells)
        assertTrue(chat.prompts.isEmpty())
        assertEquals("", vm.state.value.draft)

        // A failed command gives the draft back.
        chat.shellResult = Result.failure(OpenCodeError.Unreachable("offline"))
        vm.onEvent(ChatEvent.DraftChanged("!ls"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        assertEquals("!ls", vm.state.value.draft)
    }

    @Test
    fun `the changes sheet loads the whole session first, then the chosen range`() = runTest(main.testDispatcher) {
        val edit = FileDiff("src/a.kt", "modified", 1, 1, "@@ -1 +1 @@\n-a\n+b")
        chat.changesByScope[ChangesScope.Session] = Result.success(listOf(edit))
        chat.changesByScope[ChangesScope.LastTurn] = Result.failure(OpenCodeError.Unreachable("offline"))
        val vm = vm()
        advanceUntilIdle()

        vm.onEvent(ChatEvent.OpenChanges)
        advanceUntilIdle()
        assertEquals(ChangesPanel(ChangesScope.Session, files = listOf(edit), loading = false), vm.state.value.changes)

        vm.onEvent(ChatEvent.LoadChanges(ChangesScope.LastTurn))
        advanceUntilIdle()
        val failed = vm.state.value.changes!!
        assertNull(failed.files)
        assertTrue(failed.error != null)

        // A finished step may have edited files: the open sheet reloads its range.
        events.flow.emit(ServerEvent.StepEnded("s1", "m1", "stop", 0.0, TokenUsage(), 0))
        advanceUntilIdle()
        assertEquals(listOf(ChangesScope.Session, ChangesScope.LastTurn, ChangesScope.LastTurn), chat.changesRequests)

        vm.onEvent(ChatEvent.CloseChanges)
        assertNull(vm.state.value.changes)
    }

    @Test
    fun `loads session, transcript, catalog and permissions`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.loading)
        assertEquals("Title", state.title)
        assertEquals("plan", state.agent)
        assertEquals(ModelLabel("Claude Haiku", "Anthropic"), state.modelLabel)
        assertEquals(listOf("u1"), state.items.map { it.key })
        assertEquals(listOf("build", "plan", "review"), state.agents.map { it.id })
        assertEquals(1, state.permissions.size)
    }

    @Test
    fun `the cached transcript shows before the server sync completes`() = runTest(main.testDispatcher) {
        chat.cached = listOf(Message.User("u0", 0, "from cache"))
        chat.gate = CompletableDeferred()
        val vm = vm()
        runCurrent()

        assertFalse(vm.state.value.loading)
        assertEquals(listOf("u0"), vm.state.value.items.map { it.key })

        chat.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf("u1"), vm.state.value.items.map { it.key })
    }

    @Test
    fun `the header comes from the cached session before anything loads`() = runTest(main.testDispatcher) {
        chat.gate = CompletableDeferred()
        val vm = vm()

        assertEquals("Title", vm.state.value.title)
        assertEquals("/repo", vm.state.value.directory)
    }

    @Test
    fun `slash suggestions list builtins and server commands, and known commands run as commands`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()

        vm.onEvent(ChatEvent.DraftChanged("/"))
        assertEquals(listOf("undo", "redo", "compact", "fork", "mcps", "review"), vm.state.value.suggestions.map { (it as Suggestion.Slash).name })
        vm.onEvent(ChatEvent.PickSuggestion(vm.state.value.suggestions.last()))
        assertEquals("/review ", vm.state.value.draft)

        vm.onEvent(ChatEvent.DraftChanged("/review the diff"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        assertEquals(listOf("review" to "the diff"), chat.commands)
        assertTrue(chat.prompts.isEmpty())

        // Unknown commands are sent as plain prompts.
        vm.onEvent(ChatEvent.DraftChanged("/nope x"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        assertEquals(listOf("/nope x" to PromptDelivery.Steer), chat.prompts)
    }

    @Test
    fun `picking an at-mention completes the path and attaches the file`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()

        vm.onEvent(ChatEvent.DraftChanged("look at @comp"))
        advanceUntilIdle()
        assertEquals(listOf(Suggestion.File("src/Composer.kt")), vm.state.value.suggestions)

        vm.onEvent(ChatEvent.PickSuggestion(Suggestion.File("src/Composer.kt")))
        assertEquals("look at @src/Composer.kt ", vm.state.value.draft)
        assertEquals(
            listOf(PromptAttachment("file:///repo/src/Composer.kt", "src/Composer.kt", mention = "@src/Composer.kt")),
            vm.state.value.attachments,
        )
        assertTrue(vm.state.value.suggestions.isEmpty())

        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        assertEquals("@src/Composer.kt", chat.sentAttachments.single().mention)
        assertTrue(vm.state.value.attachments.isEmpty())
    }

    @Test
    fun `uploads alone can be sent and count on the pending prompt`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        val photo = PromptAttachment("data:image/png;base64,AA==", "shot.png", "image/png")

        vm.onEvent(ChatEvent.AddAttachments(listOf(photo, photo)))
        assertEquals(listOf(photo), vm.state.value.uploads)
        assertTrue(vm.state.value.canSend)
        vm.onEvent(ChatEvent.Send)

        assertEquals(1, vm.state.value.items.filterIsInstance<ChatItem.User>().last().attachments)
        advanceUntilIdle()
        assertEquals(listOf(photo), chat.sentAttachments)
    }

    @Test
    fun `revert hides the prompt and what follows, restores its text, and can be undone`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()

        vm.onEvent(ChatEvent.Revert("u1"))
        advanceUntilIdle()
        assertEquals(listOf("u1"), chat.reverts)
        assertEquals("hello", vm.state.value.draft)
        assertTrue(vm.state.value.items.isEmpty())
        assertEquals(1, vm.state.value.revertedCount)

        vm.onEvent(ChatEvent.RestoreRevert)
        advanceUntilIdle()
        assertEquals(listOf("u1"), vm.state.value.items.map { it.key })
        assertEquals(0, vm.state.value.revertedCount)
    }

    @Test
    fun `fork copies the history before the prompt and opens it with the prompt as draft`() = runTest(main.testDispatcher) {
        val sessions = FakeSessions(model)
        val vm = vm(sessions)
        advanceUntilIdle()

        vm.onEvent(ChatEvent.Fork("u1"))
        advanceUntilIdle()
        assertEquals(listOf("s1" to "u1"), sessions.forks)
        assertEquals("hello", drafts.load("s2"))
        assertEquals(ChatEffect.OpenSession("s2"), vm.effect.first())
        // The original chat is untouched.
        assertEquals(listOf("u1"), vm.state.value.items.map { it.key })
        assertEquals("", vm.state.value.draft)
        assertFalse(vm.state.value.forking)
    }

    @Test
    fun `slash fork copies the whole session and opens it`() = runTest(main.testDispatcher) {
        val sessions = FakeSessions(model)
        val vm = vm(sessions)
        advanceUntilIdle()

        vm.onEvent(ChatEvent.DraftChanged("/fork"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        assertEquals(listOf<Pair<String, String?>>("s1" to null), sessions.forks)
        assertEquals(ChatEffect.OpenSession("s2"), vm.effect.first())
        assertEquals("", drafts.load("s2"))
        assertTrue(chat.prompts.isEmpty())
        assertEquals("", vm.state.value.draft)
    }

    @Test
    fun `a failed fork keeps the chat and reports the error`() = runTest(main.testDispatcher) {
        val sessions = FakeSessions(model).apply { forkResult = Result.failure(OpenCodeError.Unreachable("down")) }
        val vm = vm(sessions)
        advanceUntilIdle()

        vm.onEvent(ChatEvent.Fork("u1"))
        advanceUntilIdle()
        assertTrue(vm.effect.first() is ChatEffect.ShowMessage)
        assertEquals("", drafts.load("s2"))
        assertFalse(vm.state.value.forking)
    }

    @Test
    fun `undo finds the previous prompt on the server when a long turn pushed it out of the loaded history`() = runTest(main.testDispatcher) {
        // Only the newest page is loaded: the photo prompt and the agent's long previous turn.
        chat.transcript = listOf(
            Message.Assistant("msg_02", 2, "build", model, emptyList(), completedAt = 3),
            Message.User("msg_04", 4, "", images = listOf(ImageAttachment("photo.png", "image/png", "data:image/png;base64,AA=="))),
            Message.Assistant("msg_05", 5, "build", model, emptyList(), completedAt = 6),
        )
        chat.serverPrompts = listOf(Message.User("msg_04", 4, ""), Message.User("msg_01", 1, "add the settings"))
        val vm = vm()
        advanceUntilIdle()

        // The first undo hides the photo prompt and puts the photo back in the composer.
        vm.onEvent(ChatEvent.DraftChanged("/undo"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        assertEquals(listOf("msg_04"), chat.reverts)
        assertEquals(listOf("photo.png"), vm.state.value.attachments.map { it.name })

        // The second one reverts a prompt that is not loaded: everything loaded is hidden.
        vm.onEvent(ChatEvent.DraftChanged("/undo"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        assertEquals(listOf("msg_04", "msg_01"), chat.reverts)
        assertEquals("add the settings", vm.state.value.draft)
        assertTrue(vm.state.value.items.isEmpty())
    }

    @Test
    fun `tapping the undo and redo suggestions runs them at once`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()

        vm.onEvent(ChatEvent.DraftChanged("/un"))
        vm.onEvent(ChatEvent.PickSuggestion(vm.state.value.suggestions.single()))
        advanceUntilIdle()
        assertEquals(listOf("u1"), chat.reverts)
        assertEquals(1, vm.state.value.revertedCount)
        assertEquals("hello", vm.state.value.draft)
        assertTrue(vm.state.value.suggestions.isEmpty())

        vm.onEvent(ChatEvent.DraftChanged("/re"))
        vm.onEvent(ChatEvent.PickSuggestion(Suggestion.Slash("redo", null)))
        advanceUntilIdle()
        assertEquals(0, vm.state.value.revertedCount)
        assertEquals("", vm.state.value.draft)
    }

    @Test
    fun `slash undo reverts the last prompt and a new prompt commits the revert`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()

        vm.onEvent(ChatEvent.DraftChanged("/undo"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        assertEquals(listOf("u1"), chat.reverts)

        vm.onEvent(ChatEvent.DraftChanged("hello again"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        assertEquals(1, chat.commits)
        assertEquals(0, vm.state.value.revertedCount)
    }

    @Test
    fun `forms start from defaults and submit only when required fields are answered`() = runTest(main.testDispatcher) {
        chat.forms = listOf(
            Form(
                id = "f1",
                sessionId = "s1",
                title = "Which storage?",
                fields = listOf(
                    FormField.Text("choice", required = true, options = listOf(FormOption("room", "Room"), FormOption("file", "File"))),
                    FormField.Toggle("tests", default = true),
                    FormField.Text("why", conditions = listOf(FormCondition("choice", equals = true, value = FormAnswer.Text("file")))),
                ),
                presetAnswers = mapOf("source" to FormAnswer.Text("mobile")),
            ),
        )
        val vm = vm()
        advanceUntilIdle()
        assertEquals(mapOf("source" to FormAnswer.Text("mobile"), "tests" to FormAnswer.Bool(true)), vm.state.value.formAnswers)

        vm.onEvent(ChatEvent.SubmitForm)
        advanceUntilIdle()
        assertTrue(chat.formReplies.isEmpty())

        vm.onEvent(ChatEvent.FormAnswerChanged("choice", FormAnswer.Text("room")))
        vm.onEvent(ChatEvent.FormAnswerChanged("why", FormAnswer.Text("hidden, not sent")))
        vm.onEvent(ChatEvent.SubmitForm)
        advanceUntilIdle()

        assertEquals(
            listOf("f1" to mapOf("source" to FormAnswer.Text("mobile"), "choice" to FormAnswer.Text("room"), "tests" to FormAnswer.Bool(true))),
            chat.formReplies,
        )
        assertTrue(vm.state.value.forms.isEmpty())
    }

    @Test
    fun `collapsing a question keeps its answers and tells the server nothing`() = runTest(main.testDispatcher) {
        chat.forms = listOf(Form("f1", "s1", "Which storage?", listOf(FormField.Text("choice", required = true))))
        val vm = vm()
        advanceUntilIdle()
        assertFalse(vm.state.value.isFormCollapsed(vm.state.value.forms.single().id))

        vm.onEvent(ChatEvent.FormAnswerChanged("choice", FormAnswer.Text("room")))
        vm.onEvent(ChatEvent.CollapseForm(vm.state.value.forms.single().id, true))
        events.flow.emit(ServerEvent.FormsChanged("s1"))
        advanceUntilIdle()

        assertTrue(vm.state.value.isFormCollapsed(vm.state.value.forms.single().id))
        assertEquals(mapOf("choice" to FormAnswer.Text("room")), vm.state.value.formAnswers)
        assertEquals("f1", vm.state.value.forms.single().id)
        assertTrue(chat.formReplies.isEmpty())

        vm.onEvent(ChatEvent.CollapseForm(vm.state.value.forms.single().id, false))
        assertFalse(vm.state.value.isFormCollapsed(vm.state.value.forms.single().id))
        assertEquals(mapOf("choice" to FormAnswer.Text("room")), vm.state.value.formAnswers)
    }

    @Test
    fun `collapsing a permission request never replies to it`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        assertFalse(vm.state.value.isPermissionCollapsed("p1"))

        vm.onEvent(ChatEvent.CollapsePermission("p1", true))
        events.flow.emit(ServerEvent.PermissionChanged("s1"))
        advanceUntilIdle()

        assertTrue(vm.state.value.isPermissionCollapsed("p1"))
        assertEquals("p1", vm.state.value.permissions.single().id)
        assertTrue(chat.replies.isEmpty())

        vm.onEvent(ChatEvent.CollapsePermission("p1", false))
        assertFalse(vm.state.value.isPermissionCollapsed("p1"))
        assertTrue(chat.replies.isEmpty())
    }

    @Test
    fun `permission requests start collapsed when the setting says so, each folding on its own`() = runTest(main.testDispatcher) {
        settings.settings.value = AppSettings(permissionsCollapsed = true)
        val vm = vm()
        advanceUntilIdle()
        assertTrue(vm.state.value.isPermissionCollapsed("p1"))
        assertTrue(vm.state.value.isPermissionCollapsed("p2"))

        vm.onEvent(ChatEvent.CollapsePermission("p1", false))
        assertFalse(vm.state.value.isPermissionCollapsed("p1"))
        assertTrue(vm.state.value.isPermissionCollapsed("p2"))
    }

    @Test
    fun `each new question starts collapsed or open as the setting says`() = runTest(main.testDispatcher) {
        settings.settings.value = AppSettings(questionsCollapsed = true)
        chat.forms = listOf(Form("f1", "s1", "First?", listOf(FormField.Toggle("ok"))))
        val vm = vm()
        advanceUntilIdle()
        assertTrue(vm.state.value.isFormCollapsed(vm.state.value.forms.single().id))

        vm.onEvent(ChatEvent.CollapseForm(vm.state.value.forms.single().id, false))
        settings.settings.value = AppSettings(questionsCollapsed = false)
        chat.forms = listOf(Form("f2", "s1", "Second?", listOf(FormField.Toggle("ok"))))
        events.flow.emit(ServerEvent.FormsChanged("s1"))
        advanceUntilIdle()
        assertFalse(vm.state.value.isFormCollapsed(vm.state.value.forms.single().id))

        // Folding one question does not fold the next one.
        vm.onEvent(ChatEvent.CollapseForm(vm.state.value.forms.single().id, true))
        chat.forms = listOf(Form("f3", "s1", "Third?", listOf(FormField.Toggle("ok"))))
        events.flow.emit(ServerEvent.FormsChanged("s1"))
        advanceUntilIdle()
        assertFalse(vm.state.value.isFormCollapsed(vm.state.value.forms.single().id))
        assertEquals("f3", vm.state.value.forms.single().id)
    }

    @Test
    fun `form events refresh the pending questions`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        chat.forms = listOf(Form("f2", "s1", "Continue?", listOf(FormField.Toggle("ok"))))

        events.flow.emit(ServerEvent.FormsChanged("s1"))
        advanceUntilIdle()

        assertEquals("f2", vm.state.value.forms.single().id)
    }

    @Test
    fun `older history loads one page at a time when asked, until it is complete`() = runTest(main.testDispatcher) {
        chat.olderPages += listOf(Message.User("u-1", -1, "older"))
        chat.olderPages += listOf(Message.User("u-3", -3, "oldest"), Message.User("u-2", -2, "old"))
        val vm = vm()
        advanceUntilIdle()
        // Nothing older is fetched until the user scrolls up.
        assertEquals(listOf("u1"), vm.state.value.items.map { it.key })

        vm.onEvent(ChatEvent.LoadOlder)
        advanceUntilIdle()
        assertEquals(listOf("u-1", "u1"), vm.state.value.items.map { it.key })

        vm.onEvent(ChatEvent.LoadOlder)
        advanceUntilIdle()
        assertEquals(listOf("u-3", "u-2", "u-1", "u1"), vm.state.value.items.map { it.key })
        assertTrue(vm.state.value.hasOlder)

        vm.onEvent(ChatEvent.LoadOlder)
        advanceUntilIdle()
        assertFalse(vm.state.value.hasOlder)
        assertFalse(vm.state.value.loadingOlder)
    }

    @Test
    fun `older history loaded beyond the cache stays shown when the transcript reloads`() = runTest(main.testDispatcher) {
        chat.transcript = listOf(Message.User("u1", 1, "one"), Message.User("u2", 2, "two"))
        chat.olderPages += listOf(Message.User("u0", 0, "zero"))
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(ChatEvent.LoadOlder)
        advanceUntilIdle()

        // The capped cache dropped u1 when u3 arrived; what is already shown is kept.
        chat.transcript = listOf(Message.User("u2", 2, "two"), Message.User("u3", 3, "three"))
        events.flow.emit(ServerEvent.Connected)
        advanceUntilIdle()

        assertEquals(listOf("u0", "u1", "u2", "u3"), vm.state.value.items.map { it.key })
        assertTrue(vm.state.value.hasOlder)
    }

    @Test
    fun `a failed older page offers a retry and keeps the history open`() = runTest(main.testDispatcher) {
        chat.olderResult = Result.failure(OpenCodeError.Unreachable("offline"))
        val vm = vm()
        advanceUntilIdle()

        vm.onEvent(ChatEvent.LoadOlder)
        advanceUntilIdle()
        assertTrue(vm.state.value.olderFailed)
        assertTrue(vm.state.value.hasOlder)

        chat.olderResult = null
        chat.olderPages += listOf(Message.User("u0", -1, "older"))
        vm.onEvent(ChatEvent.LoadOlder)
        advanceUntilIdle()
        assertFalse(vm.state.value.olderFailed)
        assertEquals(listOf("u0", "u1"), vm.state.value.items.map { it.key })
    }

    @Test
    fun `images the cache dropped are fetched once for the thumbnails and the preview`() = runTest(main.testDispatcher) {
        chat.transcript = listOf(
            Message.User(
                "u1", 0, "look", attachments = 2,
                images = listOf(ImageAttachment("a.png", "image/png", "data:image/png;base64,AAAA"), ImageAttachment("b.png", "image/png", null)),
            ),
        )
        chat.images = Result.success(listOf("data:image/png;base64,AAAA", "data:image/png;base64,BBBB"))
        val vm = vm()
        advanceUntilIdle()

        val user = vm.state.value.items.single() as ChatItem.User
        assertEquals(listOf("data:image/png;base64,AAAA", "data:image/png;base64,BBBB"), user.images.map { it.dataUri })
        assertEquals(listOf("u1"), chat.imageRequests)

        vm.onEvent(ChatEvent.OpenImage("u1", 1))
        assertEquals(ImagePreview("b.png", "data:image/png;base64,BBBB"), vm.state.value.preview)
        vm.onEvent(ChatEvent.ClosePreview)
        assertEquals(null, vm.state.value.preview)
        assertEquals(listOf("u1"), chat.imageRequests)
    }

    @Test
    fun `an image that cannot be fetched shows the failure`() = runTest(main.testDispatcher) {
        chat.transcript = listOf(Message.User("u1", 0, "look", attachments = 1, images = listOf(ImageAttachment("b.png", "image/png", null))))
        chat.images = Result.failure(OpenCodeError.Unreachable("down"))
        val vm = vm()
        advanceUntilIdle()

        vm.onEvent(ChatEvent.OpenImage("u1", 0))
        assertEquals(ImagePreview("b.png", null), vm.state.value.preview)
        advanceUntilIdle()

        assertEquals(ImagePreview("b.png", null, failed = true), vm.state.value.preview)
    }

    @Test
    fun `the neighbors are prefetched once the chat has loaded`() = runTest(main.testDispatcher) {
        vm(FakeSessions(model, order = listOf("s0", "s1", "s2")))
        advanceUntilIdle()

        assertEquals(listOf(listOf("s0", "s2") to 50), chat.prefetched)
    }

    @Test
    fun `an offline sync keeps the cached transcript without a full-screen error`() = runTest(main.testDispatcher) {
        chat.cached = listOf(Message.User("u0", 0, "from cache"))
        chat.messagesResult = Result.failure(OpenCodeError.Unreachable("down"))
        val vm = vm()
        advanceUntilIdle()

        assertEquals(null, vm.state.value.error)
        assertEquals(listOf("u0"), vm.state.value.items.map { it.key })
    }

    @Test
    fun `live events stream text into the transcript`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()

        events.flow.emit(ServerEvent.ExecutionStarted("s1"))
        events.flow.emit(ServerEvent.StepStarted("s1", "a1", "build", model, 10))
        events.flow.emit(ServerEvent.TextDelta("s1", "a1", 0, "Hel"))
        events.flow.emit(ServerEvent.TextDelta("s1", "a1", 0, "lo"))
        events.flow.emit(ServerEvent.TextDelta("other", "x", 0, "ignored"))
        runCurrent()

        val state = vm.state.value
        assertTrue(state.running)
        val text = state.items.filterIsInstance<ChatItem.Text>().single()
        assertEquals("Hello", text.text)
        assertTrue(text.streaming)
    }

    @Test
    fun `a running shell command can be moved to the background`() = runTest(main.testDispatcher) {
        val shell = Part.Tool("c1", "shell", ToolStatus.Running, input = "{}", startedAt = 7)
        val read = Part.Tool("c0", "read", ToolStatus.Running, input = "{}")
        chat.transcript = chat.transcript + Message.Assistant("a1", 5, "build", model, listOf(read, shell))
        val vm = vm()
        advanceUntilIdle()
        assertEquals(BackgroundableWork("a1:c1", 7), vm.state.value.backgroundable)

        vm.onEvent(ChatEvent.MoveToBackground)
        advanceUntilIdle()

        assertEquals(1, chat.backgrounds)
        assertNull(vm.state.value.backgroundable)
    }

    @Test
    fun `a running bash tool shows its shell output live until it finishes`() = runTest(main.testDispatcher) {
        val bash = Part.Tool("c1", "bash", ToolStatus.Running, input = "{}", metadata = mapOf("shellID" to "sh_1"))
        chat.transcript = chat.transcript + Message.Assistant("a1", 5, "build", model, listOf(bash))
        chat.shellPages += listOf("line 1\n", "line 2\n")
        val vm = vm()
        advanceTimeBy(2_000)
        runCurrent()

        assertEquals("line 1\nline 2\n", vm.tool("c1").output)
        assertEquals(listOf(0L, 7L, 14L), chat.shellReads.take(3).map { it.second })
        assertTrue(chat.shellReads.all { it.first == "sh_1" })

        events.flow.emit(ServerEvent.ToolSucceeded("s1", "a1", "c1", "line 1\nline 2\nline 3\n", emptyMap(), 9))
        runCurrent()
        val reads = chat.shellReads.size
        advanceTimeBy(5_000)
        runCurrent()

        assertEquals("line 1\nline 2\nline 3\n", vm.tool("c1").output)
        assertEquals(reads, chat.shellReads.size)
    }

    @Test
    fun `running tools are not followed when the setting is off`() = runTest(main.testDispatcher) {
        settings.settings.value = AppSettings(followRunningTools = false)
        val bash = Part.Tool("c1", "bash", ToolStatus.Running, input = "{}", metadata = mapOf("shellID" to "sh_1"))
        chat.transcript = chat.transcript + Message.Assistant("a1", 5, "build", model, listOf(bash))
        chat.shellPages += "line 1\n"
        val vm = vm()
        advanceTimeBy(2_000)
        runCurrent()

        assertTrue(chat.shellReads.isEmpty())
        assertNull(vm.tool("c1").output)
        assertNull(vm.state.value.live)
    }

    @Test
    fun `shell output is read at the interval from the settings`() = runTest(main.testDispatcher) {
        settings.settings.value = AppSettings(liveOutputIntervalMs = 3_000)
        val bash = Part.Tool("c1", "bash", ToolStatus.Running, input = "{}", metadata = mapOf("shellID" to "sh_1"))
        chat.transcript = chat.transcript + Message.Assistant("a1", 5, "build", model, listOf(bash))
        vm()
        advanceTimeBy(5_000)
        runCurrent()

        // Reads at 0 and 3 s; the default 700 ms would have read 8 times.
        assertEquals(2, chat.shellReads.size)
        // The tool ends, so the reading stops and the test can finish.
        events.flow.emit(ServerEvent.ToolSucceeded("s1", "a1", "c1", "", emptyMap(), 9))
        runCurrent()
    }

    @Test
    fun `shell output is not read when shell commands are not followed`() = runTest(main.testDispatcher) {
        settings.settings.value = AppSettings(liveShell = false)
        val bash = Part.Tool("c1", "bash", ToolStatus.Running, input = "{}", metadata = mapOf("shellID" to "sh_1"))
        chat.transcript = chat.transcript + Message.Assistant("a1", 5, "build", model, listOf(bash))
        val vm = vm()
        advanceTimeBy(2_000)
        runCurrent()

        assertTrue(chat.shellReads.isEmpty())
        assertEquals(LiveFollow(shell = false), vm.state.value.live)
    }

    private fun ChatViewModel.tool(callId: String): Part.Tool = state.value.items
        .flatMap { item ->
            when (item) {
                is ChatItem.Tools -> item.tools
                is ChatItem.Activity -> item.tools
                else -> emptyList()
            }
        }
        .single { it.callId == callId }

    @Test
    fun `execution end reloads messages from the server`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        chat.transcript = chat.transcript + Message.Assistant("a1", 5, "build", model, emptyList(), completedAt = 9)
        events.flow.emit(ServerEvent.ExecutionEnded("s1", SessionOutcome.Succeeded))
        advanceUntilIdle()

        assertFalse(vm.state.value.running)
        assertTrue(vm.state.value.items.any { it is ChatItem.Footer })
    }

    @Test
    fun `send adds a pending prompt and clears the draft`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(ChatEvent.DraftChanged("  do it  "))
        vm.onEvent(ChatEvent.Send)

        val pending = vm.state.value.items.filterIsInstance<ChatItem.User>().last()
        assertTrue(pending.pending)
        assertEquals("", vm.state.value.draft)
        assertEquals(ChatEffect.PromptSent, vm.effect.first())
        runCurrent()
        assertEquals(listOf("do it" to PromptDelivery.Steer), chat.prompts)
    }

    @Test
    fun `a missed execution end leaves stop usable after reconnecting`() = runTest(main.testDispatcher) {
        val sessions = FakeSessions(model).apply { activeIds = setOf("s1") }
        val vm = vm(sessions)
        advanceUntilIdle()
        events.flow.emit(ServerEvent.ExecutionStarted("s1"))
        runCurrent()
        assertTrue(vm.state.value.running)

        vm.onEvent(ChatEvent.Stop)
        advanceUntilIdle()
        assertTrue(vm.state.value.stopping)

        // The connection drops before ExecutionEnded arrives; on reconnect the server still
        // reports the session as running.
        events.flow.emit(ServerEvent.Connected)
        advanceUntilIdle()

        assertTrue(vm.state.value.running)
        assertFalse(vm.state.value.stopping)
    }

    @Test
    fun `failed send restores the draft and removes the pending prompt`() = runTest(main.testDispatcher) {
        chat.promptResult = Result.failure(OpenCodeError.Unreachable("down"))
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(ChatEvent.DraftChanged("retry me"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()

        assertEquals("retry me", vm.state.value.draft)
        assertTrue(vm.state.value.items.none { it is ChatItem.User && it.pending })
    }

    @Test
    fun `reverted messages stay gone while the prompt that commits the revert is sent and after it fails`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(ChatEvent.DraftChanged("/undo"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        assertEquals(emptyList<String>(), vm.state.value.items.map { it.key })

        chat.promptResult = Result.failure(OpenCodeError.Unreachable("down"))
        vm.onEvent(ChatEvent.DraftChanged("hello again"))
        vm.onEvent(ChatEvent.Send)
        assertTrue(vm.state.value.items.none { it.key == "u1" })

        advanceUntilIdle()
        // The commit went through, so the server no longer has u1.
        assertEquals(1, chat.commits)
        assertEquals(emptyList<String>(), vm.state.value.items.map { it.key })
        assertEquals(0, vm.state.value.revertedCount)
        assertEquals("hello again", vm.state.value.draft)
    }

    @Test
    fun `a failed revert commit keeps the revert staged`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(ChatEvent.DraftChanged("/undo"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()

        chat.commitResult = Result.failure(OpenCodeError.Unreachable("down"))
        vm.onEvent(ChatEvent.DraftChanged("hello again"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()

        assertEquals(emptyList<String>(), vm.state.value.items.map { it.key })
        assertEquals(1, vm.state.value.revertedCount)
        assertEquals("hello again", vm.state.value.draft)
        // Redo still brings the staged messages back.
        vm.onEvent(ChatEvent.DraftChanged("/redo"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        assertEquals(listOf("u1"), vm.state.value.items.map { it.key })
    }

    @Test
    fun `a failed shell command after a committed revert does not bring the reverted messages back`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(ChatEvent.DraftChanged("/undo"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()

        chat.shellResult = Result.failure(OpenCodeError.Unreachable("down"))
        vm.onEvent(ChatEvent.DraftChanged("!ls"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()

        assertEquals(1, chat.commits)
        assertEquals(emptyList<String>(), vm.state.value.items.map { it.key })
        assertEquals(0, vm.state.value.revertedCount)
    }

    @Test
    fun `permission reply removes the request`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(ChatEvent.ReplyPermission("p1", PermissionDecision.Once))
        advanceUntilIdle()

        assertEquals(listOf("p1" to PermissionDecision.Once), chat.replies)
        assertTrue(vm.state.value.permissions.isEmpty())
    }

    @Test
    fun `toggle flips expansion relative to the default`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(ChatEvent.Toggle("g:x"))
        assertTrue(vm.state.value.isExpanded("g:x"))
        assertFalse(vm.state.value.isExpanded("g:x", defaultExpanded = true))
    }

    @Test
    fun `immediately, variants switch the reasoning level and a new model starts from its default`() = runTest(main.testDispatcher) {
        settings.settings.value = AppSettings(selectionApply = SelectionApply.Immediately)
        val vm = vm()
        advanceUntilIdle()

        vm.onEvent(ChatEvent.SelectVariant("high"))
        advanceUntilIdle()
        assertEquals(model.copy(variant = "high"), chat.switched.last())
        assertEquals("high", vm.state.value.model?.variant)

        // Re-picking the same model keeps the variant and sends nothing.
        vm.onEvent(ChatEvent.SelectModel(model))
        advanceUntilIdle()
        assertEquals(1, chat.switched.size)
        assertEquals("high", vm.state.value.model?.variant)

        vm.onEvent(ChatEvent.SelectModel(ModelRef("sonnet", "anthropic", variant = "low")))
        advanceUntilIdle()
        assertEquals(ModelRef("sonnet", "anthropic"), chat.switched.last())
    }

    @Test
    fun `recent models come from recently updated sessions, current first`() = runTest(main.testDispatcher) {
        fun other(id: String, updated: Long, ref: ModelRef?) =
            Session(id, null, "p", id, "/other", "build", ref, 0.0, TokenUsage(), null, 0, updated, null)
        val opus = ModelRef("opus", "anthropic", "medium")
        val gpt = ModelRef("gpt", "openai")
        val sessions = FakeSessions(model, listOf(other("s2", 5, gpt), other("s3", 9, opus), other("s4", 8, null), other("s5", 1, opus)))
        val vm = vm(sessions)
        advanceUntilIdle()

        assertEquals(listOf(model.key, opus.key, gpt.key), vm.state.value.recentModels.map { it.key })
    }

    @Test
    fun `immediately, a model switch failure rolls back`() = runTest(main.testDispatcher) {
        settings.settings.value = AppSettings(selectionApply = SelectionApply.Immediately)
        chat.switchResult = Result.failure(OpenCodeError.Server(500, null, "no"))
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(ChatEvent.SelectModel(ModelRef("other", "openai")))
        assertEquals("other", vm.state.value.model?.id)
        advanceUntilIdle()
        assertEquals("haiku", vm.state.value.model?.id)
    }

    @Test
    fun `picked agent, model and variant stay local until the next prompt applies them in order`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        val sonnet = ModelRef("sonnet", "anthropic")

        vm.onEvent(ChatEvent.SelectAgent("build"))
        vm.onEvent(ChatEvent.SelectModel(sonnet))
        vm.onEvent(ChatEvent.SelectVariant("high"))
        advanceUntilIdle()
        assertTrue(chat.calls.isEmpty())
        assertEquals("build", vm.state.value.agent)
        assertEquals(sonnet.copy(variant = "high"), vm.state.value.model)
        // Picking does not reorder the recent models; sending does.
        assertEquals(model.key, vm.state.value.recentModels.first().key)

        vm.onEvent(ChatEvent.DraftChanged("go"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        assertEquals(listOf("agent:build", "model:anthropic/sonnet/high", "prompt:go"), chat.calls)
        assertEquals(listOf(sonnet.copy(variant = "high")), recents.models)
        assertEquals(sonnet.key, vm.state.value.recentModels.first().key)

        // Applied: the next prompt switches nothing.
        vm.onEvent(ChatEvent.DraftChanged("again"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        assertEquals("prompt:again", chat.calls.last())
        assertEquals(4, chat.calls.size)
    }

    @Test
    fun `picking the session's own agent and model again leaves nothing to apply`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(ChatEvent.SelectAgent("build"))
        vm.onEvent(ChatEvent.SelectAgent("plan"))
        vm.onEvent(ChatEvent.SelectModel(ModelRef("sonnet", "anthropic")))
        vm.onEvent(ChatEvent.SelectModel(model))
        vm.onEvent(ChatEvent.DraftChanged("go"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        assertEquals(listOf("prompt:go"), chat.calls)
    }

    @Test
    fun `server events do not overwrite a pending selection`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        val sonnet = ModelRef("sonnet", "anthropic")
        vm.onEvent(ChatEvent.SelectAgent("build"))
        vm.onEvent(ChatEvent.SelectModel(sonnet))

        events.flow.emit(ServerEvent.StepStarted("s1", "a9", "plan", ModelRef("opus", "anthropic"), 1))
        events.flow.emit(ServerEvent.SessionChanged("s1", deleted = false))
        advanceUntilIdle()
        assertEquals("build", vm.state.value.agent)
        assertEquals(sonnet, vm.state.value.model)
    }

    @Test
    fun `a failed switch keeps the prompt and the selection for the next try`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        val sonnet = ModelRef("sonnet", "anthropic")
        vm.onEvent(ChatEvent.SelectAgent("build"))
        vm.onEvent(ChatEvent.SelectModel(sonnet))
        chat.agentResult = Result.success(Unit)
        chat.switchResult = Result.failure(OpenCodeError.Server(500, null, "no"))
        vm.onEvent(ChatEvent.DraftChanged("go"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        vm.effect.first { it is ChatEffect.ShowMessage }
        assertEquals("go", vm.state.value.draft)
        assertEquals("build", vm.state.value.agent)
        assertEquals(sonnet, vm.state.value.model)
        assertTrue(chat.prompts.isEmpty())
        assertTrue(recents.models.isEmpty())

        chat.switchResult = Result.success(Unit)
        chat.agentResult = Result.failure(OpenCodeError.Server(500, null, "unexpected"))
        chat.calls.clear()
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        // The agent was switched by the first try: only the model is left.
        assertEquals(listOf("model:anthropic/sonnet/null", "prompt:go"), chat.calls)
    }

    @Test
    fun `shell commands and queued prompts keep the selection pending, the next steered prompt applies it`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        val sonnet = ModelRef("sonnet", "anthropic")
        vm.onEvent(ChatEvent.SelectAgent("build"))
        vm.onEvent(ChatEvent.SelectModel(sonnet))
        vm.onEvent(ChatEvent.DraftChanged("!ls"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        assertEquals(listOf("shell:ls"), chat.calls)

        vm.onEvent(ChatEvent.DeliveryChanged(PromptDelivery.Queue))
        vm.onEvent(ChatEvent.DraftChanged("later"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        vm.onEvent(ChatEvent.DraftChanged("/review later"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        assertEquals(listOf("shell:ls", "prompt:later", "command:review"), chat.calls)
        assertEquals("build", vm.state.value.agent)
        assertEquals(sonnet, vm.state.value.model)

        vm.onEvent(ChatEvent.DeliveryChanged(PromptDelivery.Steer))
        vm.onEvent(ChatEvent.DraftChanged("/review now"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        assertEquals(listOf("agent:build", "model:anthropic/sonnet/null", "command:review"), chat.calls.drop(3))
    }

    @Test
    fun `a queued prompt still commits a staged revert without applying the selection`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(ChatEvent.Revert("u1"))
        advanceUntilIdle()
        vm.onEvent(ChatEvent.SelectAgent("build"))
        vm.onEvent(ChatEvent.DeliveryChanged(PromptDelivery.Queue))
        vm.onEvent(ChatEvent.DraftChanged("instead"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        assertEquals(listOf("commit", "prompt:instead"), chat.calls)
        assertEquals("build", vm.state.value.agent)
    }

    @Test
    fun `a staged revert is committed between the agent and the model switch`() = runTest(main.testDispatcher) {
        chat.serverPrompts = listOf(Message.User("u1", 0, "hello"))
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(ChatEvent.Revert("u1"))
        advanceUntilIdle()
        vm.onEvent(ChatEvent.SelectAgent("build"))
        vm.onEvent(ChatEvent.SelectModel(ModelRef("sonnet", "anthropic")))
        vm.onEvent(ChatEvent.DraftChanged("instead"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        assertEquals(listOf("agent:build", "commit", "model:anthropic/sonnet/null", "prompt:instead"), chat.calls)
    }

    @Test
    fun `an agent with a configured model brings it unless a model was picked`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(ChatEvent.SelectAgent("review"))
        assertEquals(ModelRef("sonnet", "anthropic", "high"), vm.state.value.model)
        vm.onEvent(ChatEvent.SelectAgent("plan"))
        assertEquals(model, vm.state.value.model)

        val picked = ModelRef("haiku", "anthropic", "low")
        vm.onEvent(ChatEvent.SelectVariant("low"))
        vm.onEvent(ChatEvent.SelectAgent("review"))
        assertEquals(picked, vm.state.value.model)
    }

    @Test
    fun `leaving without sending shows the session's real selection next time`() = runTest(main.testDispatcher) {
        val first = vm()
        advanceUntilIdle()
        first.onEvent(ChatEvent.SelectAgent("build"))
        first.onEvent(ChatEvent.SelectModel(ModelRef("sonnet", "anthropic")))

        val reopened = vm()
        advanceUntilIdle()
        assertEquals("plan", reopened.state.value.agent)
        assertEquals(model, reopened.state.value.model)
        assertTrue(chat.calls.isEmpty())
    }

    @Test
    fun `a pick made after switching to immediately replaces a pending one`() = runTest(main.testDispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(ChatEvent.SelectAgent("build"))
        settings.settings.value = AppSettings(selectionApply = SelectionApply.Immediately)
        advanceUntilIdle()
        vm.onEvent(ChatEvent.SelectAgent("plan"))
        advanceUntilIdle()
        assertEquals("plan", vm.state.value.agent)
        vm.onEvent(ChatEvent.DraftChanged("go"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        assertEquals(listOf("agent:plan", "prompt:go"), chat.calls)
    }

    @Test
    fun `immediately, picking an agent switches the session at once`() = runTest(main.testDispatcher) {
        settings.settings.value = AppSettings(selectionApply = SelectionApply.Immediately)
        val vm = vm()
        advanceUntilIdle()
        vm.onEvent(ChatEvent.SelectAgent("build"))
        advanceUntilIdle()
        assertEquals(listOf("agent:build"), chat.calls)
        vm.onEvent(ChatEvent.DraftChanged("go"))
        vm.onEvent(ChatEvent.Send)
        advanceUntilIdle()
        assertEquals(listOf("agent:build", "prompt:go"), chat.calls)
    }
}

private class FakeSessions(
    private val model: ModelRef,
    private val others: List<Session> = emptyList(),
    private val order: List<String> = emptyList(),
    private val titles: Map<String, String> = emptyMap(),
) : SessionRepository {
    private val session = Session("s1", null, "p", "Title", "/repo", "plan", model, 0.0, TokenUsage(), null, 0, 0, null)
    override suspend fun list(search: String?, directory: String?, limit: Int) = Result.success(listOf(session) + others)
    var activeIds: Set<String> = emptySet()
    override suspend fun active(): Result<Set<String>> = Result.success(activeIds)
    var uncached = false
    var loaded: CompletableDeferred<Unit>? = null
    override suspend fun get(sessionId: String): Result<Session> {
        loaded?.await()
        return Result.success(session)
    }
    override fun cached(sessionId: String) = if (uncached) null else titles[sessionId]?.let { session.copy(id = sessionId, title = it) } ?: session
    override fun browseOrder() = order
    override suspend fun create(directory: String, agent: String?, model: ModelRef?) = Result.success(session)
    override suspend fun rename(sessionId: String, title: String) = Result.success(Unit)
    override suspend fun delete(sessionId: String) = Result.success(Unit)
    val forks = mutableListOf<Pair<String, String?>>()
    var forkResult: Result<Session> = Result.success(session.copy(id = "s2", title = "Title (fork #1)"))
    override suspend fun fork(sessionId: String, beforeMessageId: String?): Result<Session> {
        forks += sessionId to beforeMessageId
        return forkResult
    }
    override suspend fun projects(): Result<List<Project>> = Result.success(emptyList())
}

private class FakeChat(var transcript: List<Message>) : ChatRepository {
    var promptResult: Result<Unit> = Result.success(Unit)
    var switchResult: Result<Unit> = Result.success(Unit)
    val prompts = mutableListOf<Pair<String, PromptDelivery>>()
    val replies = mutableListOf<Pair<String, PermissionDecision>>()
    private var permissions = listOf(PermissionRequest("p1", "s1", "bash", listOf("ls"), null, true))

    var cached: List<Message> = emptyList()
    var messagesResult: Result<Unit> = Result.success(Unit)

    /** When set, the server sync waits for it. */
    var gate: CompletableDeferred<Unit>? = null

    /** Pages returned by [olderMessages], newest page first. */
    val olderPages = ArrayDeque<List<Message>>()

    override suspend fun cachedMessages(sessionId: String) = cached

    /** When set, returned by [olderMessages] instead of the next page. */
    var olderResult: Result<List<Message>?>? = null
    override suspend fun olderMessages(sessionId: String): Result<List<Message>?> =
        olderResult ?: Result.success(olderPages.removeFirstOrNull())
    var images: Result<List<String?>> = Result.success(emptyList())
    val imageRequests = mutableListOf<String>()
    override suspend fun messageImages(sessionId: String, messageId: String): Result<List<String?>> {
        imageRequests += messageId
        return images
    }
    val prefetched = mutableListOf<Pair<List<String>, Int>>()
    override suspend fun prefetch(sessionIds: List<String>, lastMessages: Int) {
        prefetched += sessionIds to lastMessages
    }
    override suspend fun messages(sessionId: String): Result<List<Message>> {
        gate?.await()
        return messagesResult.map { transcript }
    }
    override suspend fun prompt(
        sessionId: String,
        text: String,
        delivery: PromptDelivery,
        attachments: List<PromptAttachment>,
    ): Result<Unit> {
        prompts += text to delivery
        calls += "prompt:$text"
        sentAttachments += attachments
        return promptResult
    }

    val commands = mutableListOf<Pair<String, String>>()
    val sentAttachments = mutableListOf<PromptAttachment>()
    val reverts = mutableListOf<String>()
    var forms: List<Form> = emptyList()
    val formReplies = mutableListOf<Pair<String, Map<String, FormAnswer>>>()

    override suspend fun command(
        sessionId: String,
        name: String,
        arguments: String,
        delivery: PromptDelivery,
        attachments: List<PromptAttachment>,
    ): Result<Unit> {
        commands += name to arguments
        calls += "command:$name"
        return Result.success(Unit)
    }
    override suspend fun forms(sessionId: String) = Result.success(forms)
    override suspend fun replyForm(sessionId: String, formId: String, answers: Map<String, FormAnswer>): Result<Unit> {
        formReplies += formId to answers
        forms = forms.filterNot { it.id == formId }
        return Result.success(Unit)
    }

    /** All prompts on the server, newest first. */
    var serverPrompts: List<Message.User> = emptyList()
    override suspend fun previousPrompt(sessionId: String, beforeMessageId: String?): Result<Message.User?> =
        Result.success(serverPrompts.firstOrNull { beforeMessageId == null || it.id < beforeMessageId })
    override suspend fun revert(sessionId: String, messageId: String): Result<Unit> {
        reverts += messageId
        return Result.success(Unit)
    }
    var backgrounds = 0
    override suspend fun background(sessionId: String): Result<Unit> {
        backgrounds++
        return Result.success(Unit)
    }
    var commits = 0
    var commitResult: Result<Unit> = Result.success(Unit)
    override suspend fun commitRevert(sessionId: String): Result<Unit> {
        commits++
        calls += "commit"
        return commitResult
    }
    var inbox: List<QueuedPrompt> = emptyList()
    val cancelled = mutableListOf<String>()
    val steered = mutableListOf<String>()
    override suspend fun inbox(sessionId: String) = Result.success(inbox)
    override suspend fun cancelQueued(sessionId: String, inboxId: String): Result<Unit> {
        cancelled += inboxId
        inbox = inbox.filterNot { it.id == inboxId }
        return Result.success(Unit)
    }
    override suspend fun steerQueued(sessionId: String, inboxId: String): Result<Unit> {
        steered += inboxId
        inbox = inbox.map { if (it.id == inboxId) it.copy(delivery = PromptDelivery.Steer) else it }
        return Result.success(Unit)
    }
    val shells = mutableListOf<String>()
    var shellResult: Result<Unit> = Result.success(Unit)
    val shellPages = ArrayDeque<String>()
    val shellReads = mutableListOf<Pair<String, Long>>()
    override suspend fun shellOutput(directory: String, shellId: String, cursor: Long): Result<ShellOutput> {
        shellReads += shellId to cursor
        val text = shellPages.removeFirstOrNull().orEmpty()
        return Result.success(ShellOutput(text, cursor + text.length))
    }
    override suspend fun shell(sessionId: String, command: String): Result<Unit> {
        shells += command
        calls += "shell:$command"
        return shellResult
    }
    val changesByScope = mutableMapOf<ChangesScope, Result<List<FileDiff>>>()
    val changesRequests = mutableListOf<ChangesScope>()
    override suspend fun changes(sessionId: String, scope: ChangesScope): Result<List<FileDiff>> {
        changesRequests += scope
        return changesByScope[scope] ?: Result.success(emptyList())
    }
    override suspend fun interrupt(sessionId: String) = Result.success(Unit)

    /** Server requests in order: `agent:x`, `model:p/m/variant`, `prompt:text`, `shell:cmd`, `command:x`, `commit`. */
    val calls = mutableListOf<String>()
    var agentResult: Result<Unit>? = null
    override suspend fun switchAgent(sessionId: String, agent: String): Result<Unit> {
        calls += "agent:$agent"
        return agentResult ?: switchResult
    }
    val switched = mutableListOf<ModelRef>()
    override suspend fun switchModel(sessionId: String, model: ModelRef): Result<Unit> {
        switched += model
        calls += "model:${model.key}/${model.variant}"
        return switchResult
    }
    override suspend fun permissions(sessionId: String) = Result.success(permissions)
    override suspend fun replyPermission(sessionId: String, requestId: String, decision: PermissionDecision, message: String?): Result<Unit> {
        replies += requestId to decision
        permissions = permissions.filterNot { it.id == requestId }
        return Result.success(Unit)
    }
}

private class FakeCatalog(private val model: ModelRef) : CatalogRepository {
    override suspend fun agents(directory: String?) = Result.success(
        listOf(
            Agent("build", "build", null, AgentMode.Primary, false, null),
            Agent("plan", "plan", null, AgentMode.All, false, null),
            Agent("explore", "explore", null, AgentMode.Subagent, false, null),
            Agent("review", "review", null, AgentMode.Primary, false, null, model = ModelRef("sonnet", "anthropic", "high")),
        ),
    )
    override suspend fun models(directory: String?) = Result.success(
        listOf(
            ModelInfo(model, "Claude Haiku", "anthropic", null, 200_000, true, emptyList(), 0, true, "active", "Anthropic"),
            ModelInfo(ModelRef("sonnet", "anthropic"), "Claude Sonnet", "anthropic", null, 200_000, true, listOf("high"), 0, true, "active", "Anthropic"),
        ),
    )
    override suspend fun commands(directory: String?) = Result.success(listOf(Command("review", "Review changes")))
    override suspend fun findFiles(directory: String, query: String) =
        Result.success(listOf("src/Composer.kt", "src/Chat.kt").filter { query.lowercase() in it.lowercase() })
}

private class FakeEvents : EventRepository {
    override val connection: StateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Connected)
    val flow = MutableSharedFlow<ServerEvent>(extraBufferCapacity = 64)
    override val events = flow
}

private class FakeMcps : McpRepository {
    var servers = listOf(McpServer("test", McpStatus.Connected))
    val directories = mutableListOf<String>()
    val changes = mutableListOf<Triple<String, String, Boolean>>()
    var listFailure: OpenCodeError? = null
    var changeFailure: OpenCodeError? = null
    var gate: CompletableDeferred<Unit>? = null
    override suspend fun list(directory: String): Result<List<McpServer>> {
        directories += directory
        return listFailure?.let { Result.failure(it) } ?: Result.success(servers)
    }
    override suspend fun setConnected(directory: String, name: String, connected: Boolean): Result<Unit> {
        changes += Triple(directory, name, connected)
        gate?.await()
        changeFailure?.let { return Result.failure(it) }
        servers = servers.map { if (it.name == name) it.copy(status = if (connected) McpStatus.Connected else McpStatus.Disabled) else it }
        return Result.success(Unit)
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

private class FakeVcs : VcsRepository {
    var branch: VcsBranch? = VcsBranch("main", "main")
    var files = listOf("a.kt", "b.kt")
    val branchRequests = mutableListOf<String>()
    var statusRequests = 0
    override suspend fun branch(directory: String): Result<VcsBranch?> {
        branchRequests += directory
        return Result.success(branch)
    }
    override suspend fun changedFiles(directory: String): Result<List<String>> {
        statusRequests++
        return Result.success(files)
    }
    var diffs: List<FileDiff> = emptyList()
    val diffRequests = mutableListOf<String>()
    override suspend fun workingChanges(directory: String): Result<List<FileDiff>> {
        diffRequests += directory
        return Result.success(diffs)
    }
}

private class FakeDrafts : DraftRepository {
    private val drafts = mutableMapOf<String, String>()
    override fun load(sessionId: String) = drafts[sessionId].orEmpty()
    override fun save(sessionId: String, text: String) {
        if (text.isBlank()) drafts -= sessionId else drafts[sessionId] = text
    }
}

private class FakeSettings : SettingsRepository {
    override val settings = MutableStateFlow(AppSettings())
    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        settings.value = transform(settings.value)
    }
}
