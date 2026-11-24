package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_j_sessions_bulk_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_j_sessions_bulk_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_j_sessions_bulk_title
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
        demo = { SessionBulkActionsDemo(it) },
    ),
)
