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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcSectionHeader
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.star.GitHubStarBanner
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoSessionRow
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import kotlinx.coroutines.delay

/** The star invitation above the sessions: tapping it opens GitHub and it thanks; the X closes it for good. */
@Composable
internal fun GitHubStarDemo(active: Boolean) {
    val c = OcTheme.colors
    var banner by remember { mutableStateOf(true) }
    var starred by remember { mutableStateOf(false) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val closeInset = with(LocalDensity.current) { 24.dp.toPx() }
    DemoLoop(
        active,
        finalFrame = {
            banner = true
            starred = false
            finger.hide()
        },
    ) {
        banner = true
        starred = false
        finger.hide()
        delay(1400)
        finger.show(targets.center("banner") + Offset(0f, 60f))
        finger.moveTo(targets.center("banner"))
        finger.tap()
        starred = true
        delay(1400)
        // The X at the end of the banner (the start in right-to-left languages).
        val box = targets.bounds("banner")
        if (box != null) finger.moveTo(Offset(if (rtl) box.left + closeInset else box.right - closeInset, box.center.y))
        finger.tap()
        banner = false
        finger.hide()
        delay(2000)
    }
    Box(Modifier.fillMaxSize().then(targets.root()).background(c.bgDeep)) {
        Column(Modifier.fillMaxSize().padding(top = OcSpacing.sm)) {
            AnimatedVisibility(banner, enter = fadeIn(), exit = shrinkVertically() + fadeOut()) {
                GitHubStarBanner(
                    thanked = starred,
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
