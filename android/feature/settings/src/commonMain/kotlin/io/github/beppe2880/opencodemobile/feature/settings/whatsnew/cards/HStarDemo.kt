package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import io.github.beppe2880.opencodemobile.core.ui.components.OcSectionHeader
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.star.GitHubStarBanner
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoSessionRow
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import kotlinx.coroutines.delay

/** The star invitation above the sessions: tapping it opens GitHub and it goes away for good. */
@Composable
internal fun GitHubStarDemo(active: Boolean) {
    val c = OcTheme.colors
    var banner by remember { mutableStateOf(true) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(
        active,
        finalFrame = {
            banner = true
            finger.hide()
        },
    ) {
        banner = true
        finger.hide()
        delay(1400)
        finger.show(targets.center("banner") + Offset(0f, 60f))
        finger.moveTo(targets.center("banner"))
        finger.tap()
        banner = false
        finger.hide()
        delay(2000)
    }
    Box(Modifier.fillMaxSize().then(targets.root()).background(c.bgDeep)) {
        Column(Modifier.fillMaxSize().padding(top = OcSpacing.sm)) {
            AnimatedVisibility(banner, enter = fadeIn(), exit = shrinkVertically() + fadeOut()) {
                GitHubStarBanner(
                    onEvent = {},
                    modifier = Modifier.padding(start = OcSpacing.md, end = OcSpacing.md, bottom = OcSpacing.sm).then(targets.target("banner")),
                )
            }
            OcSectionHeader("Today", Modifier.padding(horizontal = OcSpacing.md))
            DemoSessionRow("Fix flaky RSS test", "blog", "2m")
            DemoSessionRow("Tidy shell aliases", "dotfiles", "1h")
        }
        finger.Draw()
    }
}
