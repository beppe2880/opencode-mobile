package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_follow_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_follow_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_follow_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_header_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_header_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_header_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_mcp_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_mcp_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_mcp_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_offline_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_offline_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_offline_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_output_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_output_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_output_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_settings_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_settings_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_settings_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_share_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_share_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_share_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_split_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_split_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_split_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_update_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_update_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_d_update_title
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.FeatureCard

/** Feature cards of group D; see the toolkit in ../DemoKit.kt. */
internal val tipsCardsD: List<FeatureCard> = listOf(
    FeatureCard(
        id = "chat-header-vcs",
        version = "0.10.0",
        icon = Icons.Outlined.AccountTree,
        title = Res.string.tips_d_header_title,
        body = Res.string.tips_d_header_body,
        demoDescription = Res.string.tips_d_header_demo,
        author = "beppe2880",
        pullRequest = 120,
        demo = { HeaderDemo(it) },
    ),
    FeatureCard(
        id = "offline-demo",
        version = "0.10.0",
        icon = Icons.Outlined.Science,
        title = Res.string.tips_d_offline_title,
        body = Res.string.tips_d_offline_body,
        demoDescription = Res.string.tips_d_offline_demo,
        author = "Andrease11",
        pullRequest = 103,
        demo = { OfflineDemo(it) },
    ),
    FeatureCard(
        id = "split-view",
        version = "0.10.0",
        icon = Icons.Outlined.SpaceDashboard,
        title = Res.string.tips_d_split_title,
        body = Res.string.tips_d_split_body,
        demoDescription = Res.string.tips_d_split_demo,
        author = "beppe2880",
        pullRequest = 98,
        demo = { SplitDemo(it) },
    ),
    FeatureCard(
        id = "share-to-opencode",
        version = "0.10.0",
        icon = Icons.Outlined.IosShare,
        title = Res.string.tips_d_share_title,
        body = Res.string.tips_d_share_body,
        demoDescription = Res.string.tips_d_share_demo,
        author = "beppe2880",
        pullRequest = 134,
        demo = { ShareDemo(it) },
    ),
    FeatureCard(
        id = "settings-live-preview",
        version = "0.10.0",
        icon = Icons.Outlined.Visibility,
        title = Res.string.tips_d_settings_title,
        body = Res.string.tips_d_settings_body,
        demoDescription = Res.string.tips_d_settings_demo,
        author = "beppe2880",
        pullRequest = 128,
        demo = { SettingsPreviewDemo(it) },
    ),
    FeatureCard(
        id = "update-sheet",
        version = "0.10.0",
        icon = Icons.Outlined.NewReleases,
        title = Res.string.tips_d_update_title,
        body = Res.string.tips_d_update_body,
        demoDescription = Res.string.tips_d_update_demo,
        author = "beppe2880",
        pullRequest = 132,
        demo = { UpdateDemo(it) },
    ),
    FeatureCard(
        id = "mcp-servers",
        version = "0.9.0",
        icon = Icons.Outlined.Extension,
        title = Res.string.tips_d_mcp_title,
        body = Res.string.tips_d_mcp_body,
        demoDescription = Res.string.tips_d_mcp_demo,
        author = "Andrease11",
        pullRequest = 91,
        demo = { McpDemo(it) },
    ),
    FeatureCard(
        id = "follow-live",
        version = "0.9.0",
        icon = Icons.Outlined.Dns,
        title = Res.string.tips_d_follow_title,
        body = Res.string.tips_d_follow_body,
        demoDescription = Res.string.tips_d_follow_demo,
        author = "beppe2880",
        pullRequest = 72,
        demo = { FollowDemo(it) },
    ),
    FeatureCard(
        id = "output-settings",
        version = "0.9.0",
        icon = Icons.Outlined.Tune,
        title = Res.string.tips_d_output_title,
        body = Res.string.tips_d_output_body,
        demoDescription = Res.string.tips_d_output_demo,
        author = "beppe2880",
        pullRequest = 75,
        demo = { OutputSettingsDemo(it) },
    ),
)
