package io.github.beppe2880.opencodemobile.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.resources.Res
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_clear
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_hide_password
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_show_password
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * Outlined single-line (or short multiline) field with OpenCode tokens.
 * [error] replaces [supportingText] and marks the field invalid for accessibility.
 */
@Composable
fun OcTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 5,
    mono: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val textStyle = if (mono) OcTheme.mono.code else androidx.compose.material3.MaterialTheme.typography.bodyMedium
    val text = rememberSyncedText(value)
    OutlinedTextField(
        value = text.sync(value),
        onValueChange = { text.input(it, onValueChange) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        textStyle = textStyle,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it, style = textStyle) } },
        supportingText = (error ?: supportingText)?.let { { Text(it) } },
        isError = error != null,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(OcRadius.md),
        colors = ocTextFieldColors(),
    )
}

/** [SyncedText] that falls back to the caller's value when it never echoes the last keystroke. */
@Composable
internal fun rememberSyncedText(value: String): SyncedText {
    val text = remember { SyncedText(value) }
    val current by rememberUpdatedState(value)
    LaunchedEffect(text.inputs) {
        delay(SyncedText.REJECT_AFTER_MS)
        text.settle(current)
    }
    return text
}

/**
 * The text being typed, kept until the caller's state catches up. A String-based field that
 * recomposes before the caller's state arrives (iOS sheets do) is reset to the stale value and
 * its cursor to the start, and fast keystrokes get lost. Values the caller sends back for
 * earlier keystrokes are echoes; any other new value is a real change (clear, reset, transform).
 */
@Stable
internal class SyncedText(initial: String) {
    private var field by mutableStateOf(TextFieldValue(initial, TextRange(initial.length)))
    private var external = initial
    private val sent = ArrayDeque<String>()

    /** Changes with every keystroke that edits the text. */
    var inputs by mutableIntStateOf(0)
        private set

    fun sync(value: String): TextFieldValue {
        val echo = sent.indexOf(value)
        when {
            value == field.text -> sent.clear()
            echo >= 0 -> repeat(echo + 1) { sent.removeFirst() }
            value != external -> {
                sent.clear()
                field = TextFieldValue(value, TextRange(value.length))
            }
        }
        external = value
        return field
    }

    fun input(next: TextFieldValue, onValueChange: (String) -> Unit) {
        val changed = next.text != field.text
        field = next
        if (changed) {
            sent.addLast(next.text)
            inputs++
            onValueChange(next.text)
        }
    }

    /** The caller kept another value (rejected or truncated input): show it. */
    fun settle(value: String) {
        if (sent.isEmpty() || value == field.text) return
        sent.clear()
        external = value
        field = TextFieldValue(value, TextRange(value.length))
    }

    companion object {
        const val REJECT_AFTER_MS = 300L
    }
}

/** Password field with a show/hide toggle. */
@Composable
fun OcPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    error: String? = null,
    supportingText: String? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    imeAction: androidx.compose.ui.text.input.ImeAction = androidx.compose.ui.text.input.ImeAction.Done,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OcTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        error = error,
        supportingText = supportingText,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction, autoCorrectEnabled = false),
        keyboardActions = keyboardActions,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            OcIconButton(
                icon = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = stringResource(if (visible) Res.string.oc_hide_password else Res.string.oc_show_password),
                onClick = { visible = !visible },
                tint = OcTheme.colors.iconMuted,
            )
        },
    )
}

@Composable
fun ocTextFieldColors(): TextFieldColors {
    val c = OcTheme.colors
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = c.textBase,
        unfocusedTextColor = c.textBase,
        disabledTextColor = c.textFaint,
        focusedContainerColor = c.bgBase,
        unfocusedContainerColor = c.bgBase,
        disabledContainerColor = c.layer01,
        errorContainerColor = c.bgBase,
        cursorColor = c.bgAccent,
        focusedBorderColor = c.borderFocus,
        unfocusedBorderColor = c.borderStrong,
        disabledBorderColor = c.borderMuted,
        errorBorderColor = c.danger.fg,
        focusedLabelColor = c.textAccent,
        unfocusedLabelColor = c.textMuted,
        errorLabelColor = c.danger.fg,
        focusedPlaceholderColor = c.textFaint,
        unfocusedPlaceholderColor = c.textFaint,
        focusedSupportingTextColor = c.textMuted,
        unfocusedSupportingTextColor = c.textMuted,
        errorSupportingTextColor = c.danger.fg,
        focusedLeadingIconColor = c.iconMuted,
        unfocusedLeadingIconColor = c.iconMuted,
        focusedTrailingIconColor = c.iconMuted,
        unfocusedTrailingIconColor = c.iconMuted,
        errorTrailingIconColor = c.danger.fg,
        errorCursorColor = c.danger.fg,
        disabledPlaceholderColor = Color.Transparent,
    )
}

/** Compact rounded search field used in list headers. */
@Composable
fun OcSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val c = OcTheme.colors
    val text = rememberSyncedText(value)
    OutlinedTextField(
        value = text.sync(value),
        onValueChange = { text.input(it, onValueChange) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        placeholder = { Text(placeholder, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium) },
        leadingIcon = {
            Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
        },
        trailingIcon = if (value.isNotEmpty()) {
            {
                OcIconButton(
                    icon = Icons.Outlined.Close,
                    contentDescription = stringResource(Res.string.oc_clear),
                    onClick = { onValueChange("") },
                    tint = c.iconMuted,
                    iconSize = 18.dp,
                )
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
        shape = RoundedCornerShape(OcRadius.md),
        colors = ocTextFieldColors().copy(
            focusedContainerColor = c.layer02,
            unfocusedContainerColor = c.layer02,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}
