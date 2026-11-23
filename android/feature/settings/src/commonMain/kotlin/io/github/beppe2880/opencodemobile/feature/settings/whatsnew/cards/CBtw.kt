package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcChip
import io.github.beppe2880.opencodemobile.core.ui.components.OcPulsingDot
import io.github.beppe2880.opencodemobile.core.ui.markdown.rememberMarkdownCache
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_btw_answer
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_btw_labels
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_btw_question
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_c_btw_working
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoAssistantText
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * While the agent works, a question typed in the box goes aside instead of steering it; the
 * chip above the box tracks it and the answer opens in its own panel, outside the chat.
 */
@Composable
internal fun BtwDemo(active: Boolean) {
    val c = OcTheme.colors
    val cache = rememberMarkdownCache()
    // Steer now, Queue, Ask aside, Preparing answer…, Answer ready, Side question, Copy, Close
    val labels = stringResource(Res.string.tips_c_btw_labels).split('\n')
    val question = stringResource(Res.string.tips_c_btw_question)
    val answer = stringResource(Res.string.tips_c_btw_answer)
    var text by remember { mutableStateOf("") }
    var aside by remember { mutableStateOf(false) }
    // 0 nothing asked, 1 waiting for the answer, 2 answer ready, 3 panel open
    var phase by remember { mutableIntStateOf(3) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        text = ""
        aside = false
        phase = 3
        finger.hide()
    }) {
        text = ""
        aside = false
        phase = 0
        finger.hide()
        delay(700)
        finger.show(targets.center("input"))
        finger.tap()
        fType(question) { text = it }
        delay(400)
        finger.moveTo(targets.center("aside"))
        finger.tap()
        aside = true
        delay(600)
        finger.moveTo(targets.center("send"))
        finger.tap()
        text = ""
        aside = false
        phase = 1
        finger.hide()
        delay(1800)
        phase = 2
        delay(700)
        finger.show(targets.center("status"))
        finger.tap()
        phase = 3
        finger.hide()
        delay(3500)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Column(
            Modifier.fillMaxSize().padding(OcSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(OcSpacing.xs, Alignment.Bottom),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                OcPulsingDot(color = c.agents.build)
                Text(stringResource(Res.string.tips_c_btw_working), style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            }
            FVisible(phase == 1 || phase == 2) {
                OcChip(
                    "/btw · " + labels[if (phase == 1) 3 else 4],
                    onClick = {},
                    modifier = Modifier.then(targets.target("status")),
                )
            }
            FComposer(text, targets) {
                FVisible(text.isNotBlank()) {
                    Row(Modifier.padding(bottom = OcSpacing.xs), horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                        OcChip(labels[0], {}, selected = !aside)
                        OcChip(labels[1], {})
                        OcChip(labels[2], {}, Modifier.then(targets.target("aside")), selected = aside)
                    }
                }
            }
        }
        if (phase == 3) {
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(c.layer02, RoundedCornerShape(topStart = OcRadius.lg, topEnd = OcRadius.lg))
                    .padding(OcSpacing.md),
                verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
            ) {
                Text("${labels[5]} · /btw", style = MaterialTheme.typography.titleSmall, color = c.textBase)
                Text(question, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                DemoAssistantText("btw-demo", answer, cache)
                Row(horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                    OcButton(text = labels[6], onClick = {}, modifier = Modifier.weight(1f))
                    OcButton(text = labels[7], onClick = {}, variant = OcButtonVariant.Secondary, modifier = Modifier.weight(1f))
                }
            }
        }
        finger.Draw()
    }
}
