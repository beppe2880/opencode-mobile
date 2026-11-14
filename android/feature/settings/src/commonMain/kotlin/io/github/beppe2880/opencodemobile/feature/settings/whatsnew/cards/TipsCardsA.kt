package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewSidebar
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.PhotoSizeSelectLarge
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.TabletAndroid
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewCarousel
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_attached_images_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_attached_images_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_attached_images_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_check_updates_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_check_updates_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_check_updates_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_hide_list_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_hide_list_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_hide_list_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_image_swipe_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_image_swipe_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_image_swipe_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_large_screens_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_large_screens_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_large_screens_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_pending_prompt_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_pending_prompt_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_pending_prompt_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_picker_stay_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_picker_stay_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_picker_stay_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_pin_sessions_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_pin_sessions_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_pin_sessions_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_swipe_list_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_swipe_list_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_swipe_list_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_swipe_sensitivity_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_swipe_sensitivity_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_a_swipe_sensitivity_title
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.FeatureCard

/** Feature cards of group A; see the toolkit in ../DemoKit.kt. */
internal val tipsCardsA: List<FeatureCard> = listOf(
    FeatureCard(
        id = "attached-images-large",
        version = "0.16.0",
        icon = Icons.Outlined.PhotoSizeSelectLarge,
        title = Res.string.tips_a_attached_images_title,
        body = Res.string.tips_a_attached_images_body,
        demoDescription = Res.string.tips_a_attached_images_demo,
        author = "beppe2880",
        pullRequest = 207,
        demo = { AttachedImagesSizeDemo(it) },
    ),
    FeatureCard(
        id = "pin-sessions",
        version = "0.16.0",
        icon = Icons.Outlined.PushPin,
        title = Res.string.tips_a_pin_sessions_title,
        body = Res.string.tips_a_pin_sessions_body,
        demoDescription = Res.string.tips_a_pin_sessions_demo,
        author = "beppe2880",
        pullRequest = 206,
        demo = { PinSessionsDemo(it) },
    ),
    FeatureCard(
        id = "swipe-sensitivity",
        version = "0.16.0",
        icon = Icons.Outlined.Tune,
        title = Res.string.tips_a_swipe_sensitivity_title,
        body = Res.string.tips_a_swipe_sensitivity_body,
        demoDescription = Res.string.tips_a_swipe_sensitivity_demo,
        author = "beppe2880",
        pullRequest = 191,
        demo = { SwipeSensitivityDemo(it) },
    ),
    FeatureCard(
        id = "foldable-settings",
        version = "0.16.0",
        icon = Icons.Outlined.TabletAndroid,
        title = Res.string.tips_a_large_screens_title,
        body = Res.string.tips_a_large_screens_body,
        demoDescription = Res.string.tips_a_large_screens_demo,
        author = "beppe2880",
        pullRequest = 192,
        demo = { LargeScreensDemo(it) },
    ),
    FeatureCard(
        id = "model-picker-stay-open",
        version = "0.15.0",
        icon = Icons.Outlined.Checklist,
        title = Res.string.tips_a_picker_stay_title,
        body = Res.string.tips_a_picker_stay_body,
        demoDescription = Res.string.tips_a_picker_stay_demo,
        author = "Andrease11",
        pullRequest = 176,
        demo = { ModelPickerStayDemo(it) },
    ),
    FeatureCard(
        id = "swipe-chat-list",
        version = "0.15.0",
        icon = Icons.Outlined.Swipe,
        title = Res.string.tips_a_swipe_list_title,
        body = Res.string.tips_a_swipe_list_body,
        demoDescription = Res.string.tips_a_swipe_list_demo,
        author = "beppe2880",
        pullRequest = 181,
        demo = { SwipeChatListDemo(it) },
    ),
    FeatureCard(
        id = "pending-prompt-images",
        version = "0.15.0",
        icon = Icons.Outlined.Schedule,
        title = Res.string.tips_a_pending_prompt_title,
        body = Res.string.tips_a_pending_prompt_body,
        demoDescription = Res.string.tips_a_pending_prompt_demo,
        author = "beppe2880",
        pullRequest = 180,
        demo = { PendingPromptDemo(it) },
    ),
    FeatureCard(
        id = "swipe-between-images",
        version = "0.15.0",
        icon = Icons.Outlined.ViewCarousel,
        title = Res.string.tips_a_image_swipe_title,
        body = Res.string.tips_a_image_swipe_body,
        demoDescription = Res.string.tips_a_image_swipe_demo,
        author = "beppe2880",
        pullRequest = 177,
        demo = { ImageSwipeDemo(it) },
    ),
    FeatureCard(
        id = "hide-side-list",
        version = "0.15.0",
        icon = Icons.AutoMirrored.Outlined.ViewSidebar,
        title = Res.string.tips_a_hide_list_title,
        body = Res.string.tips_a_hide_list_body,
        demoDescription = Res.string.tips_a_hide_list_demo,
        author = "beppe2880",
        pullRequest = 183,
        demo = { HideSideListDemo(it) },
    ),
    FeatureCard(
        id = "check-for-updates",
        version = "0.15.0",
        icon = Icons.Outlined.SystemUpdate,
        title = Res.string.tips_a_check_updates_title,
        body = Res.string.tips_a_check_updates_body,
        demoDescription = Res.string.tips_a_check_updates_demo,
        author = "beppe2880",
        pullRequest = 186,
        demo = { CheckUpdatesDemo(it) },
    ),
)
