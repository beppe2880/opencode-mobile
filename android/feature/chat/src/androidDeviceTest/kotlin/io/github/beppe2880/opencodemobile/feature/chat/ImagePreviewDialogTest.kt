package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** The viewer turns pages with one finger, except while the image on screen is zoomed in. */
class ImagePreviewDialogTest {
    @get:Rule val compose = createComposeRule()

    private val pages = mutableListOf<Int>()

    private fun showViewer(index: Int = 0) {
        val images = listOf(RED, GREEN, BLUE).mapIndexed { i, png -> PreviewImage("image$i.png", "data:image/png;base64,$png") }
        compose.setContent {
            OcTheme {
                ImagePreviewDialog(ImagePreview(images, index), onPage = { pages += it }, onDismiss = {})
            }
        }
    }

    @Test
    fun swipingTurnsThePagesAndReportsThem() {
        showViewer(index = 1)
        compose.onNodeWithTag("chat_image_preview_position").assertTextEquals("2 / 3")

        compose.onNodeWithTag("chat_image_preview_pager").performTouchInput { swipeLeft() }
        compose.onNodeWithTag("chat_image_preview_position").assertTextEquals("3 / 3")

        compose.onNodeWithTag("chat_image_preview_pager").performTouchInput { swipeRight() }
        compose.onNodeWithTag("chat_image_preview_pager").performTouchInput { swipeRight() }
        compose.onNodeWithTag("chat_image_preview_position").assertTextEquals("1 / 3")
        compose.waitForIdle()
        assertEquals(listOf(1, 2, 1, 0), pages)
    }

    @Test
    fun aZoomedImagePansInsteadOfTurningThePage() {
        showViewer()
        // Images decode off the main thread; zoom acts on the decoded image.
        compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("image0.png").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("chat_image_preview_page_0").performTouchInput { doubleClick() }
        compose.onNodeWithTag("chat_image_preview_pager").performTouchInput { swipeLeft() }
        compose.onNodeWithTag("chat_image_preview_position").assertTextEquals("1 / 3")

        // Back at full size, the same swipe turns the page.
        compose.onNodeWithTag("chat_image_preview_page_0").performTouchInput { doubleClick() }
        compose.onNodeWithTag("chat_image_preview_pager").performTouchInput { swipeLeft() }
        compose.onNodeWithTag("chat_image_preview_position").assertTextEquals("2 / 3")
    }

    private companion object {
        const val RED = "iVBORw0KGgoAAAANSUhEUgAAAAgAAAAGCAIAAABxZ0isAAAAEklEQVR4nGP4z8CAFWEXpZMEADMoL9HswQX5AAAAAElFTkSuQmCC"
        const val GREEN = "iVBORw0KGgoAAAANSUhEUgAAAAgAAAAGCAIAAABxZ0isAAAAEElEQVR4nGNgaGDAjgZSAgAI/xgBDmQN5QAAAABJRU5ErkJggg=="
        const val BLUE = "iVBORw0KGgoAAAANSUhEUgAAAAgAAAAGCAIAAABxZ0isAAAAEElEQVR4nGNgYPiPAw2gBADTeS/R6fwvzgAAAABJRU5ErkJggg=="
    }
}
