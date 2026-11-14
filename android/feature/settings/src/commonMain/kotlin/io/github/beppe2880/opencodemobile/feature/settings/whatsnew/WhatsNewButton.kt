package io.github.beppe2880.opencodemobile.feature.settings.whatsnew

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.beppe2880.opencodemobile.core.mvi.CollectEffect
import io.github.beppe2880.opencodemobile.core.ui.components.OcStatusDot
import io.github.beppe2880.opencodemobile.core.ui.components.exposeTestTags
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.whats_new_hide
import io.github.beppe2880.opencodemobile.feature.settings.resources.whats_new_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.whats_new_unseen
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The "What's New" button of the session list header, which opens the release history with
 * [onOpen]; draws nothing while it is hidden until the next release.
 */
@Composable
fun WhatsNewButtonRoute(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: WhatsNewButtonViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            WhatsNewButtonEffect.OpenPage -> onOpen()
        }
    }
    if (state.visible) WhatsNewButton(unseen = state.unseen, onEvent = viewModel::onEvent, modifier = modifier)
}

/**
 * A small pill: a sparkle, the label and, while there are releases not seen yet, a dot. The X at
 * its end, or "Hide until the next release" after a long press, hides it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WhatsNewButton(unseen: Boolean, onEvent: (WhatsNewButtonEvent) -> Unit, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    val hide = stringResource(Res.string.whats_new_hide)
    val newReleases = stringResource(Res.string.whats_new_unseen)
    var menu by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(
            shape = RoundedCornerShape(OcRadius.md),
            color = c.bgBase,
            contentColor = c.textBase,
            border = BorderStroke(1.dp, if (unseen) c.info.border else c.borderBase),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // One element for screen readers: the label names it, the dot is its state.
                Row(
                    modifier = Modifier
                        .combinedClickable(
                            role = Role.Button,
                            onLongClickLabel = hide,
                            onLongClick = { menu = true },
                            onClick = { onEvent(WhatsNewButtonEvent.Open) },
                        )
                        .heightIn(min = 36.dp)
                        .padding(start = 10.dp, end = OcSpacing.xs)
                        .semantics { if (unseen) stateDescription = newReleases }
                        .testTag("sessions_whats_new_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = c.textAccent, modifier = Modifier.size(16.dp))
                    Text(
                        stringResource(Res.string.whats_new_title),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (unseen) OcStatusDot(c.bgAccent, Modifier.testTag("sessions_whats_new_dot"))
                }
                Box(
                    modifier = Modifier
                        .clickable(role = Role.Button, onClick = { onEvent(WhatsNewButtonEvent.Hide) })
                        .size(36.dp)
                        .testTag("sessions_whats_new_hide"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = hide, tint = c.iconMuted, modifier = Modifier.size(14.dp))
                }
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = c.bgBase, modifier = Modifier.exposeTestTags()) {
            DropdownMenuItem(
                text = { Text(hide) },
                leadingIcon = { Icon(Icons.Outlined.VisibilityOff, contentDescription = null, tint = c.iconMuted) },
                onClick = {
                    menu = false
                    onEvent(WhatsNewButtonEvent.Hide)
                },
                modifier = Modifier.testTag("sessions_whats_new_hide_menu_item"),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun WhatsNewButtonPreview() {
    OcTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) {
        Row(Modifier.padding(OcSpacing.gutter), horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
            WhatsNewButton(unseen = true, onEvent = {})
            WhatsNewButton(unseen = false, onEvent = {})
        }
    }
}
