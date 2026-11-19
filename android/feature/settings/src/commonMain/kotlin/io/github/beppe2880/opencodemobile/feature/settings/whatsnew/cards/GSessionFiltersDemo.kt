package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcBadge
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcCard
import io.github.beppe2880.opencodemobile.core.ui.components.OcChip
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTopBar
import kotlinx.coroutines.delay

/** Opens the filter sheet, selects archived + a date + an outcome, and shows the archived row. */
@Composable
internal fun SessionFiltersDemo(active: Boolean) {
    val c = OcTheme.colors
    var sheet by remember { mutableStateOf(false) }
    var archived by remember { mutableStateOf(false) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(
        active,
        finalFrame = {
            sheet = false
            archived = false
            finger.hide()
        },
    ) {
        sheet = false
        archived = false
        finger.hide()
        delay(700)
        finger.show(targets.center("filter") + Offset(0f, 120f))
        finger.moveTo(targets.center("filter"))
        finger.tap()
        sheet = true
        delay(1800)
        sheet = false
        archived = true
        finger.hide()
        delay(2400)
    }
    Box(Modifier.fillMaxSize().then(targets.root()).background(c.bgDeep)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(OcSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
        ) {
            DemoTopBar("Sessions", subtitle = "my-api")
            Row(
                modifier = Modifier.padding(horizontal = OcSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OcChip("All projects", {}, selected = true)
                Spacer(Modifier.weight(1f))
                Box {
                    OcIconButton(
                        icon = Icons.Outlined.FilterAlt,
                        contentDescription = "Filters",
                        onClick = {},
                        tint = if (archived) c.textAccent else c.iconMuted,
                        modifier = Modifier.then(targets.target("filter")),
                    )
                    if (archived) OcBadge("1", c.info, Modifier.align(Alignment.TopEnd))
                }
            }
            OcCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = OcSpacing.sm),
                shape = RoundedCornerShape(OcRadius.lg),
            ) {
                Row(Modifier.padding(OcSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (archived) Icons.Outlined.Archive else Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint = c.iconMuted,
                        modifier = Modifier.size(18.dp),
                    )
                    Column(Modifier.weight(1f).padding(start = OcSpacing.sm)) {
                        Text("Refactor auth", style = MaterialTheme.typography.bodyMedium, color = c.textBase)
                        Text(
                            if (archived) "my-api · archived" else "my-api · 2h",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textMuted,
                        )
                    }
                }
            }
            if (archived) {
                Row(
                    modifier = Modifier.padding(horizontal = OcSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
                ) {
                    OcChip("Archived", {}, selected = true)
                    OcChip("Last 7 days", {}, selected = true)
                    OcChip("Failed", {}, selected = true)
                }
            }
        }
        if (sheet) {
            OcCard(
                modifier = Modifier.align(Alignment.Center).padding(horizontal = OcSpacing.lg).fillMaxWidth(),
                shape = RoundedCornerShape(OcRadius.lg),
            ) {
                Column(Modifier.padding(OcSpacing.md), verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                    Text("Filters", style = MaterialTheme.typography.titleSmall, color = c.textBase)
                    Row(horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                        OcChip("Active", {}, selected = false)
                        OcChip("Archived", {}, selected = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                        OcChip("Last 7 days", {}, selected = true)
                        OcChip("Failed", {}, selected = true)
                    }
                    OcButton(
                        text = "Show sessions",
                        onClick = {},
                        modifier = Modifier.fillMaxWidth().padding(top = OcSpacing.xs),
                        compact = true,
                    )
                }
            }
        }
        finger.Draw()
    }
}
