package io.github.beppe2880.opencodemobile.feature.settings.star

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.beppe2880.opencodemobile.core.mvi.CollectEffect
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcCard
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.github_star_banner
import io.github.beppe2880.opencodemobile.feature.settings.resources.github_star_banner_dismiss
import io.github.beppe2880.opencodemobile.feature.settings.resources.github_star_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.github_star_button
import io.github.beppe2880.opencodemobile.feature.settings.resources.github_star_thanks
import io.github.beppe2880.opencodemobile.feature.settings.resources.github_star_thanks_body
import io.github.beppe2880.opencodemobile.feature.settings.resources.github_star_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The banner above the sessions inviting to star the project on GitHub: shown in one run of the
 * app only, after a while of use; draws nothing otherwise. Opening it counts a day of use.
 */
@Composable
fun GitHubStarBannerRoute(modifier: Modifier = Modifier) {
    val viewModel: GitHubStarViewModel = koinViewModel { parametersOf(true) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    OpenUrlEffects(viewModel)
    if (state.bannerVisible) GitHubStarBanner(viewModel::onEvent, modifier)
}

/** The card of Tips & new features inviting to star the project; a thank you once opened. */
@Composable
fun GitHubStarCardRoute(modifier: Modifier = Modifier) {
    val viewModel: GitHubStarViewModel = koinViewModel { parametersOf(false) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    OpenUrlEffects(viewModel)
    GitHubStarCard(state.starred, viewModel::onEvent, modifier)
}

@Composable
private fun OpenUrlEffects(viewModel: GitHubStarViewModel) {
    val uriHandler = LocalUriHandler.current
    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is GitHubStarEffect.OpenUrl -> runCatching { uriHandler.openUri(effect.url) }
        }
    }
}

/** Like the update notice: tapping it opens GitHub, the X hides it for good. */
@Composable
fun GitHubStarBanner(onEvent: (GitHubStarEvent) -> Unit, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OcRadius.lg))
            .background(c.layer01)
            .testTag("github_star_banner"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // One element for screen readers: the text is its label, opening GitHub its action.
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clickable(
                    onClickLabel = stringResource(Res.string.github_star_button),
                    role = Role.Button,
                    onClick = { onEvent(GitHubStarEvent.Star) },
                )
                .padding(start = OcSpacing.md)
                .testTag("github_star_banner_open"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.StarOutline, null, tint = c.textAccent, modifier = Modifier.size(18.dp))
            Text(
                stringResource(Res.string.github_star_banner),
                style = MaterialTheme.typography.bodyMedium,
                color = c.textBase,
                // Vertical padding only shows when large text wraps above the 48 dp row.
                modifier = Modifier.padding(horizontal = OcSpacing.sm, vertical = OcSpacing.sm),
            )
        }
        OcIconButton(
            icon = Icons.Outlined.Close,
            contentDescription = stringResource(Res.string.github_star_banner_dismiss),
            onClick = { onEvent(GitHubStarEvent.DismissBanner) },
            tint = c.iconMuted,
            iconSize = 18.dp,
            modifier = Modifier.testTag("github_star_banner_dismiss"),
        )
    }
}

/** "Enjoying the app?" with the button to GitHub; once opened, a thank you without a button. */
@Composable
fun GitHubStarCard(starred: Boolean, onEvent: (GitHubStarEvent) -> Unit, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    OcCard(modifier.fillMaxWidth().testTag("github_star_card")) {
        Column(
            Modifier.padding(OcSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                Icon(Icons.Outlined.StarOutline, contentDescription = null, tint = c.textAccent, modifier = Modifier.size(20.dp))
                Text(
                    stringResource(if (starred) Res.string.github_star_thanks else Res.string.github_star_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = c.textBase,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Text(
                stringResource(if (starred) Res.string.github_star_thanks_body else Res.string.github_star_body),
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            if (!starred) {
                OcButton(
                    text = stringResource(Res.string.github_star_button),
                    onClick = { onEvent(GitHubStarEvent.Star) },
                    variant = OcButtonVariant.Secondary,
                    leadingIcon = Icons.Outlined.StarOutline,
                    compact = true,
                    modifier = Modifier.padding(top = OcSpacing.xs).testTag("github_star_card_button"),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun GitHubStarPreview() {
    OcTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) {
        Column(Modifier.padding(OcSpacing.gutter), verticalArrangement = Arrangement.spacedBy(OcSpacing.md)) {
            GitHubStarBanner(onEvent = {})
            GitHubStarCard(starred = false, onEvent = {})
            GitHubStarCard(starred = true, onEvent = {})
        }
    }
}
