package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterAlt
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_g_filters_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_g_filters_demo
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_g_filters_title
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.FeatureCard

/** Feature cards of group G; see the toolkit in ../DemoKit.kt. */
internal val tipsCardsG: List<FeatureCard> = listOf(
    FeatureCard(
        id = "session-filters",
        version = "0.18.0",
        icon = Icons.Outlined.FilterAlt,
        title = Res.string.tips_g_filters_title,
        body = Res.string.tips_g_filters_body,
        demoDescription = Res.string.tips_g_filters_demo,
        author = "Azykle",
        pullRequest = null,
        demo = { SessionFiltersDemo(it) },
    ),
)
