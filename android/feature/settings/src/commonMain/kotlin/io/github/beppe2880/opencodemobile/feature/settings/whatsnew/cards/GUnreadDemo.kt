package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcCard
import io.github.beppe2880.opencodemobile.core.ui.components.OcProjectAvatar
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTopBar
import kotlinx.coroutines.delay

/** A turn finishes and marks its row, then opening the session clears the dot. */
@Composable
internal fun UnreadSessionsDemo(active: Boolean) {
    val c = OcTheme.colors
    var unread by remember { mutableStateOf(true) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(
        active,
        finalFrame = {
            unread = true
            finger.hide()
        },
    ) {
        unread = true
        finger.hide()
        delay(900)
        finger.show(targets.center("row") + Offset(0f, 60f))
        finger.moveTo(targets.center("row"))
        finger.tap()
        unread = false
        finger.hide()
        delay(1800)
    }
    Box(Modifier.fillMaxSize().then(targets.root()).background(c.bgDeep)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(OcSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
        ) {
            DemoTopBar("Sessions", subtitle = "my-api")
            DemoSessionRow(
                project = "blog",
                title = "Fix flaky RSS test",
                subtitle = "blog · 2m",
                unread = unread,
                modifier = Modifier.then(targets.target("row")),
            )
            DemoSessionRow("dotfiles", "Tidy shell aliases", "dotfiles · 1h", unread = false)
            DemoSessionRow("shop", "Plan the checkout", "shop · 3h", unread = false)
        }
        finger.Draw()
    }
}

@Composable
private fun DemoSessionRow(
    project: String,
    title: String,
    subtitle: String,
    unread: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = OcTheme.colors
    OcCard(
        modifier = modifier.fillMaxWidth().padding(horizontal = OcSpacing.sm),
        shape = RoundedCornerShape(OcRadius.lg),
    ) {
        Row(Modifier.padding(OcSpacing.md), verticalAlignment = Alignment.CenterVertically) {
            Box {
                OcProjectAvatar(project)
                if (unread) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(c.info.fg),
                    )
                }
            }
            Column(Modifier.weight(1f).padding(start = OcSpacing.sm)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = c.textBase)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            }
        }
    }
}
