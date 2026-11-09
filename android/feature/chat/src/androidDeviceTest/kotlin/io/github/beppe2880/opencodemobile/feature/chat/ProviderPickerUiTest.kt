package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import io.github.beppe2880.opencodemobile.core.domain.KeyIntegration
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProviderPickerUiTest {
    @get:Rule val compose = createComposeRule()
    private var state by mutableStateOf(
        ProviderConnectionState(
            serverName = "Test server",
            integrations = listOf(
                KeyIntegration("openai", "OpenAI", true, null, emptyList()),
                KeyIntegration("anthropic", "Anthropic", false, null, emptyList()),
                KeyIntegration("custom", "Custom", false, null, emptyList()),
            ),
        ),
    )
    private val events = mutableListOf<ProviderConnectionEvent>()
    private var closes = 0

    private fun showPicker() {
        compose.setContent {
            OcTheme {
                ProviderPicker(state, {
                    events += it
                    if (it is ProviderConnectionEvent.Search) state = state.copy(query = it.query)
                }, { closes++ })
            }
        }
    }

    @Test
    fun connectedProviderIsSelectableAndCloseDismisses() {
        showPicker()
        compose.onNodeWithTag("provider_group_connected").assertIsDisplayed()
        compose.onNodeWithTag("provider_select_openai").assert(hasClickAction()).performClick()
        assertEquals(listOf(ProviderConnectionEvent.Select("openai")), events)
        compose.onNodeWithTag("provider_picker_close").performClick()
        assertEquals(1, closes)
    }

    @Test
    fun searchFiltersEveryGroupAndOmitsEmptySections() {
        showPicker()
        compose.onNodeWithTag("provider_search").performTextInput("anthropic")
        compose.onNodeWithTag("provider_select_anthropic").assertIsDisplayed()
        compose.onNodeWithTag("provider_select_openai").assertDoesNotExist()
        compose.onNodeWithTag("provider_group_connected").assertDoesNotExist()
        compose.onNodeWithTag("provider_group_other").assertDoesNotExist()
        compose.onNodeWithTag("provider_select_anthropic").performClick()
        assertEquals(ProviderConnectionEvent.Select("anthropic"), events.last())
    }

    @Test
    fun searchAndCloseStayVisibleWhenListScrolls() {
        state = state.copy(integrations = state.integrations + (1..30).map { KeyIntegration("custom-$it", "Other $it", false, null, emptyList()) })
        showPicker()
        compose.onNodeWithTag("provider_picker_list").performScrollToNode(hasTestTag("provider_select_custom-9"))
        compose.onNodeWithTag("provider_search").assertIsDisplayed()
        compose.onNodeWithTag("provider_picker_close").assertIsDisplayed()
        compose.onNodeWithTag("provider_search").performTextInput("openai")
        compose.onNodeWithTag("provider_select_openai").assertIsDisplayed()
    }

    @Test
    fun failedLoadOffersRetryWithoutInventingProviders() {
        state = state.copy(integrations = emptyList(), failed = true)
        showPicker()
        compose.onNodeWithTag("provider_connection_error").assertIsDisplayed()
        compose.onNodeWithTag("provider_group_popular").assertDoesNotExist()
        compose.onNodeWithTag("provider_retry").performClick()
        assertEquals(listOf(ProviderConnectionEvent.Retry), events)
    }
}
