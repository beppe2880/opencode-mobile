package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import kotlinx.coroutines.delay

private const val ASK_BEFORE = "I'll wrap the call in a "
private const val ASK_PART = "retry with backoff"
private const val ASK_AFTER = " and cap it at five attempts."
private const val ASK_QUESTION = "why not a circuit breaker?"

/**
 * A word of an answer is long-pressed, its selection is extended over a phrase and Ask in the
 * menu quotes it in the message box; a question is typed under the quote and sent.
 */
@Composable
internal fun AskSelectionDemo(active: Boolean) {
    val c = OcTheme.colors
    // 0 nothing selected, 1 a word, 2 the phrase with the menu, 3 quoted in the box, 4 sent
    var phase by remember { mutableIntStateOf(4) }
    var question by remember { mutableStateOf(ASK_QUESTION) }
    val finger = remember { DemoFinger() }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        phase = 4
        question = ASK_QUESTION
        finger.hide()
    }) {
        phase = 0
        question = ""
        finger.hide()
        delay(800)
        finger.show(targets.center("answer") + Offset(0f, 120f))
        finger.moveTo(targets.center("answer"))
        finger.longPress()
        phase = 1
        delay(400)
        finger.drag(targets.center("answer") + Offset(120f, 0f), 600)
        phase = 2
        delay(500)
        finger.moveTo(targets.center("ask"))
        finger.tap()
        phase = 3
        delay(300)
        finger.moveTo(targets.center("input"))
        finger.hide()
        fType(ASK_QUESTION) { question = it }
        delay(400)
        finger.show(targets.center("send"))
        finger.tap()
        phase = 4
        finger.hide()
        delay(2600)
    }
    Box(Modifier.fillMaxSize().then(targets.root()).background(c.bgBase)) {
        Column(
            Modifier.fillMaxSize().padding(OcSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(OcSpacing.sm, Alignment.Bottom),
        ) {
            // The selection menu sits above the reply, as in the chat; its room is kept so nothing jumps.
            Row(
                Modifier
                    .alpha(if (phase == 2) 1f else 0f)
                    .background(c.layer02, RoundedCornerShape(OcRadius.md))
                    .border(1.dp, c.borderBase, RoundedCornerShape(OcRadius.md))
                    .padding(horizontal = OcSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
            ) {
                MenuItem("Copy")
                MenuItem("Select all")
                MenuItem("Ask", Modifier.then(targets.target("ask")))
            }
            val selected = c.bgAccent.copy(alpha = 0.3f)
            Text(
                buildAnnotatedString {
                    append(ASK_BEFORE)
                    withStyle(SpanStyle(background = if (phase == 1 || phase == 2) selected else Color.Transparent)) {
                        append(if (phase == 1) "retry" else ASK_PART)
                    }
                    if (phase == 1) append(" with backoff")
                    append(ASK_AFTER)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = c.textBase,
                modifier = Modifier.then(targets.target("answer")),
            )
            FVisible(phase == 4) { QuotedBubble() }
            FComposer(if (phase == 3) "↳ \"$ASK_PART\" $question" else "", targets)
        }
        finger.Draw()
    }
}

@Composable
private fun MenuItem(label: String, modifier: Modifier = Modifier) {
    Text(label, style = MaterialTheme.typography.labelLarge, color = OcTheme.colors.textBase, modifier = modifier.padding(OcSpacing.sm))
}

/** The sent message: the quote dimmed above the question. */
@Composable
private fun QuotedBubble() {
    val c = OcTheme.colors
    Box(Modifier.fillMaxWidth().padding(start = 48.dp), contentAlignment = Alignment.CenterEnd) {
        Column(
            Modifier
                .background(c.userBubbleBg, RoundedCornerShape(OcRadius.lg))
                .padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm),
        ) {
            Text("↳ \"$ASK_PART\"", style = MaterialTheme.typography.bodySmall, color = c.userBubbleText.copy(alpha = 0.6f))
            Text(ASK_QUESTION, style = MaterialTheme.typography.bodyMedium, color = c.userBubbleText)
        }
    }
}
