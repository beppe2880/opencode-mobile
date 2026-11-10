package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import io.github.beppe2880.opencodemobile.core.ui.sample.SampleData
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ModelPickerUiTest {
    @get:Rule val compose = createComposeRule()
    private val events = mutableListOf<ChatEvent>()

    private fun showPicker(autoClose: Boolean) {
        compose.setContent {
            OcTheme {
                ChatScreen(
                    state = ChatState(
                        loading = false,
                        modelSheetOpen = true,
                        closeModelPickerOnSelect = autoClose,
                        models = SampleData.models,
                        model = SampleData.gpt,
                    ),
                    onEvent = { events += it },
                    onCopy = {},
                )
            }
        }
    }

    @Test
    fun manualModeOffersDoneAndSeparateReasoningSelection() {
        showPicker(autoClose = false)
        compose.onNodeWithTag("model_picker_done_button").assertIsDisplayed()
        compose.onNodeWithTag("model_variant_medium").performClick()
        assertEquals(listOf(ChatEvent.SelectVariant("medium")), events)
        compose.onNodeWithTag("model_picker_done_button").performClick()
        assertEquals(ChatEvent.ModelSheet(false), events.last())
    }

    @Test
    fun automaticModeDoesNotAddAConfirmationStep() {
        showPicker(autoClose = true)
        compose.onNodeWithTag("chat_model_sheet").assertIsDisplayed()
        compose.onNodeWithTag("model_picker_done_button").assertDoesNotExist()
        compose.onNodeWithTag("model_option_${SampleData.sonnet.key}").performClick()
        assertEquals(listOf(ChatEvent.SelectModel(SampleData.sonnet)), events)
    }
}
