package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.markdown.rememberMarkdownCache
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_j_hide_keyboard_answer
import io.github.beppe2880.opencodemobile.feature.settings.resources.tips_j_hide_keyboard_prompt
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoAssistantText
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoUserBubble
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * A prompt is typed with the keyboard open and sent: the keyboard slides away and the answer
 * streams in with the whole height for itself.
 */
@Composable
internal fun HideKeyboardDemo(active: Boolean) {
    val cache = rememberMarkdownCache()
    val prompt = stringResource(Res.string.tips_j_hide_keyboard_prompt)
    val answer = stringResource(Res.string.tips_j_hide_keyboard_answer)
    var text by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(true) }
    var keyboard by remember { mutableStateOf(false) }
    var shown by remember { mutableIntStateOf(answer.length) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        text = ""
        sent = true
        keyboard = false
        shown = answer.length
        finger.hide()
    }) {
        text = ""
        sent = false
        keyboard = false
        shown = 0
        finger.hide()
        delay(600)
        finger.show(targets.center("input"))
        finger.tap()
        keyboard = true
        finger.hide()
        delay(500)
        fType(prompt) { text = it }
        delay(400)
        finger.show(targets.center("send"))
        finger.tap()
        text = ""
        sent = true
        keyboard = false
        finger.hide()
        delay(500)
        while (shown < answer.length) {
            shown = minOf(answer.length, shown + 3)
            delay(30)
        }
        delay(3000)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(OcSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(OcSpacing.sm, Alignment.Bottom),
            ) {
                if (sent) {
                    DemoUserBubble(prompt)
                    DemoAssistantText("hide-keyboard-demo", answer.take(shown), cache)
                }
                FComposer(text, targets)
            }
            AnimatedVisibility(
                visible = keyboard,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) { DemoKeyboard() }
        }
        finger.Draw()
    }
}

/** The on-screen keyboard, as rows of blank keys. */
@Composable
private fun DemoKeyboard() {
    val c = OcTheme.colors
    Column(
        Modifier.fillMaxWidth().background(c.layer02).padding(OcSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
    ) {
        listOf(10 to 0.dp, 9 to 12.dp, 7 to 36.dp).forEach { (keys, inset) ->
            Row(Modifier.fillMaxWidth().padding(horizontal = inset), horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                repeat(keys) { Box(Modifier.weight(1f).height(18.dp).clip(RoundedCornerShape(4.dp)).background(c.bgBase)) }
            }
        }
        Box(Modifier.fillMaxWidth(0.5f).height(18.dp).align(Alignment.CenterHorizontally).clip(RoundedCornerShape(4.dp)).background(c.bgBase))
    }
}
