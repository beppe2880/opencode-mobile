package io.github.beppe2880.opencodemobile.feature.settings.customize

import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.ChatElement
import io.github.beppe2880.opencodemobile.core.domain.ChatHeaderLine
import io.github.beppe2880.opencodemobile.core.domain.SettingsRepository
import io.github.beppe2880.opencodemobile.core.testing.MainDispatcherRule
import io.github.beppe2880.opencodemobile.feature.settings.SettingsEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CustomizeViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val settings = object : SettingsRepository {
        override val settings = MutableStateFlow(AppSettings(showCost = false))
        override suspend fun update(transform: (AppSettings) -> AppSettings) {
            settings.value = transform(settings.value)
        }
    }

    @Test
    fun `starts from the saved settings with the edit marks shown`() = runTest(main.testDispatcher) {
        val vm = CustomizeViewModel(settings)
        advanceUntilIdle()

        assertFalse(vm.state.value.settings.showCost)
        assertTrue(vm.state.value.marks)
        assertNull(vm.state.value.selected)
        assertTrue(vm.state.value.options.isEmpty())
    }

    @Test
    fun `tapping a part opens its options and closing hides them`() = runTest(main.testDispatcher) {
        val vm = CustomizeViewModel(settings)
        vm.onEvent(CustomizeEvent.Select(ChatElement.Composer))
        assertEquals(ChatElement.Composer, vm.state.value.selected)
        assertEquals(listOf(ChatOption.AgentOutline, ChatOption.Dictation), vm.state.value.options)

        vm.onEvent(CustomizeEvent.Select(ChatElement.Footer))
        assertEquals(listOf(ChatOption.ShowCost), vm.state.value.options)

        vm.onEvent(CustomizeEvent.CloseOptions)
        assertNull(vm.state.value.selected)
    }

    @Test
    fun `a change is saved and shows in the preview at once`() = runTest(main.testDispatcher) {
        val vm = CustomizeViewModel(settings)
        vm.onEvent(CustomizeEvent.Select(ChatElement.Footer))
        vm.onEvent(CustomizeEvent.Setting(SettingsEvent.ShowCostChanged(true)))
        vm.onEvent(CustomizeEvent.Setting(SettingsEvent.AgentColorOutlineChanged(false)))
        advanceUntilIdle()

        assertTrue(settings.settings.value.showCost)
        assertFalse(settings.settings.value.agentColorOutline)
        assertTrue(vm.state.value.settings.showCost)
        // The options stay open for the next change.
        assertEquals(ChatElement.Footer, vm.state.value.selected)
    }

    @Test
    fun `header options follow the header line`() = runTest(main.testDispatcher) {
        val vm = CustomizeViewModel(settings)
        vm.onEvent(CustomizeEvent.Select(ChatElement.Header))
        assertEquals(3, vm.state.value.options.size)

        vm.onEvent(CustomizeEvent.Setting(SettingsEvent.ChatHeaderLineChanged(ChatHeaderLine.Folder)))
        advanceUntilIdle()

        assertEquals(listOf(ChatOption.HeaderLine), vm.state.value.options)
    }

    @Test
    fun `events that are not a setting change nothing`() = runTest(main.testDispatcher) {
        val vm = CustomizeViewModel(settings)
        val before = settings.settings.value
        vm.onEvent(CustomizeEvent.Setting(SettingsEvent.OpenUsage))
        advanceUntilIdle()
        assertEquals(before, settings.settings.value)
    }

    @Test
    fun `marks can be hidden and back navigates`() = runTest(main.testDispatcher) {
        val vm = CustomizeViewModel(settings)
        vm.onEvent(CustomizeEvent.MarksChanged(false))
        assertFalse(vm.state.value.marks)

        vm.onEvent(CustomizeEvent.Back)
        assertEquals(CustomizeEffect.NavigateBack, vm.effect.first())
    }
}
