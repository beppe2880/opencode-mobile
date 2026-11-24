package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_i_chat_actions_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_i_chat_actions_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_i_chat_actions_title
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.FeatureCard

/** Feature cards of group I; see the toolkit in ../DemoKit.kt. */
internal val tipsCardsI: List<FeatureCard> = listOf(
    FeatureCard(
        id = "chat-actions",
        version = "0.19.0",
        icon = Icons.Outlined.MoreVert,
        title = Res.string.tips_i_chat_actions_title,
        body = Res.string.tips_i_chat_actions_body,
        demoDescription = Res.string.tips_i_chat_actions_demo,
        author = "Azykle",
        pullRequest = 267,
        demo = { ChatActionsDemo(it) },
    ),
)
