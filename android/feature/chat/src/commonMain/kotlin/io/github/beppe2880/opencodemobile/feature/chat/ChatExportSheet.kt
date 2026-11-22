package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.beppe2880.opencodemobile.core.ui.components.OcBottomSheet
import io.github.beppe2880.opencodemobile.core.ui.components.OcListRow
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_export
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_export_save
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_export_share
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ChatExportSheet(state: ChatState, onEvent: (ChatEvent) -> Unit) {
    if (state.exportOpen) OcBottomSheet(onDismiss = { onEvent(ChatEvent.ExportSheet(false)) }, testTag = "chat_export_sheet") {
        Column(Modifier.padding(OcSpacing.lg)) {
            Text(stringResource(Res.string.chat_export), style = MaterialTheme.typography.titleMedium, color = OcTheme.colors.textBase)
            if (state.exporting) OcSpinner() else {
                OcListRow(
                    title = stringResource(Res.string.chat_export_share),
                    leading = { Icon(Icons.Outlined.Share, null) },
                    onClick = { onEvent(ChatEvent.Export(save = false)) },
                    modifier = Modifier.testTag("chat_export_share"),
                )
                OcListRow(
                    title = stringResource(Res.string.chat_export_save),
                    leading = { Icon(Icons.Outlined.SaveAlt, null) },
                    onClick = { onEvent(ChatEvent.Export(save = true)) },
                    modifier = Modifier.testTag("chat_export_save"),
                )
            }
        }
    }
}
