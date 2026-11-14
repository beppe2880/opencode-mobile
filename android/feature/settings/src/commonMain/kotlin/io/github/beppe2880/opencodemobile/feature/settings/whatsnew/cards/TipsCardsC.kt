package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_next_message_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_next_message_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_next_message_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_pending_bar_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_pending_bar_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_pending_bar_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_running_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_running_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_running_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_settings_categories_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_settings_categories_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_settings_categories_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_terminal_fullscreen_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_terminal_fullscreen_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_terminal_fullscreen_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_thoughts_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_thoughts_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_thoughts_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_voice_cloud_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_voice_cloud_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_voice_cloud_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_voice_record_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_voice_record_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_voice_record_title
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.FeatureCard

/** Feature cards of group C; see the toolkit in ../DemoKit.kt. */
internal val tipsCardsC: List<FeatureCard> = listOf(
    FeatureCard(
        id = "voice-recording-waveform",
        version = "0.11.0",
        icon = Icons.Outlined.Mic,
        title = Res.string.tips_c_voice_record_title,
        body = Res.string.tips_c_voice_record_body,
        demoDescription = Res.string.tips_c_voice_record_demo,
        author = "beppe2880",
        pullRequest = 140,
        demo = { VoiceRecordDemo(it) },
    ),
    FeatureCard(
        id = "voice-cloud-transcription",
        version = "0.11.0",
        icon = Icons.Outlined.CloudQueue,
        title = Res.string.tips_c_voice_cloud_title,
        body = Res.string.tips_c_voice_cloud_body,
        demoDescription = Res.string.tips_c_voice_cloud_demo,
        author = "beppe2880",
        pullRequest = 136,
        demo = { VoiceCloudDemo(it) },
    ),
    FeatureCard(
        id = "settings-categories-search",
        version = "0.11.0",
        icon = Icons.Outlined.Tune,
        title = Res.string.tips_c_settings_categories_title,
        body = Res.string.tips_c_settings_categories_body,
        demoDescription = Res.string.tips_c_settings_categories_demo,
        author = "beppe2880",
        pullRequest = 141,
        demo = { SettingsCategoriesDemo(it) },
    ),
    FeatureCard(
        id = "terminal-fullscreen",
        version = "0.10.0",
        icon = Icons.Outlined.Fullscreen,
        title = Res.string.tips_c_terminal_fullscreen_title,
        body = Res.string.tips_c_terminal_fullscreen_body,
        demoDescription = Res.string.tips_c_terminal_fullscreen_demo,
        author = "beppe2880",
        pullRequest = 99,
        demo = { TerminalFullscreenDemo(it) },
    ),
    FeatureCard(
        id = "model-change-next-message",
        version = "0.10.0",
        icon = Icons.Outlined.SwapHoriz,
        title = Res.string.tips_c_next_message_title,
        body = Res.string.tips_c_next_message_body,
        demoDescription = Res.string.tips_c_next_message_demo,
        author = "beppe2880",
        pullRequest = 118,
        demo = { NextMessageDemo(it) },
    ),
    FeatureCard(
        id = "grouped-thoughts",
        version = "0.10.0",
        icon = Icons.Outlined.Lightbulb,
        title = Res.string.tips_c_thoughts_title,
        body = Res.string.tips_c_thoughts_body,
        demoDescription = Res.string.tips_c_thoughts_demo,
        author = "beppe2880",
        pullRequest = 124,
        demo = { ThoughtsGroupDemo(it) },
    ),
    FeatureCard(
        id = "pending-card-bar",
        version = "0.10.0",
        icon = Icons.Outlined.Checklist,
        title = Res.string.tips_c_pending_bar_title,
        body = Res.string.tips_c_pending_bar_body,
        demoDescription = Res.string.tips_c_pending_bar_demo,
        author = "beppe2880",
        pullRequest = 119,
        demo = { PendingBarDemo(it) },
    ),
    FeatureCard(
        id = "running-work",
        version = "0.10.0",
        icon = Icons.Outlined.AccountTree,
        title = Res.string.tips_c_running_title,
        body = Res.string.tips_c_running_body,
        demoDescription = Res.string.tips_c_running_demo,
        author = "beppe2880",
        pullRequest = 105,
        demo = { RunningWorkDemo(it) },
    ),
)
