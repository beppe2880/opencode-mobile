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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcChip
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoFinger
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoLoop
import io.github.beppe2880.opencodemobile.feature.settings.whatsnew.DemoTargets
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Swipe right in a chat: the chat list slides in as a panel, then a swipe left closes it. */
@Composable
internal fun SwipeChatListDemo(active: Boolean) {
    val finger = remember { DemoFinger() }
    val pull = remember { ADragPull(finger) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    DemoLoop(active, finalFrame = {
        finger.hide()
        pull.settle.snapTo(1f)
    }) {
        pull.settle.snapTo(0f)
        delay(900)
        val w = size.width.toFloat()
        val h = size.height.toFloat()
        finger.show(Offset(w * 0.12f, h * 0.62f))
        pull.pull(Offset(w * 0.12f, h * 0.62f), dx = w * 0.5f)
        finger.hide()
        delay(1800)
        finger.show(Offset(w * 0.75f, h * 0.62f))
        pull.pull(Offset(w * 0.75f, h * 0.62f), dx = -w * 0.45f)
        finger.hide()
        delay(1000)
    }
    Box(Modifier.fillMaxSize().onSizeChanged { size = it }) {
        ASwipeChatScene(pull)
        finger.Draw()
    }
}

private val SENSITIVITIES = listOf("Low", "Medium", "High")
private val SENSITIVITY_THRESHOLDS = listOf(0.66f, 0.5f, 0.25f)

/** The same short swipe: with Low it springs back, with High it opens the chat list. */
@Composable
internal fun SwipeSensitivityDemo(active: Boolean) {
    var sensitivity by remember { mutableIntStateOf(2) }
    val finger = remember { DemoFinger() }
    val pull = remember { ADragPull(finger) }
    val targets = remember { DemoTargets() }
    DemoLoop(active, finalFrame = {
        sensitivity = 2
        finger.hide()
        pull.settle.snapTo(1f)
    }) {
        sensitivity = 0
        pull.settle.snapTo(0f)
        delay(900)
        val scene = targets.bounds("scene") ?: return@DemoLoop
        val start = Offset(scene.left + scene.width * 0.1f, scene.center.y)
        val dx = pull.width * 0.35f
        finger.show(start)
        pull.pull(start, dx, SENSITIVITY_THRESHOLDS[0])
        delay(700)
        finger.moveTo(targets.center("high"))
        finger.tap()
        sensitivity = 2
        delay(500)
        pull.pull(start, dx, SENSITIVITY_THRESHOLDS[2])
        finger.hide()
        delay(1600)
        pull.settle.animateTo(0f, tween(400))
        delay(300)
    }
    Box(Modifier.fillMaxSize().then(targets.root())) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(OcSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Swipe sensitivity", style = MaterialTheme.typography.labelMedium, color = OcTheme.colors.textMuted)
                SENSITIVITIES.forEachIndexed { index, label ->
                    val modifier = if (index == 2) targets.target("high") else Modifier
                    OcChip(label, onClick = {}, selected = sensitivity == index, modifier = modifier)
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(0.dp)).then(targets.target("scene"))) {
                ASwipeChatScene(pull)
            }
        }
        finger.Draw()
    }
}

/** On a wide window the chat swipe hides the list beside the chat and brings it back. */
@Composable
internal fun HideSideListDemo(active: Boolean) {
    val c = OcTheme.colors
    val finger = remember { DemoFinger() }
    val pull = remember { ADragPull(finger, initial = 1f) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val listPx = with(LocalDensity.current) { 130.dp.toPx() }
    LaunchedEffect(listPx) { pull.width = listPx }
    DemoLoop(active, finalFrame = {
        finger.hide()
        pull.settle.snapTo(1f)
    }) {
        pull.settle.snapTo(1f)
        delay(900)
        val w = size.width.toFloat()
        val h = size.height.toFloat()
        val from = Offset(w * 0.78f, h * 0.65f)
        finger.show(from)
        pull.pull(from, dx = -listPx * 0.8f)
        delay(1500)
        val back = Offset(w * 0.3f, h * 0.65f)
        pull.pull(back, dx = listPx * 0.8f)
        finger.hide()
        delay(1500)
    }
    Box(Modifier.fillMaxSize().onSizeChanged { size = it }) {
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .layout { measurable, constraints ->
                        val full = listPx.roundToInt()
                        val placeable = measurable.measure(constraints.copy(minWidth = full, maxWidth = full))
                        layout((full * pull.fraction).roundToInt(), placeable.height) { placeable.place(0, 0) }
                    },
            ) {
                AChatList()
            }
            Box(Modifier.fillMaxHeight().width(1.dp).background(c.borderMuted))
            Box(Modifier.weight(1f)) { AMiniChat() }
        }
        finger.Draw()
    }
}

@Stable
private class AImagePager(private val finger: DemoFinger) {
    val page = Animatable(0f)
    var width by mutableFloatStateOf(1f)
    private var dragging by mutableStateOf(false)
    private var startX = 0f
    private var startPage = 0f

    val position: Float
        get() = if (dragging) (startPage - (finger.position.x - startX) / width).coerceIn(0f, 2f) else page.value

    suspend fun swipe(from: Offset, dx: Float) {
        finger.moveTo(from)
        startX = from.x
        startPage = page.value
        dragging = true
        finger.drag(from + Offset(dx, 0f))
        val released = position
        dragging = false
        page.snapTo(released)
        page.animateTo(released.roundToInt().toFloat(), tween(300))
    }
}

/** The full-screen viewer: a swipe moves to the next image and the counter follows. */
@Composable
internal fun ImageSwipeDemo(active: Boolean) {
    val finger = remember { DemoFinger() }
    val pager = remember { AImagePager(finger) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    DemoLoop(active, finalFrame = {
        finger.hide()
        pager.page.snapTo(1f)
    }) {
        pager.page.snapTo(0f)
        delay(900)
        val w = size.width.toFloat()
        val from = Offset(w * 0.8f, size.height * 0.5f)
        finger.show(from)
        pager.swipe(from, -w * 0.55f)
        delay(900)
        pager.swipe(from, -w * 0.55f)
        finger.hide()
        delay(1500)
        pager.page.animateTo(0f, tween(500))
        delay(400)
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF101010))
            .onSizeChanged {
                size = it
                pager.width = it.width.toFloat()
            },
    ) {
        repeat(3) { index ->
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationX = (index - pager.position) * pager.width }
                    .padding(horizontal = OcSpacing.sm, vertical = 36.dp),
            ) {
                APicture(index, Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)), sun = 28.dp)
            }
        }
        Text(
            "${pager.position.roundToInt() + 1} / 3",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier.align(Alignment.TopCenter).padding(OcSpacing.md),
        )
        Icon(Icons.Outlined.Close, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(OcSpacing.sm))
        finger.Draw()
    }
}
