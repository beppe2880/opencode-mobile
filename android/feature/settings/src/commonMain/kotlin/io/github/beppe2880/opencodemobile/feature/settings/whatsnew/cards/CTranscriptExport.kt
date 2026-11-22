package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.beppe2880.opencodemobile.core.ui.components.OcListRow
import io.github.beppe2880.opencodemobile.core.ui.components.OcSegment
import io.github.beppe2880.opencodemobile.core.ui.components.OcSegmentedControl
import io.github.beppe2880.opencodemobile.core.ui.text.formatInteger
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_export_cached
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_export_downloading
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_export_file
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_export_markdown
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_export_messages
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_export_panel_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_export_percent
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_export_processing
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_export_ready
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_export_save
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_export_share
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_export_waiting
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private enum class ExportDemoStep { Choose, Waiting, Downloading, Processing, Messages, Markdown, File, Ready }

@Composable
internal fun TranscriptExportDemo(active: Boolean) {
    var json by remember { mutableStateOf(false) }
    var step by remember { mutableStateOf(ExportDemoStep.Choose) }
    var fraction by remember { mutableStateOf<Float?>(null) }
    var cached by remember { mutableStateOf(false) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }

    suspend fun measured(next: ExportDemoStep, pause: Long = 100) {
        step = next
        for (percent in 0..100 step 20) {
            fraction = percent / 100f
            delay(pause)
        }
    }

    DemoLoop(active, finalFrame = {
        json = true
        step = ExportDemoStep.Choose
        fraction = null
        cached = false
        finger.hide()
    }) {
        json = false
        cached = false
        step = ExportDemoStep.Choose
        fraction = null
        delay(600)
        finger.show(targets.center("share"))
        finger.tap()
        step = ExportDemoStep.Waiting
        finger.hide()
        delay(600)
        measured(ExportDemoStep.Downloading)
        step = ExportDemoStep.Processing
        fraction = null
        delay(500)
        measured(ExportDemoStep.Messages)
        measured(ExportDemoStep.Markdown)
        measured(ExportDemoStep.File, 70)
        step = ExportDemoStep.Ready
        delay(900)

        // Same native JSON, another format: demonstrate a cache hit without another download.
        step = ExportDemoStep.Choose
        fraction = null
        cached = true
        delay(400)
        finger.show(targets.center("formats"))
        finger.tap()
        json = true
        delay(500)
        finger.moveTo(targets.center("share"))
        finger.tap()
        step = ExportDemoStep.File
        finger.hide()
        measured(ExportDemoStep.File, 50)
        step = ExportDemoStep.Ready
        delay(1500)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Column(Modifier.padding(OcSpacing.md)) {
            Text(stringResource(Res.string.tips_export_panel_title), style = MaterialTheme.typography.titleSmall, color = OcTheme.colors.textBase)
            if (step == ExportDemoStep.Choose) {
                OcSegmentedControl(
                    segments = listOf(OcSegment(false, "Markdown", "demo_export_markdown"), OcSegment(true, "JSON", "demo_export_json")),
                    selected = json,
                    onSelect = { json = it },
                    modifier = Modifier.padding(vertical = OcSpacing.sm).then(targets.target("formats")),
                )
                OcListRow(
                    title = stringResource(Res.string.tips_export_share, if (json) "JSON" else "Markdown"),
                    leading = { Icon(Icons.Outlined.Share, null) },
                    onClick = {},
                    modifier = Modifier.then(targets.target("share")),
                )
                OcListRow(
                    title = stringResource(Res.string.tips_export_save, if (json) "JSON" else "Markdown"),
                    leading = { Icon(Icons.Outlined.SaveAlt, null) },
                    onClick = {},
                    modifier = Modifier.testTag("demo_export_save"),
                )
            } else {
                // Explanatory demo caption, not a cache notification in the real export sheet.
                if (cached) Text(
                    stringResource(Res.string.tips_export_cached),
                    style = MaterialTheme.typography.labelSmall,
                    color = OcTheme.colors.textMuted,
                    modifier = Modifier.padding(top = OcSpacing.md),
                )
                if (step == ExportDemoStep.Ready) {
                    Row(
                        Modifier.padding(top = OcSpacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Description, null, tint = OcTheme.colors.iconBase)
                        Column {
                            Text(stringResource(Res.string.tips_export_ready), style = MaterialTheme.typography.bodyMedium, color = OcTheme.colors.textBase)
                            Text("OpenCode.${if (json) "json" else "md"}", style = MaterialTheme.typography.bodySmall, color = OcTheme.colors.textMuted)
                        }
                    }
                } else {
                    val phase = when (step) {
                        ExportDemoStep.Waiting -> Res.string.tips_export_waiting
                        ExportDemoStep.Downloading -> Res.string.tips_export_downloading
                        ExportDemoStep.Processing -> Res.string.tips_export_processing
                        ExportDemoStep.Messages -> Res.string.tips_export_messages
                        ExportDemoStep.Markdown -> Res.string.tips_export_markdown
                        else -> Res.string.tips_export_file
                    }
                    val progress = fraction
                    Row(
                        Modifier.fillMaxWidth().padding(top = OcSpacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(phase),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OcTheme.colors.textBase,
                            modifier = Modifier.weight(1f),
                        )
                        if (progress != null) Text(
                            stringResource(Res.string.tips_export_percent, formatInteger((progress * 100).toLong())),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OcTheme.colors.textBase,
                        )
                    }
                    val bar = Modifier.fillMaxWidth().padding(top = OcSpacing.md).testTag("demo_export_progress")
                    if (progress == null) LinearProgressIndicator(modifier = bar) else LinearProgressIndicator(progress = { progress }, modifier = bar)
                }
            }
        }
        finger.Draw()
    }
}
