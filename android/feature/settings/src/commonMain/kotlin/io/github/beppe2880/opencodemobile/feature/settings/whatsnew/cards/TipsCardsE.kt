package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Widgets
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_e_compaction_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_e_compaction_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_e_compaction_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_e_dictation_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_e_dictation_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_e_dictation_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_e_files_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_e_files_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_e_files_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_e_notifications_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_e_notifications_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_e_notifications_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_e_terminal_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_e_terminal_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_e_terminal_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_e_usage_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_e_usage_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_e_usage_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_e_widget_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_e_widget_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_e_widget_title
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.FeatureCard

/** Feature cards of group E; see the toolkit in ../DemoKit.kt. */
internal val tipsCardsE: List<FeatureCard> = listOf(
    FeatureCard(
        id = "home-screen-widget",
        version = "0.8.0",
        icon = Icons.Outlined.Widgets,
        title = Res.string.tips_e_widget_title,
        body = Res.string.tips_e_widget_body,
        demoDescription = Res.string.tips_e_widget_demo,
        author = "beppe2880",
        pullRequest = 67,
        demo = { WidgetDemo(it) },
    ),
    FeatureCard(
        id = "file-viewer",
        version = "0.8.0",
        icon = Icons.Outlined.Description,
        title = Res.string.tips_e_files_title,
        body = Res.string.tips_e_files_body,
        demoDescription = Res.string.tips_e_files_demo,
        author = "beppe2880",
        pullRequest = 69,
        demo = { FileViewerDemo(it) },
    ),
    FeatureCard(
        id = "terminal",
        version = "0.8.0",
        icon = Icons.Outlined.Terminal,
        title = Res.string.tips_e_terminal_title,
        body = Res.string.tips_e_terminal_body,
        demoDescription = Res.string.tips_e_terminal_demo,
        author = "beppe2880",
        pullRequest = 66,
        demo = { TerminalDemo(it) },
    ),
    FeatureCard(
        id = "compaction-summary",
        version = "0.8.0",
        icon = Icons.Outlined.Compress,
        title = Res.string.tips_e_compaction_title,
        body = Res.string.tips_e_compaction_body,
        demoDescription = Res.string.tips_e_compaction_demo,
        author = "beppe2880",
        pullRequest = 64,
        demo = { CompactionDemo(it) },
    ),
    FeatureCard(
        id = "dictation",
        version = "0.7.0",
        icon = Icons.Outlined.Mic,
        title = Res.string.tips_e_dictation_title,
        body = Res.string.tips_e_dictation_body,
        demoDescription = Res.string.tips_e_dictation_demo,
        author = "beppe2880",
        pullRequest = 39,
        demo = { DictationDemo(it) },
    ),
    FeatureCard(
        id = "usage-statistics",
        version = "0.6.0",
        icon = Icons.Outlined.BarChart,
        title = Res.string.tips_e_usage_title,
        body = Res.string.tips_e_usage_body,
        demoDescription = Res.string.tips_e_usage_demo,
        author = "beppe2880",
        pullRequest = 35,
        demo = { UsageDemo(it) },
    ),
    FeatureCard(
        id = "notification-kinds",
        version = "0.5.0",
        icon = Icons.Outlined.NotificationsActive,
        title = Res.string.tips_e_notifications_title,
        body = Res.string.tips_e_notifications_body,
        demoDescription = Res.string.tips_e_notifications_demo,
        author = "beppe2880",
        pullRequest = 22,
        demo = { NotificationKindsDemo(it) },
    ),
)
