package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcCard
import io.github.beppe2880.opencodemobile.core.ui.components.OcDivider
import io.github.beppe2880.opencodemobile.core.ui.components.OcProjectAvatar
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTopBar
import kotlinx.coroutines.delay

/** Long-press selects a row, a tap adds another, and the menu deletes both after one confirmation. */
@Composable
internal fun SessionBulkActionsDemo(active: Boolean) {
    val c = OcTheme.colors
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var menu by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf(false) }
    var deleted by remember { mutableStateOf(false) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(
        active,
        finalFrame = {
            selected = setOf("a", "b")
            menu = false
            dialog = false
            deleted = false
            finger.hide()
        },
    ) {
        selected = emptySet()
        menu = false
        dialog = false
        deleted = false
        finger.hide()
        delay(800)
        val first = targets.center("row-a")
        finger.show(first + Offset(0f, 120f))
        finger.moveTo(first)
        finger.longPress(650)
        selected = setOf("a")
        delay(600)
        finger.moveTo(targets.center("row-b"))
        finger.tap()
        selected = setOf("a", "b")
        delay(700)
        finger.moveTo(targets.center("menu"))
        finger.tap()
        menu = true
        delay(700)
        finger.moveTo(targets.center("delete"))
        finger.tap()
        menu = false
        dialog = true
        delay(1600)
        finger.moveTo(targets.center("confirm"))
        finger.tap()
        dialog = false
        selected = emptySet()
        deleted = true
        delay(2200)
        finger.hide()
        delay(600)
    }
    Box(Modifier.fillMaxSize().then(targets.root()).background(c.bgDeep)) {
        Column(
            Modifier.fillMaxSize().padding(OcSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
        ) {
            if (selected.isEmpty()) {
                DemoTopBar("Sessions", subtitle = "my-api")
            } else {
                SelectionBar(selected.size) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = null,
                        tint = c.iconBase,
                        modifier = Modifier.size(20.dp).then(targets.target("menu")),
                    )
                }
            }
            if (!deleted) DemoSession("Fix flaky RSS test", "my-api", "2m", "a" in selected, selected.isNotEmpty(), Modifier.then(targets.target("row-a")))
            if (!deleted) DemoSession("Plan the checkout", "shop", "1h", "b" in selected, selected.isNotEmpty(), Modifier.then(targets.target("row-b")))
            DemoSession("Tidy shell aliases", "dotfiles", "3h", checked = false, selectionMode = false)
        }
        if (menu) {
            BulkMenu(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 44.dp, end = OcSpacing.md),
                delete = Modifier.then(targets.target("delete")),
            )
        }
        if (dialog) DeleteDialog(confirm = Modifier.then(targets.target("confirm")))
        finger.Draw()
    }
}

@Composable
private fun SelectionBar(count: Int, trailing: @Composable () -> Unit) {
    val c = OcTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.bgBase)
            .padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Close, contentDescription = null, tint = c.iconBase, modifier = Modifier.size(20.dp))
        Text(
            "$count selected",
            style = MaterialTheme.typography.titleSmall,
            color = c.textBase,
            modifier = Modifier.weight(1f).padding(start = OcSpacing.md),
        )
        trailing()
    }
}

@Composable
private fun BulkMenu(modifier: Modifier = Modifier, delete: Modifier) {
    val c = OcTheme.colors
    Column(
        modifier
            .width(180.dp)
            .clip(RoundedCornerShape(OcRadius.md))
            .background(c.bgBase)
            .border(1.dp, c.borderBase, RoundedCornerShape(OcRadius.md))
            .padding(vertical = OcSpacing.xs),
    ) {
        DemoMenuItem("Pin", Icons.Outlined.PushPin)
        DemoMenuItem("Archive", Icons.Outlined.Archive)
        OcDivider(Modifier.padding(vertical = OcSpacing.xs))
        DemoMenuItem("Delete", Icons.Outlined.Delete, tint = c.danger.fg, modifier = delete)
    }
}

@Composable
private fun DemoMenuItem(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = OcTheme.colors.textBase,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 36.dp).padding(horizontal = OcSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = tint)
    }
}

@Composable
private fun DeleteDialog(confirm: Modifier) {
    val c = OcTheme.colors
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)),
        contentAlignment = Alignment.Center,
    ) {
        OcCard(Modifier.fillMaxWidth(0.86f)) {
            Column(Modifier.padding(OcSpacing.lg), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                Text("Delete 2 sessions?", style = MaterialTheme.typography.titleMedium, color = c.textBase)
                Text(
                    "Their histories will be permanently deleted from the server.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textMuted,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        "Cancel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textMuted,
                        modifier = Modifier.padding(horizontal = OcSpacing.sm, vertical = OcSpacing.xs),
                    )
                    Text(
                        "Delete",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.danger.fg,
                        modifier = confirm.padding(horizontal = OcSpacing.sm, vertical = OcSpacing.xs),
                    )
                }
            }
        }
    }
}

@Composable
private fun DemoSession(
    title: String,
    subtitle: String,
    time: String,
    checked: Boolean,
    selectionMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = OcTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .background(if (checked) c.layer02 else c.bgBase)
            .padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
    ) {
        if (selectionMode) {
            Icon(
                imageVector = if (checked) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (checked) c.textAccent else c.iconMuted,
                modifier = Modifier.size(20.dp),
            )
        } else {
            OcProjectAvatar(subtitle)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = c.textBase, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
        Text(time, style = MaterialTheme.typography.labelSmall, color = c.textFaint)
    }
}
