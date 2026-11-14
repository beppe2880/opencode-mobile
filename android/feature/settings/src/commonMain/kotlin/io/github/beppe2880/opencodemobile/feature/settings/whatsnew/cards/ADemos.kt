package io.github.beppe2880.opencodemobile.feature.settings.whatsnew.cards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcProjectAvatar
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTopBar
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoUserBubble

/** Pictures drawn with gradients, so the demos need no image files. */
internal val APictures = listOf(
    Brush.verticalGradient(listOf(Color(0xFF7FB3E8), Color(0xFFF6D7A7), Color(0xFF6E9B5C))),
    Brush.linearGradient(listOf(Color(0xFFF08A5D), Color(0xFFB83B5E), Color(0xFF6A2C70))),
    Brush.linearGradient(listOf(Color(0xFF43CEA2), Color(0xFF185A9D))),
)

@Composable
internal fun APicture(index: Int, modifier: Modifier = Modifier, sun: Dp = 14.dp) {
    Box(modifier.background(APictures[index % APictures.size])) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(sun / 2)
                .size(sun)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.6f)),
        )
    }
}

internal class ASession(val title: String, val project: String, val time: String)

internal val ASessions = listOf(
    ASession("Fix login redirect", "web-app", "2m"),
    ASession("Add dark mode toggle", "mobile", "1h"),
    ASession("Refactor billing", "api", "3h"),
    ASession("Write release notes", "docs", "1d"),
)

/** A compact session row, like the list draws it: project avatar, [title] (with a pin when [pinned]) and [time]. */
@Composable
internal fun ASessionRow(session: ASession, modifier: Modifier = Modifier, pinned: Boolean = false) {
    val c = OcTheme.colors
    Row(
        modifier.fillMaxWidth().heightIn(min = 40.dp).padding(horizontal = OcSpacing.md, vertical = OcSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
    ) {
        OcProjectAvatar(session.project)
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
            if (pinned) {
                Icon(Icons.Filled.PushPin, contentDescription = null, tint = c.textFaint, modifier = Modifier.size(14.dp))
            }
            Text(session.title, style = MaterialTheme.typography.bodyMedium, color = c.textBase, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(session.time, style = MaterialTheme.typography.labelSmall, color = c.textFaint)
    }
}

/** A conversation, the content the chat list slides over. */
@Composable
internal fun AMiniChat(modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Column(modifier.fillMaxSize().background(c.layer01)) {
        DemoTopBar("Fix login redirect", subtitle = "web-app")
        Column(Modifier.padding(OcSpacing.md), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
            DemoUserBubble("Why does login loop?")
            Text("The callback drops the return URL, so the guard sends you back to /login.", style = MaterialTheme.typography.bodyMedium, color = c.textBase)
            Text("I'll keep it in the session and redirect after sign-in.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
        }
    }
}

@Composable
internal fun AChatList(modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Column(modifier.fillMaxSize().background(c.bgBase)) {
        Text(
            "Chats",
            style = MaterialTheme.typography.titleSmall,
            color = c.textBase,
            modifier = Modifier.padding(horizontal = OcSpacing.md, vertical = OcSpacing.md),
        )
        ASessions.forEach { ASessionRow(it) }
    }
}

/**
 * A pull that follows the finger: [fraction] goes from 0 to 1 while the finger moves by [width], and when it is
 * released it settles to 0 or 1.
 */
@Stable
internal class ADragPull(private val finger: DemoFinger, initial: Float = 0f) {
    val settle = Animatable(initial)
    var width by mutableFloatStateOf(1f)
    private var dragging by mutableStateOf(false)
    private var startX = 0f
    private var startFraction = 0f

    val fraction: Float
        get() = if (dragging) (startFraction + (finger.position.x - startX) / width).coerceIn(0f, 1f) else settle.value

    /** Presses at [from], moves by [dx] and releases; settles open when past [opensAbove]. */
    suspend fun pull(from: Offset, dx: Float, opensAbove: Float = 0.5f) {
        finger.moveTo(from)
        startX = from.x
        startFraction = settle.value
        dragging = true
        finger.drag(from + Offset(dx, 0f))
        val released = fraction
        dragging = false
        settle.snapTo(released)
        settle.animateTo(if (released > opensAbove) 1f else 0f, tween(300))
    }
}

/** The chat with the chat list as a panel over it, pulled in from the start edge by [pull]. */
@Composable
internal fun ASwipeChatScene(pull: ADragPull, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Box(modifier.fillMaxSize()) {
        AMiniChat()
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = pull.fraction }.background(c.scrim))
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.78f)
                .onSizeChanged { pull.width = it.width.toFloat() }
                .graphicsLayer { translationX = -(1f - pull.fraction) * size.width }
                .background(c.bgBase),
        ) {
            AChatList()
        }
    }
}

/** A settings row with a switch, as [io.github.beppe2880.opencodemobile.core.ui.components.OcSwitchRow] but shorter. */
@Composable
internal fun ASwitchRow(title: String, checked: Boolean, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Row(
        modifier.fillMaxWidth().heightIn(min = 40.dp).padding(horizontal = OcSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = c.textBase,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.onAccent,
                checkedTrackColor = c.bgAccent,
                checkedBorderColor = c.bgAccent,
                uncheckedThumbColor = c.iconMuted,
                uncheckedTrackColor = c.layer02,
                uncheckedBorderColor = c.borderStrong,
            ),
        )
    }
}

/** A short settings row with the [title] and the current [value] on the end side. */
@Composable
internal fun AValueRow(title: String, value: String, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Row(
        modifier.fillMaxWidth().heightIn(min = 40.dp).padding(horizontal = OcSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = c.textBase,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(value, style = MaterialTheme.typography.bodySmall, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
