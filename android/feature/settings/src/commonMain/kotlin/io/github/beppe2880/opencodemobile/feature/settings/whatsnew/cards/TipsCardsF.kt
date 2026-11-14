package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Terminal
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_changes_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_changes_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_changes_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_fork_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_fork_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_fork_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_live_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_live_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_live_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_notifications_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_notifications_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_notifications_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_pair_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_pair_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_pair_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_queue_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_queue_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_queue_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_shell_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_shell_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_shell_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_steer_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_steer_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_steer_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_undo_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_undo_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_f_undo_title
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.FeatureCard

/** Feature cards of group F; see the toolkit in ../DemoKit.kt. */
internal val tipsCardsF: List<FeatureCard> = listOf(
    FeatureCard(
        id = "shell-commands",
        version = "0.4.0",
        icon = Icons.Outlined.Terminal,
        title = Res.string.tips_f_shell_title,
        body = Res.string.tips_f_shell_body,
        demoDescription = Res.string.tips_f_shell_demo,
        author = "beppe2880",
        pullRequest = 19,
        demo = { ShellDemo(it) },
    ),
    FeatureCard(
        id = "queued-prompts",
        version = "0.4.0",
        icon = Icons.Outlined.Schedule,
        title = Res.string.tips_f_queue_title,
        body = Res.string.tips_f_queue_body,
        demoDescription = Res.string.tips_f_queue_demo,
        author = "beppe2880",
        pullRequest = 20,
        demo = { QueuedDemo(it) },
    ),
    FeatureCard(
        id = "session-changes",
        version = "0.4.0",
        icon = Icons.Outlined.Difference,
        title = Res.string.tips_f_changes_title,
        body = Res.string.tips_f_changes_body,
        demoDescription = Res.string.tips_f_changes_demo,
        author = "beppe2880",
        pullRequest = 18,
        demo = { ChangesDemo(it) },
    ),
    FeatureCard(
        id = "session-notifications",
        version = "0.4.0",
        icon = Icons.Outlined.NotificationsActive,
        title = Res.string.tips_f_notifications_title,
        body = Res.string.tips_f_notifications_body,
        demoDescription = Res.string.tips_f_notifications_demo,
        author = "beppe2880",
        pullRequest = 17,
        demo = { NotificationsDemo(it) },
    ),
    FeatureCard(
        id = "pair-with-server",
        version = "0.3.0",
        icon = Icons.Outlined.QrCodeScanner,
        title = Res.string.tips_f_pair_title,
        body = Res.string.tips_f_pair_body,
        demoDescription = Res.string.tips_f_pair_demo,
        author = "beppe2880",
        pullRequest = null,
        demo = { PairDemo(it) },
    ),
    FeatureCard(
        id = "live-sessions",
        version = "0.3.0",
        icon = Icons.Outlined.Sync,
        title = Res.string.tips_f_live_title,
        body = Res.string.tips_f_live_body,
        demoDescription = Res.string.tips_f_live_demo,
        author = "beppe2880",
        pullRequest = null,
        demo = { LiveDemo(it) },
    ),
    FeatureCard(
        id = "steer-queue-commands",
        version = "0.3.0",
        icon = Icons.Outlined.Keyboard,
        title = Res.string.tips_f_steer_title,
        body = Res.string.tips_f_steer_body,
        demoDescription = Res.string.tips_f_steer_demo,
        author = "beppe2880",
        pullRequest = null,
        demo = { SteerDemo(it) },
    ),
    FeatureCard(
        id = "fork-session",
        version = "0.3.0",
        icon = Icons.Outlined.CallSplit,
        title = Res.string.tips_f_fork_title,
        body = Res.string.tips_f_fork_body,
        demoDescription = Res.string.tips_f_fork_demo,
        author = "beppe2880",
        pullRequest = null,
        demo = { ForkDemo(it) },
    ),
    FeatureCard(
        id = "undo-redo",
        version = "0.3.0",
        icon = Icons.AutoMirrored.Outlined.Undo,
        title = Res.string.tips_f_undo_title,
        body = Res.string.tips_f_undo_body,
        demoDescription = Res.string.tips_f_undo_demo,
        author = "beppe2880",
        pullRequest = null,
        demo = { UndoDemo(it) },
    ),
)
