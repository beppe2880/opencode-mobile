package io.github.beppe2880.opencodemobile.feature.settings.update

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NewReleases
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.beppe2880.opencodemobile.core.domain.AppRelease
import io.github.beppe2880.opencodemobile.core.mvi.CollectEffect
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_available
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_banner_open
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_dismiss
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The banner announcing a newer release of the app, with the banner style or after the window
 * was closed; draws nothing otherwise. Shares its ViewModel with [UpdateSheetRoute] in the same screen.
 */
@Composable
fun UpdateNoticeRoute(modifier: Modifier = Modifier) {
    val viewModel: UpdateNoticeViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is UpdateNoticeEffect.OpenUrl -> runCatching { uriHandler.openUri(effect.url) }
        }
    }
    if (state.bannerVisible) UpdateNotice(state, viewModel::onEvent, modifier)
}

/** "Version x is available": tapping it opens the update sheet, the X skips the version. */
@Composable
fun UpdateNotice(state: UpdateNoticeState, onEvent: (UpdateNoticeEvent) -> Unit, modifier: Modifier = Modifier) {
    val release = state.release ?: return
    val c = OcTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OcRadius.lg))
            .background(c.layer01)
            .testTag("update_notice"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // One element for screen readers: the text is its label, opening the sheet its action.
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clickable(
                    onClickLabel = stringResource(Res.string.update_banner_open),
                    role = Role.Button,
                    onClick = { onEvent(UpdateNoticeEvent.ShowSheet) },
                )
                .padding(start = OcSpacing.md)
                .testTag("update_notice_open"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.NewReleases, null, tint = c.textAccent, modifier = Modifier.size(18.dp))
            Text(
                stringResource(Res.string.update_available, release.version),
                style = MaterialTheme.typography.bodyMedium,
                color = c.textBase,
                // Vertical padding only shows when large text wraps above the 48 dp row.
                modifier = Modifier.padding(horizontal = OcSpacing.sm, vertical = OcSpacing.sm),
            )
        }
        OcIconButton(
            icon = Icons.Outlined.Close,
            contentDescription = stringResource(Res.string.update_dismiss),
            onClick = { onEvent(UpdateNoticeEvent.Dismiss) },
            tint = c.iconMuted,
            iconSize = 18.dp,
            modifier = Modifier.testTag("update_notice_dismiss_button"),
        )
    }
}

@PreviewLightDark
@Composable
private fun UpdateNoticePreview() {
    OcTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) {
        val release = AppRelease("1.0.0", "https://github.com/beppe2880/opencode-mobile/releases/tag/v1.0.0")
        UpdateNotice(
            state = UpdateNoticeState(release, UpdateTarget.Android.actionFor(release)),
            onEvent = {},
            modifier = Modifier.padding(OcSpacing.gutter),
        )
    }
}
