package io.github.beppe2880.opencodemobile.core.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import io.github.beppe2880.opencodemobile.core.ui.resources.Res
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_mute_duration
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_mute_until_finished
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_mute_until_finished_unavailable
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_mute_until_unmuted
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun SessionMuteSheet(running: Boolean, onDismiss: () -> Unit, onMute: (untilFinished: Boolean) -> Unit) {
    OcBottomSheet(onDismiss = onDismiss, testTag = "session_mute_sheet") {
        SessionMuteChoices(running, onMute)
    }
}

@Composable
fun ColumnScope.SessionMuteChoices(running: Boolean, onMute: (untilFinished: Boolean) -> Unit) {
    val colors = OcTheme.colors
    Text(
        stringResource(Res.string.oc_mute_duration),
        style = MaterialTheme.typography.titleSmall,
        color = colors.textBase,
        modifier = Modifier.padding(horizontal = OcSpacing.lg, vertical = OcSpacing.sm),
    )
    OcListRow(
        title = stringResource(Res.string.oc_mute_until_unmuted),
        leading = { Icon(Icons.Outlined.NotificationsOff, null, tint = colors.iconBase) },
        onClick = { onMute(false) },
        titleMaxLines = 2,
        modifier = Modifier.testTag("session_mute_manual"),
    )
    OcListRow(
        title = stringResource(Res.string.oc_mute_until_finished),
        subtitle = if (!running) stringResource(Res.string.oc_mute_until_finished_unavailable) else null,
        leading = { Icon(Icons.Outlined.Schedule, null, tint = colors.iconBase) },
        titleColor = if (running) colors.textBase else colors.textFaint,
        onClick = if (running) ({ onMute(true) }) else null,
        titleMaxLines = 2,
        subtitleMaxLines = 2,
        modifier = Modifier.testTag("session_mute_until_finished").semantics { if (!running) disabled() },
    )
    Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = OcSpacing.lg))
}
