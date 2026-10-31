package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import io.github.beppe2880.opencodemobile.core.domain.Form
import io.github.beppe2880.opencodemobile.core.domain.FormAnswer
import io.github.beppe2880.opencodemobile.core.domain.FormField
import io.github.beppe2880.opencodemobile.core.domain.FormOption
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** The question card folds into a bar and back, by button, tap and swipe, keeping the answers. */
class FormCardTest {
    @get:Rule val compose = createComposeRule()

    private val form = Form(
        id = "f1",
        sessionId = "s1",
        title = "Which storage should the cache use?",
        fields = listOf(FormField.Text("choice", required = true, options = listOf(FormOption("room", "Room"), FormOption("file", "File")))),
    )
    private var collapsed by mutableStateOf(false)
    private var answers by mutableStateOf<Map<String, FormAnswer>>(emptyMap())
    private var submits = 0
    private var dismisses = 0

    private fun showCard() {
        compose.setContent {
            OcTheme {
                FormCard(
                    form = form,
                    answers = answers,
                    pendingCount = 1,
                    submitting = false,
                    collapsed = collapsed,
                    onCollapse = { collapsed = it },
                    onAnswer = { key, answer -> answers = if (answer == null) answers - key else answers + (key to answer) },
                    onSubmit = { submits++ },
                    onDismiss = { dismisses++ },
                )
            }
        }
    }

    @Test
    fun minimiseButtonFoldsAndTapExpandsKeepingTheAnswer() {
        showCard()
        compose.onNodeWithTag("form_option_choice_room").performClick()
        compose.onNodeWithTag("form_collapse_button").performClick()

        compose.onNodeWithTag("form_collapsed_bar").assertIsDisplayed()
        compose.onNodeWithText(form.title).assertIsDisplayed()
        compose.onNodeWithTag("form_submit_button").assertDoesNotExist()
        assertTrue(collapsed)

        compose.onNodeWithTag("form_collapsed_bar").assert(hasClickAction()).performClick()
        compose.onNodeWithTag("form_submit_button").assertIsDisplayed()
        assertEquals(mapOf("choice" to FormAnswer.Text("room")), answers)
        assertEquals(0, submits + dismisses)
    }

    @Test
    fun swipeDownOnTheHeaderFoldsAndSwipeUpOnTheBarExpands() {
        showCard()
        compose.onNodeWithTag("form_header").performTouchInput { swipeDown() }
        compose.onNodeWithTag("form_collapsed_bar").assertIsDisplayed()

        compose.onNodeWithTag("form_collapsed_bar").performTouchInput { swipeUp() }
        compose.onNodeWithTag("form_collapse_button").assertIsDisplayed()
        assertEquals(0, submits + dismisses)
    }
}
