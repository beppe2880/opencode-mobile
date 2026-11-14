package io.github.beppe2880.opencodemobile.feature.settings.whatsnew

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.markdown.MarkdownCache
import io.github.beppe2880.opencodemobile.core.ui.markdown.OcMarkdown
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/*
 * The toolkit of the feature demos. A demo is a small scene drawn with the app's components and
 * played by a script in a loop:
 *
 *     @Composable
 *     internal fun MyDemo(active: Boolean) {
 *         var step by remember { mutableIntStateOf(LAST) }
 *         val finger = remember { DemoFinger() }
 *         val targets = remember { DemoTargets() }
 *         DemoLoop(active, finalFrame = { step = LAST; finger.hide() }) {
 *             step = 0
 *             delay(600)
 *             finger.show(targets.center("button") + Offset(0f, 120f))
 *             finger.moveTo(targets.center("button"))
 *             finger.tap()
 *             step = 1
 *             delay(2000)
 *         }
 *         Box(Modifier.fillMaxSize().then(targets.root())) {
 *             ... the scene, with Modifier.then(targets.target("button")) on what the finger touches
 *             finger.Draw()
 *         }
 *     }
 */

/**
 * Runs [script] in a loop while [active]; when not active (another card is in view, or the demo is
 * not on screen) shows [finalFrame], the state that best explains the feature at a glance.
 */
@Composable
internal fun DemoLoop(active: Boolean, finalFrame: suspend () -> Unit, script: suspend () -> Unit) {
    LaunchedEffect(active) {
        if (!active) {
            finalFrame()
            return@LaunchedEffect
        }
        // Lets the scene lay out once, so the targets of the finger are known.
        delay(300)
        while (true) {
            script()
        }
    }
}

/** Where the parts of a scene are, so the [DemoFinger] can move to them. */
@Stable
internal class DemoTargets {
    private var origin by mutableStateOf(Offset.Zero)
    private val bounds = mutableStateMapOf<String, Rect>()

    /** On the root of the scene: positions are relative to it. */
    fun root(): Modifier = Modifier.onGloballyPositioned { origin = it.boundsInRoot().topLeft }

    /** On a part of the scene the finger will touch. */
    fun target(name: String): Modifier = Modifier.onGloballyPositioned { bounds[name] = it.boundsInRoot().translate(-origin) }

    fun center(name: String): Offset = bounds[name]?.center ?: Offset.Zero

    fun bounds(name: String): Rect? = bounds[name]
}

/** A finger that moves over a demo and taps, so the demo shows where to touch. */
@Stable
internal class DemoFinger {
    private val x = Animatable(0f)
    private val y = Animatable(0f)
    private val alpha = Animatable(0f)
    private val press = Animatable(0f)
    private val ripple = Animatable(0f)

    suspend fun show(at: Offset) {
        // A target that is not laid out (or not in the scene) would put the finger in the corner.
        if (at == Offset.Zero) return
        x.snapTo(at.x)
        y.snapTo(at.y)
        alpha.animateTo(1f, tween(250))
    }

    suspend fun hide() {
        alpha.animateTo(0f, tween(250))
    }

    suspend fun moveTo(target: Offset, durationMillis: Int = 550) {
        if (target == Offset.Zero) return
        coroutineScope {
            launch { x.animateTo(target.x, tween(durationMillis)) }
            launch { y.animateTo(target.y, tween(durationMillis)) }
        }
        delay(120)
    }

    suspend fun tap() {
        press.animateTo(1f, tween(110))
        coroutineScope {
            launch {
                ripple.snapTo(0f)
                ripple.animateTo(1f, tween(450))
            }
            press.animateTo(0f, tween(160))
        }
    }

    /** Presses and holds for [millis], as a long press. */
    suspend fun longPress(millis: Long = 700) {
        press.animateTo(1f, tween(110))
        coroutineScope {
            launch {
                ripple.snapTo(0f)
                ripple.animateTo(1f, tween(millis.toInt()))
            }
            delay(millis)
        }
        press.animateTo(0f, tween(160))
    }

    /** Presses at the current position, drags to [to] and lifts: a swipe or a drag. */
    suspend fun drag(to: Offset, durationMillis: Int = 600) {
        press.animateTo(1f, tween(110))
        moveTo(to, durationMillis)
        press.animateTo(0f, tween(160))
    }

    /** Where the finger is now, to move a dragged part of the scene with it. */
    val position: Offset get() = Offset(x.value, y.value)

    /** Whether the finger is pressing, to show a dragged part as held. */
    val pressed: Boolean get() = press.value > 0.5f

    /** Draw last in the scene, so it is above everything. */
    @Composable
    fun Draw() {
        val radius = with(LocalDensity.current) { 18.dp.toPx() }
        Box(
            Modifier
                .offset { IntOffset((x.value - radius).roundToInt(), (y.value - radius).roundToInt()) }
                .size(36.dp)
                .alpha(alpha.value),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .scale(1f + ripple.value * 0.9f)
                    .alpha((1f - ripple.value) * 0.5f)
                    .clip(CircleShape)
                    .background(Color.White),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .scale(1f - press.value * 0.2f)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
                    .border(2.dp, Color.White.copy(alpha = 0.9f), CircleShape),
            )
        }
    }
}

// ---- Parts shared by many scenes, drawn like the app draws them ----

/** The message box of the chat: [attachments] above the text, the + and the send button below. */
@Composable
internal fun DemoComposer(
    modifier: Modifier = Modifier,
    text: String = "",
    placeholder: String = "Ask anything…",
    attachments: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
) {
    val c = OcTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OcRadius.lg))
            .background(c.bgBase)
            .border(1.dp, c.borderFocus, RoundedCornerShape(OcRadius.lg))
            .padding(OcSpacing.md),
    ) {
        attachments()
        Text(
            text.ifEmpty { placeholder },
            style = MaterialTheme.typography.bodyMedium,
            color = if (text.isEmpty()) c.textFaint else c.textBase,
            modifier = Modifier.padding(vertical = OcSpacing.sm),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = c.iconBase)
            Spacer(Modifier.weight(1f))
            trailing()
            Icon(Icons.Outlined.Mic, contentDescription = null, tint = c.iconBase)
            Spacer(Modifier.size(OcSpacing.md))
            Box(Modifier.size(32.dp).clip(CircleShape).background(c.bgAccent), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** A prompt of the user, as the chat shows it: a bubble on the end side. */
@Composable
internal fun DemoUserBubble(text: String, modifier: Modifier = Modifier) {
    val c = OcTheme.colors
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = c.userBubbleText,
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(RoundedCornerShape(OcRadius.lg))
                .background(c.userBubbleBg)
                .padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm),
        )
    }
}

/** A reply of the assistant, rendered by the real markdown renderer of the chat. */
@Composable
internal fun DemoAssistantText(id: String, markdown: String, cache: MarkdownCache, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth()) { OcMarkdown(id = id, content = markdown, cache = cache) }
}

/** The top bar of a screen: back arrow, [title] and [subtitle], [actions] on the end side. */
@Composable
internal fun DemoTopBar(title: String, modifier: Modifier = Modifier, subtitle: String? = null, actions: @Composable () -> Unit = {}) {
    val c = OcTheme.colors
    Row(
        modifier.fillMaxWidth().background(c.bgBase).padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
    ) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = c.iconBase, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.textBase, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        actions()
    }
}

/** A session in the list: project initial, [title], [subtitle] and [time]. */
@Composable
internal fun DemoSessionRow(title: String, subtitle: String, time: String, modifier: Modifier = Modifier, leading: @Composable () -> Unit = {}) {
    val c = OcTheme.colors
    Row(
        modifier.fillMaxWidth().padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
    ) {
        Box(Modifier.size(22.dp).clip(RoundedCornerShape(OcRadius.xs)).background(c.layer03), contentAlignment = Alignment.Center) {
            Text(subtitle.take(1).uppercase(), style = MaterialTheme.typography.labelSmall, color = c.textBase)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                leading()
                Text(title, style = MaterialTheme.typography.bodyMedium, color = c.textBase, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = c.textMuted)
        }
        Text(time, style = MaterialTheme.typography.labelSmall, color = c.textFaint)
    }
}
