package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
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
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_lock_title
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_preview_lock_unlock
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoSessionRow
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTopBar
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private enum class LockStep { Sessions, Locked, Scanning }

/** The app comes back locked: Unlock shows the system prompt, a fingerprint opens the sessions. */
@Composable
internal fun AppLockDemo(active: Boolean) {
    var step by remember { mutableStateOf(LockStep.Sessions) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        step = LockStep.Locked
        finger.hide()
    }) {
        step = LockStep.Sessions
        finger.hide()
        delay(1500)
        step = LockStep.Locked
        delay(1400)
        finger.show(targets.center("unlock"))
        finger.tap()
        finger.hide()
        step = LockStep.Scanning
        delay(1600)
        step = LockStep.Sessions
        delay(2200)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        AnimatedContent(step == LockStep.Sessions, transitionSpec = { fadeIn() togetherWith fadeOut() }) { open ->
            if (open) DemoSessions() else DemoLockScreen(scanning = step == LockStep.Scanning, unlock = targets.target("unlock"))
        }
        finger.Draw()
    }
}

@Composable
private fun DemoSessions() {
    Column(Modifier.fillMaxSize().background(OcTheme.colors.bgBase).padding(OcSpacing.sm)) {
        DemoTopBar("Sessions", subtitle = "my-api")
        DemoSessionRow("Fix flaky RSS test", "my-api", "2m")
        DemoSessionRow("Plan the checkout", "shop", "1h")
        DemoSessionRow("Tidy shell aliases", "dotfiles", "3h")
    }
}

@Composable
private fun DemoLockScreen(scanning: Boolean, unlock: Modifier) {
    val c = OcTheme.colors
    Box(Modifier.fillMaxSize().background(c.bgBase)) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = c.iconBase, modifier = Modifier.size(28.dp))
            Text(stringResource(Res.string.settings_preview_lock_title), style = MaterialTheme.typography.titleSmall, color = c.textBase)
            Text(
                stringResource(Res.string.settings_preview_lock_unlock),
                style = MaterialTheme.typography.labelLarge,
                color = c.onAccent,
                modifier = unlock
                    .background(c.bgAccent, RoundedCornerShape(OcRadius.lg))
                    .padding(horizontal = OcSpacing.lg, vertical = OcSpacing.xs),
            )
        }
        // The system prompt, at the bottom like Android's.
        if (scanning) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(OcSpacing.sm)
                    .background(c.layer02, RoundedCornerShape(OcRadius.xl))
                    .padding(OcSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
            ) {
                Text("Unlock OpenCode", style = MaterialTheme.typography.labelLarge, color = c.textBase)
                Icon(Icons.Outlined.Fingerprint, contentDescription = null, tint = c.textAccent, modifier = Modifier.size(36.dp))
            }
        }
    }
}
