package io.github.beppe2880.opencodemobile.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.R
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme

/** Centered icon + title + message with an optional action. */
@Composable
fun OcEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val c = OcTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = OcSpacing.xl, vertical = OcSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OcSpacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(OcRadius.lg))
                .background(c.layer02),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(24.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = c.textBase,
            textAlign = TextAlign.Center,
        )
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = c.textMuted,
                textAlign = TextAlign.Center,
            )
        }
        action?.invoke()
    }
}

@Composable
fun OcErrorState(
    title: String,
    message: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryTestTag: String = "error_retry_button",
) {
    OcEmptyState(
        icon = Icons.Outlined.CloudOff,
        title = title,
        message = message,
        modifier = modifier,
        action = {
            OcButton(
                text = stringResource(R.string.oc_retry),
                onClick = onRetry,
                variant = OcButtonVariant.Secondary,
                leadingIcon = Icons.Outlined.Refresh,
                compact = true,
                modifier = Modifier.testTag(retryTestTag),
            )
        },
    )
}

/** Confirmation dialog; [destructive] colors the confirm action as danger. */
@Composable
fun OcConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    testTagPrefix: String = "confirm",
    dismissLabel: String? = null,
) {
    val c = OcTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.exposeTestTags().testTag("${testTagPrefix}_dialog"),
        containerColor = c.bgBase,
        titleContentColor = c.textBase,
        textContentColor = c.textMuted,
        shape = RoundedCornerShape(OcRadius.xl),
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag("${testTagPrefix}_confirm_button")) {
                Text(confirmLabel, color = if (destructive) c.danger.fg else c.textAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("${testTagPrefix}_cancel_button")) {
                Text(dismissLabel ?: stringResource(R.string.oc_cancel), color = c.textMuted)
            }
        },
    )
}

/** Single text field dialog (rename flows). */
@Composable
fun OcTextInputDialog(
    title: String,
    initialValue: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    testTagPrefix: String,
    label: String? = null,
) {
    val c = OcTheme.colors
    val value = androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.exposeTestTags().testTag("${testTagPrefix}_dialog"),
        containerColor = c.bgBase,
        titleContentColor = c.textBase,
        shape = RoundedCornerShape(OcRadius.xl),
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = {
            OcTextField(
                value = value.value,
                onValueChange = { value.value = it },
                label = label,
                modifier = Modifier.testTag("${testTagPrefix}_field"),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value.value.trim()) },
                enabled = value.value.isNotBlank(),
                modifier = Modifier.testTag("${testTagPrefix}_confirm_button"),
            ) { Text(confirmLabel, color = if (value.value.isNotBlank()) c.textAccent else c.textFaint) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("${testTagPrefix}_cancel_button")) {
                Text(stringResource(R.string.oc_cancel), color = c.textMuted)
            }
        },
    )
}
