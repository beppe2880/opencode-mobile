package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.PermissionDecision
import io.github.beppe2880.opencodemobile.core.domain.PermissionRequest
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcCard
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_permission_always
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_permission_collapse
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_permission_expand
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_permission_more
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_permission_once
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_permission_reject
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_permission_title
import org.jetbrains.compose.resources.stringResource

/**
 * Pinned above the composer while the agent waits for a permission decision. [collapsed]
 * folds it into a one-line bar; the request stays pending until one of the buttons is used.
 */
@Composable
internal fun PermissionCard(
    request: PermissionRequest,
    pendingCount: Int,
    replying: Boolean,
    collapsed: Boolean,
    onCollapse: (Boolean) -> Unit,
    onReply: (PermissionDecision) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = OcTheme.colors
    val collapse by rememberUpdatedState(onCollapse)
    OcCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag("permission_card"),
        color = if (c.isDark) c.layer01 else c.warning.bg,
        borderColor = c.warning.border,
        shape = RoundedCornerShape(OcRadius.xl),
    ) {
        if (collapsed) {
            CollapsedPermission(request, pendingCount, onExpand = { collapse(false) })
        } else {
            ExpandedPermission(request, pendingCount, replying, onCollapse = { collapse(true) }, onReply)
        }
    }
}

@Composable
private fun ExpandedPermission(
    request: PermissionRequest,
    pendingCount: Int,
    replying: Boolean,
    onCollapse: () -> Unit,
    onReply: (PermissionDecision) -> Unit,
) {
    val c = OcTheme.colors
    Column(
        Modifier.padding(start = OcSpacing.md, end = OcSpacing.md, bottom = OcSpacing.md),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        PendingCardHeader("permission", stringResource(Res.string.chat_permission_collapse), onCollapse) {
            Icon(Icons.Outlined.Shield, null, tint = c.warning.fg, modifier = Modifier.size(18.dp))
            Text(
                stringResource(Res.string.chat_permission_title),
                style = MaterialTheme.typography.titleSmall,
                color = c.textBase,
                modifier = Modifier.weight(1f),
            )
            Text(request.action, style = OcTheme.mono.label, color = c.textMuted)
        }
        request.message?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
        request.resources.take(3).forEach { resource ->
            Text(
                resource,
                style = OcTheme.mono.codeSmall,
                color = c.textCodePath,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
            )
        }
        if (pendingCount > 1) {
            Text(stringResource(Res.string.chat_permission_more, pendingCount - 1), style = MaterialTheme.typography.labelSmall, color = c.textFaint)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
            OcButton(
                text = stringResource(Res.string.chat_permission_reject),
                onClick = { onReply(PermissionDecision.Reject) },
                variant = OcButtonVariant.Secondary,
                enabled = !replying,
                compact = true,
                modifier = Modifier.weight(1f).testTag("permission_reject_button"),
            )
            if (request.canSave) {
                OcButton(
                    text = stringResource(Res.string.chat_permission_always),
                    onClick = { onReply(PermissionDecision.Always) },
                    variant = OcButtonVariant.Secondary,
                    enabled = !replying,
                    compact = true,
                    modifier = Modifier.weight(1f).testTag("permission_always_button"),
                )
            }
            OcButton(
                text = stringResource(Res.string.chat_permission_once),
                onClick = { onReply(PermissionDecision.Once) },
                variant = OcButtonVariant.Inverse,
                loading = replying,
                compact = true,
                modifier = Modifier.weight(1.2f).testTag("permission_allow_once_button"),
            )
        }
    }
}

/** The folded card: still says a permission is waiting and for which tool. */
@Composable
private fun CollapsedPermission(request: PermissionRequest, pendingCount: Int, onExpand: () -> Unit) {
    val c = OcTheme.colors
    val title = stringResource(Res.string.chat_permission_title)
    PendingCardBar(
        tagPrefix = "permission",
        icon = Icons.Outlined.Shield,
        iconTint = c.warning.fg,
        iconDescription = null,
        expandLabel = stringResource(Res.string.chat_permission_expand),
        onExpand = onExpand,
    ) {
        // One text, so a long title at a large font scale still leaves room for the tool name.
        val toolStyle = OcTheme.mono.label.toSpanStyle().copy(color = c.textMuted)
        Text(
            buildAnnotatedString {
                append(title)
                append("  ")
                withStyle(toolStyle) { append(request.action) }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = c.textBase,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (pendingCount > 1) {
            Text(stringResource(Res.string.chat_permission_more, pendingCount - 1), style = MaterialTheme.typography.labelSmall, color = c.textFaint)
        }
    }
}
