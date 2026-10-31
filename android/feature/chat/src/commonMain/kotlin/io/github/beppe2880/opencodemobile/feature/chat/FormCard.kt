package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.Form
import io.github.beppe2880.opencodemobile.core.domain.FormAnswer
import io.github.beppe2880.opencodemobile.core.domain.FormField
import io.github.beppe2880.opencodemobile.core.domain.FormOption
import io.github.beppe2880.opencodemobile.core.domain.isComplete
import io.github.beppe2880.opencodemobile.core.domain.isVisible
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcCard
import io.github.beppe2880.opencodemobile.core.ui.components.OcChip
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcSwitchRow
import io.github.beppe2880.opencodemobile.core.ui.components.OcTextField
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_form_collapse
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_form_dismiss
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_form_expand
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_form_more
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_form_open_link
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_form_other
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_form_submit
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_form_title
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Pinned above the composer while the agent waits for the user to answer a question.
 * [collapsed] folds it into a one-line bar so the transcript stays readable; the answers are
 * kept by the caller and nothing is sent to the server until Send or Dismiss.
 */
@Composable
internal fun FormCard(
    form: Form,
    answers: Map<String, FormAnswer>,
    pendingCount: Int,
    submitting: Boolean,
    collapsed: Boolean,
    onCollapse: (Boolean) -> Unit,
    onAnswer: (String, FormAnswer?) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = OcTheme.colors
    val collapse by rememberUpdatedState(onCollapse)
    OcCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag("form_card"),
        color = if (c.isDark) c.layer01 else c.info.bg,
        borderColor = c.info.border,
        shape = RoundedCornerShape(OcRadius.xl),
    ) {
        if (collapsed) {
            CollapsedForm(form, pendingCount, onExpand = { collapse(false) })
        } else {
            ExpandedForm(form, answers, pendingCount, submitting, { collapse(true) }, onAnswer, onSubmit, onDismiss)
        }
    }
}

@Composable
private fun ExpandedForm(
    form: Form,
    answers: Map<String, FormAnswer>,
    pendingCount: Int,
    submitting: Boolean,
    onCollapse: () -> Unit,
    onAnswer: (String, FormAnswer?) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = OcTheme.colors
    Column(
        Modifier.padding(start = OcSpacing.md, end = OcSpacing.md, bottom = OcSpacing.md),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        // Handle and header: a swipe down folds the card into a bar.
        Column(Modifier.fillMaxWidth().verticalSwipe(onSwipeDown = onCollapse).testTag("form_header")) {
            Box(
                Modifier
                    .padding(top = OcSpacing.xs)
                    .size(width = 32.dp, height = 4.dp)
                    .background(c.borderStrong, CircleShape)
                    .align(Alignment.CenterHorizontally)
                    .testTag("form_drag_handle"),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                Icon(Icons.Outlined.HelpOutline, null, tint = c.info.fg, modifier = Modifier.size(18.dp))
                Text(
                    stringResource(Res.string.chat_form_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = c.textMuted,
                    modifier = Modifier.weight(1f),
                )
                if (pendingCount > 1) {
                    Text(
                        pluralStringResource(Res.plurals.chat_form_more, pendingCount - 1, pendingCount - 1),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textFaint,
                    )
                }
                OcIconButton(
                    icon = Icons.Outlined.ExpandMore,
                    contentDescription = stringResource(Res.string.chat_form_collapse),
                    onClick = onCollapse,
                    tint = c.iconMuted,
                    modifier = Modifier.testTag("form_collapse_button"),
                )
            }
        }
        Text(form.title, style = MaterialTheme.typography.titleSmall, color = c.textBase)
        Column(
            modifier = Modifier
                .heightIn(max = 320.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(OcSpacing.md),
        ) {
            form.fields.filter { it.isVisible(answers) }.forEach { field ->
                FormFieldInput(field, answers[field.key], onAnswer = { onAnswer(field.key, it) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
            OcButton(
                text = stringResource(Res.string.chat_form_dismiss),
                onClick = onDismiss,
                variant = OcButtonVariant.Secondary,
                enabled = !submitting,
                compact = true,
                modifier = Modifier.weight(1f).testTag("form_dismiss_button"),
            )
            OcButton(
                text = stringResource(Res.string.chat_form_submit),
                onClick = onSubmit,
                variant = OcButtonVariant.Inverse,
                enabled = form.isComplete(answers),
                loading = submitting,
                compact = true,
                modifier = Modifier.weight(1.4f).testTag("form_submit_button"),
            )
        }
    }
}

/** The folded card: the question on one line; a tap or a swipe up opens it again. */
@Composable
private fun CollapsedForm(form: Form, pendingCount: Int, onExpand: () -> Unit) {
    val c = OcTheme.colors
    val expandLabel = stringResource(Res.string.chat_form_expand)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClickLabel = expandLabel, role = Role.Button, onClick = onExpand)
            .verticalSwipe(onSwipeUp = onExpand)
            .padding(start = OcSpacing.md, end = OcSpacing.sm)
            .testTag("form_collapsed_bar"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        Icon(Icons.Outlined.HelpOutline, stringResource(Res.string.chat_form_title), tint = c.info.fg, modifier = Modifier.size(18.dp))
        Text(
            form.title,
            style = MaterialTheme.typography.bodyMedium,
            color = c.textBase,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (pendingCount > 1) {
            Text(
                pluralStringResource(Res.plurals.chat_form_more, pendingCount - 1, pendingCount - 1),
                style = MaterialTheme.typography.labelSmall,
                color = c.textFaint,
            )
        }
        Icon(Icons.Outlined.ExpandLess, null, tint = c.iconMuted, modifier = Modifier.size(20.dp))
    }
}

/**
 * Calls [onSwipeDown] or [onSwipeUp] once a vertical drag ends past a short distance. The
 * gesture is not restarted on recomposition, so the callbacks must read current state.
 */
private fun Modifier.verticalSwipe(onSwipeDown: (() -> Unit)? = null, onSwipeUp: (() -> Unit)? = null): Modifier =
    pointerInput(Unit) {
        val threshold = SwipeThreshold.toPx()
        var total = 0f
        detectVerticalDragGestures(
            onDragStart = { total = 0f },
            onDragEnd = {
                when {
                    total > threshold -> onSwipeDown?.invoke()
                    total < -threshold -> onSwipeUp?.invoke()
                }
            },
        ) { change, amount ->
            change.consume()
            total += amount
        }
    }

private val SwipeThreshold = 24.dp

@Composable
private fun FormFieldInput(field: FormField, answer: FormAnswer?, onAnswer: (FormAnswer?) -> Unit) {
    val c = OcTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.xs), modifier = Modifier.testTag("form_field_${field.key}")) {
        if (field !is FormField.Toggle) {
            field.title?.let { title ->
                Text(
                    if (field.required) "$title *" else title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textBase,
                )
            }
            field.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.textMuted) }
        }
        when (field) {
            is FormField.Text -> TextInput(field, (answer as? FormAnswer.Text)?.value, onAnswer)
            is FormField.Toggle -> OcSwitchRow(
                title = field.title ?: field.key,
                subtitle = field.description,
                checked = (answer as? FormAnswer.Bool)?.value ?: false,
                onCheckedChange = { onAnswer(FormAnswer.Bool(it)) },
                modifier = Modifier.testTag("form_toggle_${field.key}"),
            )
            is FormField.MultiSelect -> MultiSelectInput(field, (answer as? FormAnswer.Choices)?.values.orEmpty(), onAnswer)
            is FormField.Number -> {
                // Local text so partial input like "3." is not reformatted while typing.
                var text by rememberSaveable(field.key) {
                    mutableStateOf((answer as? FormAnswer.Number)?.value?.let { if (field.integer) it.toLong().toString() else it.toString() }.orEmpty())
                }
                OcTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        onAnswer(it.replace(',', '.').toDoubleOrNull()?.let(FormAnswer::Number))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = if (field.integer) KeyboardType.Number else KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("form_number_${field.key}"),
                )
            }
            is FormField.External -> {
                val uriHandler = LocalUriHandler.current
                OcButton(
                    text = stringResource(Res.string.chat_form_open_link),
                    onClick = { runCatching { uriHandler.openUri(field.url) } },
                    variant = OcButtonVariant.Secondary,
                    leadingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
                    compact = true,
                    modifier = Modifier.testTag("form_link_${field.key}"),
                )
            }
        }
    }
}

/** Options as single-choice chips; free text when there are none or [FormField.Text.custom] allows it. */
@Composable
private fun TextInput(field: FormField.Text, value: String?, onAnswer: (FormAnswer?) -> Unit) {
    val isOption = field.options.any { it.value == value }
    if (field.options.isNotEmpty()) {
        OptionChips(field.key, field.options, selected = { it.value == value }) { option ->
            onAnswer(if (option.value == value) null else FormAnswer.Text(option.value))
        }
    }
    if (field.options.isEmpty() || field.custom) {
        OcTextField(
            value = if (isOption) "" else value.orEmpty(),
            onValueChange = { onAnswer(it.takeIf(String::isNotEmpty)?.let(FormAnswer::Text)) },
            placeholder = field.placeholder ?: if (field.options.isNotEmpty()) stringResource(Res.string.chat_form_other) else null,
            singleLine = false,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth().testTag("form_text_${field.key}"),
        )
    }
}

@Composable
private fun MultiSelectInput(field: FormField.MultiSelect, values: List<String>, onAnswer: (FormAnswer?) -> Unit) {
    val update = { next: List<String> -> onAnswer(next.takeIf { it.isNotEmpty() }?.let(FormAnswer::Choices)) }
    OptionChips(field.key, field.options, selected = { it.value in values }) { option ->
        update(if (option.value in values) values - option.value else values + option.value)
    }
    if (field.custom) {
        val optionValues = field.options.map { it.value }.toSet()
        val custom = values.firstOrNull { it !in optionValues }.orEmpty()
        OcTextField(
            value = custom,
            onValueChange = { text -> update(values.filter { it in optionValues } + listOfNotNull(text.takeIf(String::isNotEmpty))) },
            placeholder = stringResource(Res.string.chat_form_other),
            modifier = Modifier.fillMaxWidth().testTag("form_text_${field.key}"),
        )
    }
}

/** Short options as chips; options with descriptions as full-width selectable rows. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionChips(
    key: String,
    options: List<FormOption>,
    selected: (FormOption) -> Boolean,
    onClick: (FormOption) -> Unit,
) {
    if (options.none { it.description != null }) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(OcSpacing.xs),
        ) {
            options.forEach { option ->
                val isSelected = selected(option)
                OcChip(
                    text = option.label,
                    selected = isSelected,
                    leadingIcon = if (isSelected) Icons.Outlined.Check else null,
                    onClick = { onClick(option) },
                    modifier = Modifier.testTag("form_option_${key}_${option.value}"),
                )
            }
        }
        return
    }
    val c = OcTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
        options.forEach { option ->
            val isSelected = selected(option)
            Surface(
                onClick = { onClick(option) },
                shape = RoundedCornerShape(OcRadius.md),
                color = if (isSelected) c.layer03 else c.bgBase,
                border = BorderStroke(1.dp, if (isSelected) c.borderStrong else c.borderBase),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { this.selected = isSelected }
                    .testTag("form_option_${key}_${option.value}"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = OcSpacing.md, vertical = OcSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(option.label, style = MaterialTheme.typography.bodyMedium, color = c.textBase)
                        option.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.textMuted) }
                    }
                    if (isSelected) Icon(Icons.Outlined.Check, null, tint = c.textBase, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
