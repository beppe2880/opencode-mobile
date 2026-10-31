package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import io.github.beppe2880.opencodemobile.core.domain.PermissionDecision
import io.github.beppe2880.opencodemobile.core.domain.PermissionRequest
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** The permission card folds into a bar that still names the tool, and never replies by itself. */
class PermissionCardTest {
    @get:Rule val compose = createComposeRule()

    private val request = PermissionRequest("p1", "s1", "bash", listOf("ls -la"), null, canSave = true)
    private var collapsed by mutableStateOf(false)
    private val replies = mutableListOf<PermissionDecision>()

    private fun showCard() {
        compose.setContent {
            OcTheme {
                PermissionCard(
                    request = request,
                    pendingCount = 1,
                    replying = false,
                    collapsed = collapsed,
                    onCollapse = { collapsed = it },
                    onReply = { replies += it },
                )
            }
        }
    }

    @Test
    fun minimiseButtonFoldsToABarNamingTheToolAndTapExpands() {
        showCard()
        compose.onNodeWithTag("permission_collapse_button").performClick()

        compose.onNodeWithTag("permission_collapsed_bar", useUnmergedTree = false)
            .assertIsDisplayed()
            .assert(hasClickAction())
            .assert(hasText("bash", substring = true))
        compose.onNodeWithTag("permission_allow_once_button").assertDoesNotExist()

        compose.onNodeWithTag("permission_collapsed_bar").performClick()
        compose.onNodeWithTag("permission_allow_once_button").assertIsDisplayed()
        assertTrue(replies.isEmpty())
    }

    @Test
    fun swipeDownOnTheHeaderFoldsAndSwipeUpOnTheBarExpands() {
        showCard()
        compose.onNodeWithTag("permission_header").performTouchInput { swipeDown() }
        compose.onNodeWithTag("permission_collapsed_bar").assertIsDisplayed()

        compose.onNodeWithTag("permission_collapsed_bar").performTouchInput { swipeUp() }
        compose.onNodeWithTag("permission_reject_button").assertIsDisplayed()
        assertTrue(replies.isEmpty())
    }
}
