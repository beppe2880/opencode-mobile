package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayCircleOutline
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.beppe2880.opencodemobile.core.ui.components.OcDivider
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_chat_menu_actions
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_chat_menu_groups
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_chat_menu_title
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTopBar
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ChatMenuDemo(active: Boolean) {
    var open by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf(-1) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    val actions = stringResource(Res.string.tips_c_chat_menu_actions).split('\n')
    val groups = stringResource(Res.string.tips_c_chat_menu_groups).split('\n')
    val icons = listOf(
        Icons.Outlined.Add, Icons.Outlined.AccountTree, Icons.Outlined.Compress,
        Icons.AutoMirrored.Outlined.Undo, Icons.Outlined.PlayCircleOutline, Icons.Outlined.Hub,
    )
    DemoLoop(active, finalFrame = { open = true; selected = -1; finger.hide() }) {
        open = false
        selected = -1
        delay(700)
        finger.show(targets.center("menu"))
        finger.tap()
        open = true
        delay(800)
        finger.moveTo(targets.center("action-1"))
        finger.tap()
        selected = 1
        delay(1800)
        finger.hide()
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        DemoTopBar(stringResource(Res.string.tips_c_chat_menu_title), actions = {
            Icon(
                Icons.Outlined.MoreVert, null, tint = OcTheme.colors.iconBase,
                modifier = Modifier.size(24.dp).then(targets.target("menu")),
            )
        })
        if (open) Column(
            Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = OcSpacing.md).widthIn(max = 270.dp)
                .background(OcTheme.colors.layer02, RoundedCornerShape(OcRadius.md)).padding(OcSpacing.sm),
        ) {
            actions.forEachIndexed { index, action ->
                if (index > 0 && index % 2 == 0) {
                    Row(
                        Modifier.padding(vertical = OcSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
                    ) {
                        OcDivider(Modifier.weight(1f))
                        Text(
                            groups[index / 2], color = OcTheme.colors.textFaint,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
                        )
                        OcDivider(Modifier.weight(1f))
                    }
                }
                Row(
                    Modifier.then(targets.target("action-$index"))
                        .background(if (selected == index) OcTheme.colors.layer03 else OcTheme.colors.layer02).padding(OcSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icons[index], null, tint = OcTheme.colors.iconBase, modifier = Modifier.size(18.dp))
                    Text(
                        action, color = OcTheme.colors.textBase, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = OcSpacing.sm),
                    )
                }
            }
        }
        finger.Draw()
    }
}
