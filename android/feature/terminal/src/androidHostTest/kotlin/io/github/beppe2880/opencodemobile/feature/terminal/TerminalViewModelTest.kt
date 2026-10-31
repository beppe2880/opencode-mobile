package io.github.beppe2880.opencodemobile.feature.terminal

import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.domain.TerminalInfo
import io.github.beppe2880.opencodemobile.core.domain.TerminalLink
import io.github.beppe2880.opencodemobile.core.domain.TerminalRepository
import io.github.beppe2880.opencodemobile.core.domain.TerminalUpdate
import io.github.beppe2880.opencodemobile.core.testing.MainDispatcherRule
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
    }

    private class FakeSettings : SettingsRepository {
        override val settings = MutableStateFlow(AppSettings())
        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            settings.value = transform(settings.value)
        }
    }

    private class FakeTerminals : TerminalRepository {
        override suspend fun open(directory: String) = Result.success(TerminalInfo("t1", "bash", true))

        override suspend fun resize(directory: String, terminalId: String, columns: Int, rows: Int) = Result.success(Unit)

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
