package io.github.beppe2880.opencodemobile.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.SharedContent
import io.github.beppe2880.opencodemobile.core.ui.resources.Res
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_shared_images
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_shared_skipped
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_shared_title
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_shared_truncated
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * What another app shared, waiting for a chat: the start of the text, how many images, and
 * what was left out. [message] says what happens next; [actions] go below it.
 */
@Composable
fun OcSharedContent(
    content: SharedContent,
    modifier: Modifier = Modifier,
    message: String? = null,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    val c = OcTheme.colors
    OcCard(modifier = modifier.fillMaxWidth().testTag("shared_content"), color = c.layer01) {
        Column(Modifier.padding(OcSpacing.md), verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                Icon(Icons.Outlined.Share, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(16.dp))
                Text(stringResource(Res.string.oc_shared_title), style = MaterialTheme.typography.labelMedium, color = c.textMuted)
            }
            if (content.text.isNotBlank()) {
                Text(
                    content.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textBase,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("shared_content_text"),
                )
            }
            if (content.attachments.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                    Icon(Icons.Outlined.Image, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(16.dp))
                    Text(
                        pluralStringResource(Res.plurals.oc_shared_images, content.attachments.size, content.attachments.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textBase,
                        modifier = Modifier.testTag("shared_content_images"),
                    )
                }
            }
            if (content.skipped > 0) {
                Text(
                    pluralStringResource(Res.plurals.oc_shared_skipped, content.skipped, content.skipped),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.warning.fg,
                )
            }
            if (content.truncated) {
                Text(stringResource(Res.string.oc_shared_truncated), style = MaterialTheme.typography.bodySmall, color = c.warning.fg)
            }
            if (message != null) {
                Text(message, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            }
            actions()
        }
    }
}
