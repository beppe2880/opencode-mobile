package io.github.beppe2880.opencodemobile.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme

/**
 * Filter / selector chip. [dotColor] renders a leading color dot (agents); [secondaryText]
 * follows [text] in a fainter color (e.g. the provider of a model).
 */
@Composable
fun OcChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    dotColor: Color? = null,
    mono: Boolean = false,
    secondaryText: String? = null,
    /** Makes [trailingIcon] a separate button (e.g. remove) instead of part of the chip. */
    onTrailingClick: (() -> Unit)? = null,
    trailingContentDescription: String? = null,
) {
    val c = OcTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 32.dp),
        shape = RoundedCornerShape(OcRadius.md),
        color = if (selected) c.layer03 else c.bgBase,
        contentColor = if (selected) c.textBase else c.textMuted,
        border = BorderStroke(1.dp, if (selected) c.borderStrong else c.borderBase),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (dotColor != null) OcStatusDot(dotColor)
            if (leadingIcon != null) Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
                text = buildAnnotatedString {
                    append(text)
                    if (secondaryText != null) {
                        withStyle(SpanStyle(color = c.textFaint)) { append(" · $secondaryText") }
                    }
                },
                style = if (mono) OcTheme.mono.label else MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            when {
                trailingIcon == null -> Unit
                onTrailingClick == null -> Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(16.dp))
                else -> Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(OcRadius.sm))
                        .clickable(role = Role.Button, onClick = onTrailingClick)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(trailingIcon, contentDescription = trailingContentDescription, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

data class OcSegment<T>(val value: T, val label: String, val testTag: String)

/** Equal-width segmented control. */
@Composable
fun <T> OcSegmentedControl(
    segments: List<OcSegment<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = OcTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OcRadius.md))
            .background(c.layer02)
            .padding(3.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        segments.forEach { segment ->
            val isSelected = segment.value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(OcRadius.sm))
                    .background(if (isSelected) c.bgBase else Color.Transparent)
                    .then(
                        if (isSelected) Modifier.border(1.dp, c.borderBase, RoundedCornerShape(OcRadius.sm)) else Modifier,
                    )
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(segment.value) })
                    .testTag(segment.testTag),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = segment.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) c.textBase else c.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Compact top bar, hairline bottom border. Consumes the status bar inset itself.
 * [title] and [subtitle] are single line; [subtitleContent], when set, replaces [subtitle].
 * [leading] usually holds a back button.
 */
@Composable
fun OcTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleContent: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    titleAccessory: (@Composable RowScope.() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    applyStatusBarInset: Boolean = true,
) {
    val c = OcTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(c.bgBase)
            .then(if (applyStatusBarInset) Modifier.windowInsetsPadding(WindowInsets.statusBars) else Modifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = if (leading == null) OcSpacing.gutter else OcSpacing.xs, end = OcSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading?.invoke()
            Column(modifier = Modifier.weight(1f).padding(start = if (leading == null) 0.dp else OcSpacing.xs)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = c.textBase,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).semantics { heading() },
                    )
                    titleAccessory?.invoke(this)
                }
                if (subtitleContent != null) {
                    subtitleContent()
                } else if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
        OcDivider()
    }
}

/** Settings style row with a trailing switch; the whole row toggles. */
@Composable
fun OcSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val c = OcTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .heightIn(min = 56.dp)
            .padding(horizontal = OcSpacing.lg, vertical = OcSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = c.textBase)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            }
        }
        Box(Modifier.width(OcSpacing.md))
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

/** Settings style row with a slider under the title; [valueLabel] shows the current value. */
@Composable
fun OcSliderRow(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    steps: Int = 0,
) {
    val c = OcTheme.colors
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = OcSpacing.lg, vertical = OcSpacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = c.textBase)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                }
            }
            Box(Modifier.width(OcSpacing.md))
            Text(valueLabel, style = MaterialTheme.typography.labelLarge, color = c.textBase)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = c.bgAccent,
                activeTrackColor = c.bgAccent,
                inactiveTrackColor = c.layer03,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            modifier = Modifier.semantics { contentDescription = title },
        )
    }
}

/** Clickable list row with optional leading/trailing slots. */
@Composable
fun OcListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    titleColor: Color = OcTheme.colors.textBase,
    monoSubtitle: Boolean = false,
) {
    val c = OcTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .padding(horizontal = OcSpacing.lg, vertical = OcSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = if (monoSubtitle) OcTheme.mono.codeSmall else MaterialTheme.typography.bodySmall,
                    color = c.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
    }
}
