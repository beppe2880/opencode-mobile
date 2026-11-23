package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.beppe2880.opencodemobile.core.ui.components.OcListRow
import io.github.beppe2880.opencodemobile.core.ui.components.OcPulsingDot
import io.github.beppe2880.opencodemobile.core.ui.components.SessionMuteChoices
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_mute_duration
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import io.github.beppe2880.opencodemobile.core.ui.resources.Res as UiRes

@Composable
internal fun SessionMuteDemo(active: Boolean) {
    var stage by remember { mutableIntStateOf(0) }
    DemoLoop(active, finalFrame = { stage = 1 }) {
        stage = 0
        delay(900)
        stage = 1
        delay(1800)
        stage = 2
        delay(1500)
        stage = 3
        delay(1100)
    }
    Column(Modifier.fillMaxWidth()) {
        OcListRow(
            title = "Run the tests",
            leading = { if (stage != 3) OcPulsingDot(OcTheme.colors.success.fg) },
            trailing = { if (stage == 2) Icon(Icons.Outlined.NotificationsOff, null, tint = OcTheme.colors.iconBase) },
        )
        when (stage) {
            0 -> OcListRow(title = stringResource(UiRes.string.oc_mute_duration), leading = { Icon(Icons.Outlined.NotificationsOff, null) })
            1 -> SessionMuteChoices(running = true, onMute = {})
        }
    }
}
