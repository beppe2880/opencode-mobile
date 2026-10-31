package io.github.beppe2880.opencodemobile.feature.settings.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.beppe2880.opencodemobile.core.domain.AppRelease
import io.github.beppe2880.opencodemobile.core.mvi.CollectEffect
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_available
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_dismiss
import io.github.beppe2880.opencodemobile.feature.settings.resources.update_view
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** A banner announcing a newer release of the app; draws nothing otherwise. */
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
    UpdateNotice(state, viewModel::onEvent, modifier)
}

@Composable
fun UpdateNotice(state: UpdateNoticeState, onEvent: (UpdateNoticeEvent) -> Unit, modifier: Modifier = Modifier) {
    val release = state.release ?: return
    val c = OcTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(c.layer01, RoundedCornerShape(OcRadius.lg))
            .padding(start = OcSpacing.md)
            .testTag("update_notice"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.NewReleases, null, tint = c.iconMuted, modifier = Modifier.size(18.dp))
        Text(
            stringResource(Res.string.update_available, release.version),
            style = MaterialTheme.typography.bodyMedium,
            color = c.textBase,
            // Vertical padding only shows when large text wraps above the 48 dp buttons.
            modifier = Modifier.weight(1f).padding(horizontal = OcSpacing.sm, vertical = OcSpacing.sm),
        )
        OcButton(
            text = stringResource(Res.string.update_view),
            onClick = { onEvent(UpdateNoticeEvent.Open) },
            variant = OcButtonVariant.Ghost,
            compact = true,
            modifier = Modifier.testTag("update_notice_view_button"),
        )
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
        UpdateNotice(
            state = UpdateNoticeState(AppRelease("1.0.0", "https://github.com/beppe2880/opencode-mobile/releases/tag/v1.0.0")),
            onEvent = {},
            modifier = Modifier.padding(OcSpacing.gutter),
        )
    }
}
