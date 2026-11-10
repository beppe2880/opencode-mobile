package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import io.github.beppe2880.opencodemobile.core.domain.ImageAttachment
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import org.junit.Rule
import org.junit.Test

/** A prompt being sent shows its images and a pending sign at once; the sign goes once it is delivered. */
class UserBubblePendingTest {
    @get:Rule val compose = createComposeRule()

    private val image = ImageAttachment("dot.png", "image/png", "data:image/png;base64,$PNG")
    private var item by mutableStateOf(ChatItem.User("local-0", "", pending = true, attachments = 1, images = listOf(image), sending = true))

    @Test
    fun aPromptOfImagesAloneShowsItsThumbnailAndThePendingSignUntilDelivered() {
        compose.setContent { OcTheme { UserBubble(item) } }

        compose.onNodeWithTag("chat_user_image_local-0_0").assertIsDisplayed()
        compose.onNodeWithTag("chat_user_pending_local-0").assertIsDisplayed()

        item = ChatItem.User("msg_1", "", pending = true, attachments = 1, images = listOf(image))
        compose.onNodeWithTag("chat_user_pending_msg_1").assertIsDisplayed()

        item = item.copy(pending = false)
        compose.onNodeWithTag("chat_user_image_msg_1_0").assertIsDisplayed()
        compose.onNodeWithTag("chat_user_pending_msg_1").assertDoesNotExist()
    }

    private companion object {
        /** A 1x1 red PNG. */
        const val PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg=="
    }
}
