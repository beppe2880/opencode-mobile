package io.github.beppe2880.opencodemobile.core.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.SettingsPart
import io.github.beppe2880.opencodemobile.core.ui.resources.Res
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_part_customize
import io.github.beppe2880.opencodemobile.core.ui.text.label
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * Editing the settings on the real screens (the live demo): while [active], every element marked
 * [editable] shows an outline and a pencil, and a tap on it calls [onEdit] instead of what the
 * element does; [selected] is the one whose options are open.
 */
@Immutable
class EditMode(
    val active: Boolean,
    val selected: SettingsPart? = null,
    val onEdit: (SettingsPart) -> Unit = {},
)

/** Null outside the live demo: [editable] then changes nothing. */
val LocalEditMode = compositionLocalOf<EditMode?> { null }

/**
 * Marks this element as the [part] of the settings it shows. Without an active [mode] (always
 * outside the live demo) it returns this modifier unchanged, so the app behaves exactly as without
 * it. While active: a tap opens the part's options and never reaches the element (nor its long
 * press), drags still scroll the screen, the innermost marked element wins, and screen readers
 * read it as one button. A selected element scrolls into view once the options make room.
 */
fun Modifier.editable(part: SettingsPart, mode: EditMode?): Modifier =
    if (mode == null || !mode.active) this else then(editableMarks(part, mode))

private fun editableMarks(part: SettingsPart, mode: EditMode): Modifier = Modifier.composed {
    val c = OcTheme.colors
    val selected = mode.selected == part
    val label = part.label()
    val action = stringResource(Res.string.oc_part_customize)
    val badge = rememberVectorPainter(Icons.Outlined.Edit)
    val onEdit by rememberUpdatedState(mode.onEdit)
    val bringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(selected) {
        if (!selected) return@LaunchedEffect
        // The options panel first takes its room (it slides in), then the screen scrolls the
        // element above it; once more when the panel has settled.
        repeat(2) { withFrameNanos { } }
        bringIntoView.bringIntoView()
        delay(PANEL_SETTLE_MS)
        bringIntoView.bringIntoView()
    }
    val outline = if (selected) c.textAccent else c.textAccent.copy(alpha = 0.5f)
    Modifier
        .bringIntoViewRequester(bringIntoView)
        .pointerInput(part) {
            awaitEachGesture {
                // Taken before the element sees it: its click and long press never start.
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                down.consume()
                var tap = true
                while (true) {
                    // Inner elements see the release first: the innermost marked element wins.
                    val change = awaitPointerEvent(PointerEventPass.Main).changes.firstOrNull { it.id == down.id } ?: break
                    if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) tap = false
                    if (!change.pressed) {
                        if (tap && !change.isConsumed) {
                            change.consume()
                            onEdit(part)
                        }
                        break
                    }
                }
            }
        }
        .clearAndSetSemantics {
            contentDescription = label
            role = Role.Button
            this.selected = selected
            onClick(label = action) {
                onEdit(part)
                true
            }
        }
        .drawWithContent {
            drawContent()
            val width = (if (selected) 2.dp else 1.dp).toPx()
            drawRoundRect(
                color = outline,
                cornerRadius = CornerRadius(OcRadius.md.toPx()),
                topLeft = Offset(width / 2, width / 2),
                size = Size(size.width - width, size.height - width),
                style = Stroke(width, pathEffect = if (selected) null else PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
            )
            // On the top edge, inset from the corner, over the element's padding; a single line
            // (header line, chips) has none, so its pencil sits just past its end instead.
            val radius = 8.dp.toPx()
            val center = if (size.height < 40.dp.toPx()) Offset(size.width + radius + 2.dp.toPx(), size.height / 2) else Offset(size.width - 3 * radius, 0f)
            drawCircle(if (selected) c.bgAccent else c.layer02, radius, center)
            val icon = 10.dp.toPx()
            translate(center.x - icon / 2, center.y - icon / 2) {
                with(badge) { draw(Size(icon, icon), colorFilter = ColorFilter.tint(if (selected) c.onAccent else c.textAccent)) }
            }
        }
}

/** A little longer than the options panel takes to slide in. */
private const val PANEL_SETTLE_MS = 400L
