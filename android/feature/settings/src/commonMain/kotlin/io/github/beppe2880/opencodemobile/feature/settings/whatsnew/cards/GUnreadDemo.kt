package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcSectionHeader
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoSessionRow
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import kotlinx.coroutines.delay

/**
 * A turn finishes and its row gets a dot and moves to the unread group; opening it clears the dot
 * and returns it to Today.
 */
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
        unread = false
        finger.hide()
        delay(900)
        unread = true
        delay(1600)
        finger.show(targets.center("row") + Offset(0f, 60f))
        finger.moveTo(targets.center("row"))
        finger.tap()
        unread = false
        finger.hide()
        delay(1800)
    }
    Box(Modifier.fillMaxSize().then(targets.root()).background(c.bgDeep)) {
        Column(Modifier.fillMaxSize().padding(top = OcSpacing.xs)) {
            AnimatedVisibility(unread, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column {
                    OcSectionHeader("Unread", Modifier.padding(horizontal = OcSpacing.md))
                    DemoSessionRow(
                        title = "Fix flaky RSS test",
                        subtitle = "blog",
                        time = "2m",
                        modifier = Modifier.then(targets.target("row")),
                        trailing = { UnreadDot() },
                    )
                }
            }
            OcSectionHeader("Today", Modifier.padding(horizontal = OcSpacing.md))
            AnimatedVisibility(!unread, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                DemoSessionRow("Fix flaky RSS test", "blog", "2m")
            }
            DemoSessionRow("Tidy shell aliases", "dotfiles", "1h")
        }
        finger.Draw()
    }
}

@Composable
private fun UnreadDot() {
    Box(
        Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(OcTheme.colors.info.fg),
    )
}
