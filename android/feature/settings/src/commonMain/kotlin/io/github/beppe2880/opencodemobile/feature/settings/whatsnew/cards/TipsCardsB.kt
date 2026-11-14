package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.TouchApp
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_browse_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_browse_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_browse_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_chips_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_chips_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_chips_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_connect_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_connect_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_connect_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_github_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_github_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_github_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_model_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_model_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_model_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_new_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_new_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_new_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_popover_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_popover_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_popover_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_search_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_search_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_search_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_stopall_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_stopall_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_stopall_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_themes_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_themes_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_themes_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_toolmedia_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_toolmedia_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_b_toolmedia_title
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.FeatureCard

/** Feature cards of group B; see the toolkit in ../DemoKit.kt. */
internal val tipsCardsB: List<FeatureCard> = listOf(
    FeatureCard(
        id = "browse-server-folders",
        version = "0.14.0",
        icon = Icons.Outlined.FolderOpen,
        title = Res.string.tips_b_browse_title,
        body = Res.string.tips_b_browse_body,
        demoDescription = Res.string.tips_b_browse_demo,
        author = "rollins1338",
        pullRequest = 173,
        demo = { FolderBrowseDemo(it) },
    ),
    FeatureCard(
        id = "running-popover-open-stop",
        version = "0.13.0",
        icon = Icons.Outlined.TouchApp,
        title = Res.string.tips_b_popover_title,
        body = Res.string.tips_b_popover_body,
        demoDescription = Res.string.tips_b_popover_demo,
        author = "beppe2880",
        pullRequest = 167,
        demo = { RunningPopoverDemo(it) },
    ),
    FeatureCard(
        id = "tool-media-inline",
        version = "0.13.0",
        icon = Icons.Outlined.Image,
        title = Res.string.tips_b_toolmedia_title,
        body = Res.string.tips_b_toolmedia_body,
        demoDescription = Res.string.tips_b_toolmedia_demo,
        author = "beppe2880",
        pullRequest = 164,
        demo = { ToolMediaDemo(it) },
    ),
    FeatureCard(
        id = "new-command",
        version = "0.12.0",
        icon = Icons.Outlined.AddComment,
        title = Res.string.tips_b_new_title,
        body = Res.string.tips_b_new_body,
        demoDescription = Res.string.tips_b_new_demo,
        author = "Andrease11",
        pullRequest = 152,
        demo = { NewCommandDemo(it) },
    ),
    FeatureCard(
        id = "connect-providers",
        version = "0.12.0",
        icon = Icons.Outlined.Key,
        title = Res.string.tips_b_connect_title,
        body = Res.string.tips_b_connect_body,
        demoDescription = Res.string.tips_b_connect_demo,
        author = "Andrease11",
        pullRequest = 148,
        demo = { ConnectProviderDemo(it) },
    ),
    FeatureCard(
        id = "github-links-tool-rows",
        version = "0.12.0",
        icon = Icons.AutoMirrored.Outlined.OpenInNew,
        title = Res.string.tips_b_github_title,
        body = Res.string.tips_b_github_body,
        demoDescription = Res.string.tips_b_github_demo,
        author = "beppe2880",
        pullRequest = 161,
        demo = { GitHubOpenDemo(it) },
    ),
    FeatureCard(
        id = "running-chips",
        version = "0.12.0",
        icon = Icons.Outlined.Terminal,
        title = Res.string.tips_b_chips_title,
        body = Res.string.tips_b_chips_body,
        demoDescription = Res.string.tips_b_chips_demo,
        author = "beppe2880",
        pullRequest = 162,
        demo = { RunningChipsDemo(it) },
    ),
    FeatureCard(
        id = "model-button",
        version = "0.12.0",
        icon = Icons.Outlined.Psychology,
        title = Res.string.tips_b_model_title,
        body = Res.string.tips_b_model_body,
        demoDescription = Res.string.tips_b_model_demo,
        author = "beppe2880",
        pullRequest = 163,
        demo = { ModelButtonDemo(it) },
    ),
    FeatureCard(
        id = "stop-all-running",
        version = "0.12.0",
        icon = Icons.Outlined.StopCircle,
        title = Res.string.tips_b_stopall_title,
        body = Res.string.tips_b_stopall_body,
        demoDescription = Res.string.tips_b_stopall_demo,
        author = "beppe2880",
        pullRequest = 146,
        demo = { StopAllDemo(it) },
    ),
    FeatureCard(
        id = "search-projects",
        version = "0.12.0",
        icon = Icons.Outlined.Search,
        title = Res.string.tips_b_search_title,
        body = Res.string.tips_b_search_body,
        demoDescription = Res.string.tips_b_search_demo,
        author = "beppe2880",
        pullRequest = 155,
        demo = { ProjectSearchDemo(it) },
    ),
    FeatureCard(
        id = "opencode-themes",
        version = "0.12.0",
        icon = Icons.Outlined.Palette,
        title = Res.string.tips_b_themes_title,
        body = Res.string.tips_b_themes_body,
        demoDescription = Res.string.tips_b_themes_demo,
        author = "beppe2880",
        pullRequest = 156,
        demo = { OpenCodeThemesDemo(it) },
    ),
)
