package io.github.beppe2880.opencodemobile.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.AppSettings
import io.github.beppe2880.opencodemobile.core.domain.SettingsPart
import io.github.beppe2880.opencodemobile.core.ui.resources.Res
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_customize
import io.github.beppe2880.opencodemobile.core.ui.text.label
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * What a settings preview draws: the app with [settings], the parts outlined when [marks] is on
 * and [selected] highlighted. [running] shows a chat while the agent works; [still] (thumbnails)
 * stops anything that moves. Tapping a part calls [onCustomize].
 */
@Immutable
data class PartPreview(
    val settings: AppSettings,
    val marks: Boolean = true,
    val selected: SettingsPart? = null,
    val running: Boolean = false,
    val still: Boolean = false,
    val onCustomize: (SettingsPart) -> Unit = {},
)

/**
 * [content] as one button named after [part]: taps never reach the controls inside, which only
 * show how the app looks. With marks a dashed outline and a pencil on its corner show it can be
 * tapped; the selected part has a solid outline and scrolls into view, also after it changes size.
 * The content is inset so the pencil, half outside the outline, never covers it.
 */
@Composable
fun OcCustomizable(
    part: SettingsPart,
    preview: PartPreview,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val c = OcTheme.colors
    val selected = preview.selected == part
    val marks = preview.marks || selected
    val label = part.label()
    val action = stringResource(Res.string.oc_part_customize)
    val outline = if (selected) c.textAccent else c.textAccent.copy(alpha = 0.45f)
    val bringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(selected, preview.settings) { if (selected) bringIntoView.bringIntoView() }
    Box(
        modifier = modifier
            .bringIntoViewRequester(bringIntoView)
            .testTag("preview_part_${part.name.lowercase()}")
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                this.selected = selected
                onClick(label = action) {
                    preview.onCustomize(part)
                    true
                }
            }
            .drawWithContent {
                drawContent()
                if (!marks) return@drawWithContent
                val width = (if (selected) 2.dp else 1.dp).toPx()
                drawRoundRect(
                    color = outline,
                    cornerRadius = CornerRadius(OcRadius.lg.toPx()),
                    topLeft = Offset(width / 2, width / 2),
                    size = Size(size.width - width, size.height - width),
                    style = Stroke(
                        width = width,
                        pathEffect = if (selected) null else PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                    ),
                )
            },
    ) {
        Box(Modifier.padding(PartInset)) { content() }
        // On top of the content, so the controls under it never get the tap.
        Box(
            Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(OcRadius.lg))
                .clickable { preview.onCustomize(part) },
        )
        if (marks) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = BadgeSize / 2, y = -BadgeSize / 2)
                    .size(BadgeSize)
                    .background(if (selected) c.bgAccent else c.layer02, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null, tint = if (selected) c.onAccent else c.textAccent, modifier = Modifier.size(10.dp))
            }
        }
    }
}

private val BadgeSize = 16.dp

/** Space between a part's outline and its content: the pencil, centered on the corner, stays outside it. */
val PartInset = OcSpacing.sm

/**
 * [content] laid out as if [scale] times larger and drawn at [scale], cut to the bounds: a
 * miniature of a whole screen. Hidden from screen readers and from touch (a tap reaches the
 * caller, e.g. the card around it); the caller names it.
 */
@Composable
fun OcThumbnail(scale: Float, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.clipToBounds().clearAndSetSemantics { }) {
        Miniature(scale, content)
        // Above the miniature, so its controls never get a touch; it does not consume them either.
        Box(Modifier.matchParentSize().pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } })
    }
}

@Composable
private fun Miniature(scale: Float, content: @Composable () -> Unit) {
    Layout(
        content = content,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val inner = Constraints.fixed((width / scale).roundToInt(), (height / scale).roundToInt())
        val placeables = measurables.map { it.measure(inner) }
        layout(width, height) { placeables.forEach { it.place(0, 0) } }
    }
}
