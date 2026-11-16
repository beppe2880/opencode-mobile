package io.github.beppe2880.opencodemobile.feature.terminal

import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.TerminalInfo
import io.github.beppe2880.opencodemobile.core.domain.TerminalLink
import io.github.beppe2880.opencodemobile.core.domain.TerminalRepository
import io.github.beppe2880.opencodemobile.core.domain.TerminalUpdate
import io.github.beppe2880.opencodemobile.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TerminalViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    @Test
    fun `terminal output pauses while the terminal is hidden and resumes when shown`() = runTest(main.testDispatcher) {
        val terminals = FakeTerminals()
        val viewModel = TerminalViewModel("/project", terminals, FakeSettings())
        val page = RecordingPage()
        viewModel.surface.attach(page)

        runCurrent()
        viewModel.setTerminalVisible(false)
        runCurrent()
        viewModel.setTerminalVisible(true)
        advanceUntilIdle()

        // WebSurface writes to the page as JavaScript literals, so each chunk arrives quoted.
        assertEquals(listOf("\"hello\"", "\"again\""), page.writes)
        assertEquals(TerminalLink.Connected, viewModel.state.value.link)
        assertEquals(1, terminals.openCalls)
    }

    @Test
    fun `server exit followed by pause and resume does not open a new shell`() = runTest(main.testDispatcher) {
        val terminals = FakeTerminals(exits = true)
        val viewModel = TerminalViewModel("/project", terminals, FakeSettings())
        runCurrent()
        assertEquals(TerminalLink.Exited, viewModel.state.value.link)
        viewModel.setTerminalVisible(false)
        runCurrent()
        viewModel.setTerminalVisible(true)
        runCurrent()
        assertEquals(1, terminals.openCalls)
        assertEquals(TerminalLink.Exited, viewModel.state.value.link)
    }

    @Test
    fun `new terminal does not resize to the old shell dimensions before replay`() = runTest(main.testDispatcher) {
        val terminals = FakeTerminals()
        val viewModel = TerminalViewModel("/project", terminals, FakeSettings())
        runCurrent()
        viewModel.surface.onPageResize(50, 12)
        advanceUntilIdle()
        assertEquals(listOf(Triple("t1", 50, 12)), terminals.resizes)

        viewModel.onEvent(TerminalEvent.Restart)
        advanceUntilIdle()
        assertEquals(2, terminals.openCalls)
        assertEquals(listOf(Triple("t1", 50, 12)), terminals.resizes)

        // The new page signals its fitted size only after parsing the replay.
        viewModel.surface.onPageResize(50, 12)
        advanceUntilIdle()
        assertEquals(listOf(Triple("t1", 50, 12), Triple("t2", 50, 12)), terminals.resizes)
    }

    private class FakeSettings : SettingsRepository {
        override val settings = MutableStateFlow(AppSettings())
        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            settings.value = transform(settings.value)
        }
    }

    private class FakeTerminals(private val exits: Boolean = false) : TerminalRepository {
        var openCalls = 0
        val resizes = mutableListOf<Triple<String, Int, Int>>()
        override suspend fun open(directory: String): Result<TerminalInfo> {
            openCalls++
            return Result.success(TerminalInfo("t$openCalls", "bash", true))
        }

        override suspend fun resize(directory: String, terminalId: String, columns: Int, rows: Int): Result<Unit> {
            resizes += Triple(terminalId, columns, rows)
            return Result.success(Unit)
        }

        override suspend fun close(directory: String, terminalId: String) = Result.success(Unit)

        override fun attach(
            directory: String,
            terminalId: String,
            input: Flow<ByteArray>,
            visible: Flow<Boolean>,
        ) = flow {
            visible.first { it }
            emit(TerminalUpdate.Link(TerminalLink.Connected))
            emit(TerminalUpdate.Output("hello"))
            if (exits) {
                emit(TerminalUpdate.Link(TerminalLink.Exited))
                return@flow
            }
            visible.first { !it }
            visible.first { it }
            emit(TerminalUpdate.Output("again"))
        }
    }

    private class RecordingPage : WebTerminalPage {
        val writes = mutableListOf<String>()

        override fun call(function: String, vararg arguments: String) {
            if (function == "write") writes += arguments.first()
        }
    }
}
