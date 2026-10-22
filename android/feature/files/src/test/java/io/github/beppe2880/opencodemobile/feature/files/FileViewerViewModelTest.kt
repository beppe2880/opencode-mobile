package io.github.beppe2880.opencodemobile.feature.files

import androidx.compose.ui.text.AnnotatedString
import io.github.beppe2880.opencodemobile.core.domain.FileContent
import io.github.beppe2880.opencodemobile.core.domain.FileRepository
import io.github.beppe2880.opencodemobile.core.domain.OpenCodeError
import io.github.beppe2880.opencodemobile.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class FileViewerViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val files = FakeFiles()
    private val text = FileContent.Text("one\ntwo\n", 8, truncated = false)

    private fun vm() = FileViewerViewModel(FileTarget("/home/me/app", "/home/me/app/src/Main.kt"), files)

    @Test
    fun `opens the file and shows its path inside the session folder`() = runTest(main.testDispatcher) {
        files.result = Result.success(text)
        val vm = vm()
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(text, state.content)
        assertFalse(state.loading)
        assertEquals("Main.kt", state.name)
        assertEquals("src/Main.kt", state.displayPath)
        assertEquals(listOf("/home/me/app" to "/home/me/app/src/Main.kt"), files.reads)
    }

    @Test
    fun `a failed first read shows an error that retry clears`() = runTest(main.testDispatcher) {
        files.result = Result.failure(OpenCodeError.NotFound("File not found"))
        val vm = vm()
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)

        files.result = Result.success(text)
        vm.onEvent(FileViewerEvent.Reload)
        advanceUntilIdle()

        assertNull(vm.state.value.error)
        assertEquals(text, vm.state.value.content)
    }

    @Test
    fun `a failed reload keeps the file and reports it once`() = runTest(main.testDispatcher) {
        files.result = Result.success(text)
        val vm = vm()
        advanceUntilIdle()

        files.result = Result.failure(OpenCodeError.Unreachable("down"))
        vm.onEvent(FileViewerEvent.Reload)
        advanceUntilIdle()

        assertEquals(text, vm.state.value.content)
        assertNull(vm.state.value.error)
        assertEquals(true, vm.effect.first() is FileViewerEffect.ShowMessage)
    }

    @Test
    fun `wrapping is on by default and toggles`() = runTest(main.testDispatcher) {
        val vm = vm()
        vm.onEvent(FileViewerEvent.ToggleWrap)
        assertFalse(vm.state.value.wrap)
    }

    @Test
    fun `lines are split without their breaks and a final newline adds none`() {
        assertEquals(listOf("a", "b"), splitLines(AnnotatedString("a\r\nb\n")).map { it.text })
        assertEquals(listOf("a", "", "b"), splitLines(AnnotatedString("a\n\nb")).map { it.text })
        assertEquals(listOf("only"), splitLines(AnnotatedString("only")).map { it.text })
    }
}

private class FakeFiles : FileRepository {
    var result: Result<FileContent> = Result.success(FileContent.Text("", 0, truncated = false))
    val reads = mutableListOf<Pair<String, String>>()

    override suspend fun read(directory: String, path: String): Result<FileContent> {
        reads += directory to path
        return result
    }
}
