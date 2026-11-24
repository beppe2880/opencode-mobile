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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.sp
import io.github.beppe2880.opencodemobile.core.ui.components.OcDivider
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTopBar
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoUserBubble
import kotlinx.coroutines.delay

/**
 * The chat's three-dot menu gains Rename chat and, under a divider, the destructive
 * Delete chat; their dialogs work on the open session.
 */
@Composable
internal fun ChatActionsDemo(active: Boolean) {
    val c = OcTheme.colors
    var menu by remember { mutableStateOf(false) }
    var dialog by remember { mutableIntStateOf(0) } // 0 none, 1 rename, 2 delete
    var title by remember { mutableStateOf("Fix flaky RSS test") }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(
        active,
        finalFrame = {
            menu = false
            dialog = 1
            title = "Fix flaky RSS test"
            finger.hide()
        },
    ) {
        menu = false
        dialog = 0
        title = "Fix flaky RSS test"
        delay(900)
        finger.show(targets.center("more") + Offset(0f, 120f))
        finger.moveTo(targets.center("more"))
        finger.tap()
        menu = true
        delay(700)
        finger.moveTo(targets.center("rename"))
        finger.tap()
        menu = false
        dialog = 1
        delay(1700)
        finger.hide()
        delay(350)
        finger.show(targets.center("save") + Offset(0f, 90f))
        finger.moveTo(targets.center("save"))
        finger.tap()
        dialog = 0
        title = "Flaky RSS test fixed"
        delay(600)
        finger.moveTo(targets.center("more"))
        finger.tap()
        menu = true
        delay(700)
        finger.moveTo(targets.center("delete"))
        finger.tap()
        menu = false
        dialog = 2
        delay(2300)
        finger.hide()
        delay(500)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Column(Modifier.fillMaxSize()) {
            DemoTopBar(
                title = title,
                subtitle = "my-api",
                actions = {
                    Box(
                        Modifier.size(32.dp).then(targets.target("more")),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = null, tint = c.iconBase, modifier = Modifier.size(20.dp))
                    }
                },
            )
            Column(
                Modifier.fillMaxWidth().padding(OcSpacing.md),
                verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
            ) {
                DemoUserBubble("The RSS test is flaky, can you fix it?")
                Text("Looking at the test now.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
            }
        }
        if (menu) {
            Column(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 42.dp, end = OcSpacing.sm)
                    .width(164.dp)
                    .clip(RoundedCornerShape(OcRadius.md))
                    .background(c.bgBase)
                    .border(1.dp, c.borderBase, RoundedCornerShape(OcRadius.md))
                    .padding(vertical = OcSpacing.xs),
            ) {
                HMenuItem("Rename chat", Icons.Outlined.DriveFileRenameOutline, Modifier.then(targets.target("rename")))
                HMenuItem("Mute notifications", Icons.Outlined.NotificationsOff)
                HMenuItem("Pin", Icons.Outlined.PushPin)
                HMenuItem("Archive", Icons.Outlined.Archive)
                HMenuGroup("Context & history")
                HMenuItem("Compact", Icons.Outlined.Compress)
                HMenuGroup("Tools")
                HMenuItem("Running", Icons.Outlined.PlayCircleOutline)
                OcDivider(Modifier.padding(vertical = OcSpacing.xs))
                HMenuItem("Delete chat", Icons.Outlined.Delete, Modifier.then(targets.target("delete")), tint = c.danger.fg)
            }
        }
        if (dialog != 0) HDialog(rename = dialog == 1, title = title, targets = targets)
        finger.Draw()
    }
}

@Composable
private fun HMenuItem(
    text: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    tint: Color = OcTheme.colors.textBase,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 17.dp).padding(horizontal = OcSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 11.sp),
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HMenuGroup(label: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = OcSpacing.md, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
    ) {
        OcDivider(Modifier.weight(1f))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 9.sp, letterSpacing = 0.sp),
            color = OcTheme.colors.textFaint,
            maxLines = 1,
        )
        OcDivider(Modifier.weight(1f))
    }
}

@Composable
private fun HDialog(rename: Boolean, title: String, targets: DemoTargets) {
    val c = OcTheme.colors
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.32f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(horizontal = OcSpacing.lg)
                .widthIn(max = 290.dp)
                .clip(RoundedCornerShape(OcRadius.xl))
                .background(c.bgBase)
                .padding(OcSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(OcSpacing.md),
        ) {
            Text(
                if (rename) "Rename chat" else "Delete chat?",
                style = MaterialTheme.typography.titleMedium,
                color = c.textBase,
            )
            if (rename) {
                Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                    Text("Title", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(OcRadius.md))
                            .border(1.dp, c.borderFocus, RoundedCornerShape(OcRadius.md))
                            .padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm),
                    ) {
                        Text(title, style = MaterialTheme.typography.bodyMedium, color = c.textBase)
                    }
                }
            } else {
                Text(
                    "“$title” and its history will be permanently deleted from the server.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textMuted,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge, color = c.textMuted, modifier = Modifier.padding(OcSpacing.sm))
                if (rename) {
                    Text(
                        "Save",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.textAccent,
                        modifier = Modifier.padding(OcSpacing.sm).then(targets.target("save")),
                    )
                } else {
                    Text("Delete", style = MaterialTheme.typography.labelLarge, color = c.danger.fg, modifier = Modifier.padding(OcSpacing.sm))
                }
            }
        }
    }
}
