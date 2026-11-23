package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarOutline
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_h_star_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_h_star_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_h_star_title
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.FeatureCard

/** Feature cards of group H; see the toolkit in ../DemoKit.kt. */
internal val tipsCardsH: List<FeatureCard> = listOf(
    FeatureCard(
        id = "github-star",
        version = "0.18.0",
        icon = Icons.Outlined.StarOutline,
        title = Res.string.tips_h_star_title,
        body = Res.string.tips_h_star_body,
        demoDescription = Res.string.tips_h_star_demo,
        author = "beppe2880",
        pullRequest = 256,
        demo = { GitHubStarDemo(it) },
    ),
)
