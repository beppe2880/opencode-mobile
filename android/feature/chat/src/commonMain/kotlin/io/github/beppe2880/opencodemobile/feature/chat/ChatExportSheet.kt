package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import io.github.beppe2880.opencodemobile.core.ui.components.OcBottomSheet
import io.github.beppe2880.opencodemobile.core.ui.components.OcListRow
import io.github.beppe2880.opencodemobile.core.ui.components.OcSegment
import io.github.beppe2880.opencodemobile.core.ui.components.OcSegmentedControl
import io.github.beppe2880.opencodemobile.core.ui.text.formatInteger
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_export
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_export_decoding
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_export_downloading
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_export_json
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_export_json_save
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_export_json_share
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_export_markdown
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_export_percent
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_export_preparing_file
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_export_preparing_markdown
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_export_preparing_messages
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_export_save
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_export_share
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_export_waiting_server
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ChatExportSheet(state: ChatState, onEvent: (ChatEvent) -> Unit) {
    if (state.exportOpen) OcBottomSheet(onDismiss = { onEvent(ChatEvent.ExportSheet(false)) }, testTag = "chat_export_sheet") {
        Column(Modifier.padding(OcSpacing.lg)) {
            Text(stringResource(Res.string.chat_export), style = MaterialTheme.typography.titleMedium, color = OcTheme.colors.textBase)
            if (state.exporting) {
                Column(Modifier.fillMaxWidth().padding(vertical = OcSpacing.lg)) {
                    val fraction = state.exportFraction
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OcSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(
                                when (state.exportPhase) {
                                    ExportPhase.WaitingForServer -> Res.string.chat_export_waiting_server
                                    ExportPhase.Downloading -> Res.string.chat_export_downloading
                                    ExportPhase.Decoding -> Res.string.chat_export_decoding
                                    ExportPhase.PreparingMessages -> Res.string.chat_export_preparing_messages
                                    ExportPhase.PreparingMarkdown -> Res.string.chat_export_preparing_markdown
                                    ExportPhase.PreparingFile -> Res.string.chat_export_preparing_file
                                },
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OcTheme.colors.textBase,
                            modifier = Modifier.weight(1f).testTag("chat_export_phase").semantics { liveRegion = LiveRegionMode.Polite },
                        )
                        if (fraction != null) Text(
                            stringResource(Res.string.chat_export_percent, formatInteger((fraction * 100).toLong())),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OcTheme.colors.textBase,
                            modifier = Modifier.testTag("chat_export_percent"),
                        )
                    }
                    if (fraction == null) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().padding(top = OcSpacing.md).testTag("chat_export_progress"),
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxWidth().padding(top = OcSpacing.md).testTag("chat_export_progress"),
                        )
                    }
                }
            } else {
                val json = state.exportFormat == TranscriptFormat.Json
                OcSegmentedControl(
                    segments = listOf(
                        OcSegment(TranscriptFormat.Markdown, stringResource(Res.string.chat_export_markdown), "chat_export_format_markdown"),
                        OcSegment(TranscriptFormat.Json, stringResource(Res.string.chat_export_json), "chat_export_format_json"),
                    ),
                    selected = state.exportFormat,
                    onSelect = { onEvent(ChatEvent.SelectExportFormat(it)) },
                    modifier = Modifier.padding(vertical = OcSpacing.md),
                )
                OcListRow(
                    title = stringResource(if (json) Res.string.chat_export_json_share else Res.string.chat_export_share),
                    leading = { Icon(Icons.Outlined.Share, null) },
                    onClick = { onEvent(ChatEvent.Export(save = false)) },
                    modifier = Modifier.testTag("chat_export_share"),
                )
                OcListRow(
                    title = stringResource(if (json) Res.string.chat_export_json_save else Res.string.chat_export_save),
                    leading = { Icon(Icons.Outlined.SaveAlt, null) },
                    onClick = { onEvent(ChatEvent.Export(save = true)) },
                    modifier = Modifier.testTag("chat_export_save"),
                )
            }
        }
    }
}
