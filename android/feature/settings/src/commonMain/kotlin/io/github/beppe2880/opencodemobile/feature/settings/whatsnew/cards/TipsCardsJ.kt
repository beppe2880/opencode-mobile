package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.KeyboardHide
import androidx.compose.material.icons.outlined.Lock
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_session_shortcuts
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_j_app_lock_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_j_app_lock_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_j_app_lock_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_j_hide_keyboard_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_j_hide_keyboard_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_j_hide_keyboard_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_j_sessions_bulk_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_j_sessions_bulk_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_j_sessions_bulk_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_session_shortcuts_body
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.FeatureCard

/** Feature cards of group J; see the toolkit in ../DemoKit.kt. */
internal val tipsCardsJ: List<FeatureCard> = listOf(
    FeatureCard(
        id = "session-bulk-actions",
        version = "0.19.0",
        icon = Icons.Outlined.Checklist,
        title = Res.string.tips_j_sessions_bulk_title,
        body = Res.string.tips_j_sessions_bulk_body,
        demoDescription = Res.string.tips_j_sessions_bulk_demo,
        author = "Azykle",
        pullRequest = 266,
        morePullRequests = listOf(269),
        demo = { SessionBulkActionsDemo(it) },
    ),
    FeatureCard(
        id = "session-selection-shortcuts",
        version = "0.20.0",
        icon = Icons.Outlined.Checklist,
        title = Res.string.settings_session_shortcuts,
        body = Res.string.tips_session_shortcuts_body,
        demoDescription = Res.string.settings_session_shortcuts,
        author = "Andrease11",
        pullRequest = 287,
        morePullRequests = listOf(290),
        demo = { SessionBulkActionsDemo(it, shortcuts = true) },
    ),
    FeatureCard(
        id = "hide-keyboard-after-send",
        version = "0.19.0",
        icon = Icons.Outlined.KeyboardHide,
        title = Res.string.tips_j_hide_keyboard_title,
        body = Res.string.tips_j_hide_keyboard_body,
        demoDescription = Res.string.tips_j_hide_keyboard_demo,
        author = "beppe2880",
        pullRequest = 275,
        demo = { HideKeyboardDemo(it) },
    ),
    FeatureCard(
        id = "app-lock",
        version = "0.20.0",
        icon = Icons.Outlined.Lock,
        title = Res.string.tips_j_app_lock_title,
        body = Res.string.tips_j_app_lock_body,
        demoDescription = Res.string.tips_j_app_lock_demo,
        author = "beppe2880",
        pullRequest = 292,
        demo = { AppLockDemo(it) },
    ),
)
