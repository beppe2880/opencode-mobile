package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import kotlinx.coroutines.delay

/** Types [full] one character at a time into [set], starting after [from]. */
internal suspend fun fType(full: String, from: String = "", delayMillis: Long = 65, set: (String) -> Unit) {
    for (i in from.length + 1..full.length) {
        set(full.take(i))
        delay(delayMillis)
    }
}

/**
 * The message box of the chat, compact: [above] (hints, chips), the draft, then the + and the
 * send button. The draft is the "input" target and the send button the "send" target.
 */
@Composable
internal fun FComposer(
    text: String,
    targets: DemoTargets,
    modifier: Modifier = Modifier,
    above: @Composable ColumnScope.() -> Unit = {},
) {
    val c = OcTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OcRadius.xl))
            .background(c.bgBase)
            .border(1.dp, c.agents.build, RoundedCornerShape(OcRadius.xl))
            .padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm),
    ) {
        above()
        Text(
            text.ifEmpty { "Ask anything…" },
            style = MaterialTheme.typography.bodyMedium,
            color = if (text.isEmpty()) c.textFaint else c.textBase,
            maxLines = 1,
            modifier = Modifier.then(targets.target("input")).fillMaxWidth().padding(vertical = OcSpacing.xs),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = c.iconBase, modifier = Modifier.size(20.dp))
            Spacer(Modifier.weight(1f))
            Icon(Icons.Outlined.Mic, contentDescription = null, tint = c.iconBase, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(OcSpacing.md))
            Box(
                Modifier.then(targets.target("send")).size(30.dp).clip(CircleShape).background(c.bgInverse),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = c.textInverse, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** Fades and grows [content] in when [visible], and out again. */
@Composable
internal fun FVisible(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically(),
    ) { content() }
}
