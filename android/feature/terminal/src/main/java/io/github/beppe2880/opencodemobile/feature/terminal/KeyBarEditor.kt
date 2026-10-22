package io.github.beppe2880.opencodemobile.feature.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import io.github.beppe2880.opencodemobile.core.ui.components.OcBottomSheet
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcCaption
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcSectionHeader
import io.github.beppe2880.opencodemobile.core.ui.components.OcTextField
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme

/** Rearranges the key bar; every change is saved and shown at once. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun KeyBarEditor(rows: List<List<BarKey>>, onSave: (List<List<BarKey>>) -> Unit, onReset: () -> Unit, onDismiss: () -> Unit) {
    // Row and index of the key the actions apply to.
    var selected by rememberSaveable { mutableStateOf<Pair<Int, Int>?>(null) }
    var addingTo by rememberSaveable { mutableStateOf<Int?>(null) }
    var customFor by rememberSaveable { mutableStateOf<Int?>(null) }

    fun update(transform: (MutableList<MutableList<BarKey>>) -> Unit) {
        val next = rows.map { it.toMutableList() }.toMutableList()
        transform(next)
        onSave(next)
    }

    OcBottomSheet(onDismiss = onDismiss, testTag = "terminal_keys_editor") {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OcSpacing.gutter, vertical = OcSpacing.md),
            verticalArrangement = Arrangement.spacedBy(OcSpacing.md),
        ) {
            Text(stringResource(R.string.terminal_keys_title), style = MaterialTheme.typography.titleMedium, color = OcTheme.colors.textBase)
            OcCaption(stringResource(R.string.terminal_keys_hint))

            rows.forEachIndexed { rowIndex, row ->
                OcSectionHeader(stringResource(R.string.terminal_keys_row, rowIndex + 1))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs), verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                    row.forEachIndexed { index, key ->
                        KeyButton(
                            key = key,
                            selected = selected == rowIndex to index,
                            onClick = {
                                addingTo = null
                                selected = if (selected == rowIndex to index) null else rowIndex to index
                            },
                            modifier = Modifier.testTag("terminal_keys_editor_key_${rowIndex}_$index"),
                        )
                    }
                    OcIconButton(
                        icon = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.terminal_keys_add_to_row, rowIndex + 1),
                        onClick = {
                            selected = null
                            addingTo = if (addingTo == rowIndex) null else rowIndex
                        },
                        modifier = Modifier.testTag("terminal_keys_editor_add_$rowIndex"),
                    )
                }
                selected?.takeIf { it.first == rowIndex }?.let { (r, i) ->
                    SelectedKeyActions(
                        canMoveLeft = i > 0,
                        canMoveRight = i < row.lastIndex,
                        onMoveLeft = {
                            update { it[r].add(i - 1, it[r].removeAt(i)) }
                            selected = r to i - 1
                        },
                        onMoveRight = {
                            update { it[r].add(i + 1, it[r].removeAt(i)) }
                            selected = r to i + 1
                        },
                        onOtherRow = {
                            val target = (r + 1) % rows.size
                            update { it[target].add(it[r].removeAt(i)) }
                            selected = target to rows[target].size
                        },
                        onRemove = {
                            update { it[r].removeAt(i) }
                            selected = null
                        },
                    )
                }
                if (addingTo == rowIndex) {
                    OcCaption(stringResource(R.string.terminal_keys_pick))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs), verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                        BarKeys.Catalog.forEach { key ->
                            KeyButton(
                                key = key,
                                onClick = { update { it[rowIndex].add(key) } },
                                modifier = Modifier.testTag("terminal_keys_catalog_${key.testId}"),
                            )
                        }
                    }
                    OcButton(
                        text = stringResource(R.string.terminal_keys_custom),
                        onClick = { customFor = rowIndex },
                        variant = OcButtonVariant.Secondary,
                        compact = true,
                        leadingIcon = Icons.Outlined.Add,
                        modifier = Modifier.testTag("terminal_keys_custom_button"),
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm, Alignment.End)) {
                OcButton(
                    text = stringResource(R.string.terminal_keys_reset),
                    onClick = {
                        selected = null
                        addingTo = null
                        onReset()
                    },
                    variant = OcButtonVariant.Ghost,
                    modifier = Modifier.testTag("terminal_keys_reset_button"),
                )
                OcButton(
                    text = stringResource(R.string.terminal_keys_done),
                    onClick = onDismiss,
                    modifier = Modifier.testTag("terminal_keys_done_button"),
                )
            }
        }
    }

    customFor?.let { row ->
        CustomKeyDialog(
            onConfirm = { key ->
                update { it[row].add(key) }
                customFor = null
            },
            onDismiss = { customFor = null },
        )
    }
}

@Composable
private fun SelectedKeyActions(
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onOtherRow: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OcIconButton(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = stringResource(R.string.terminal_keys_move_left),
            onClick = onMoveLeft,
            enabled = canMoveLeft,
            modifier = Modifier.testTag("terminal_keys_move_left"),
        )
        OcIconButton(
            icon = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = stringResource(R.string.terminal_keys_move_right),
            onClick = onMoveRight,
            enabled = canMoveRight,
            modifier = Modifier.testTag("terminal_keys_move_right"),
        )
        OcIconButton(
            icon = Icons.Outlined.SwapVert,
            contentDescription = stringResource(R.string.terminal_keys_other_row),
            onClick = onOtherRow,
            modifier = Modifier.testTag("terminal_keys_other_row"),
        )
        OcIconButton(
            icon = Icons.Outlined.DeleteOutline,
            contentDescription = stringResource(R.string.terminal_keys_remove),
            onClick = onRemove,
            tint = OcTheme.colors.danger.fg,
            modifier = Modifier.testTag("terminal_keys_remove"),
        )
    }
}

/** A key that sends any text: a symbol, or a macro such as `\e:wq\r`. */
@Composable
private fun CustomKeyDialog(onConfirm: (BarKey) -> Unit, onDismiss: () -> Unit) {
    var label by rememberSaveable { mutableStateOf("") }
    var text by rememberSaveable { mutableStateOf("") }
    val valid = label.isNotBlank() && text.isNotEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OcTheme.colors.layer01,
        title = { Text(stringResource(R.string.terminal_keys_custom)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.md)) {
                OcTextField(
                    value = label,
                    onValueChange = { label = it.take(MAX_LABEL) },
                    label = stringResource(R.string.terminal_keys_custom_label),
                    modifier = Modifier.fillMaxWidth().testTag("terminal_keys_custom_label"),
                )
                OcTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = stringResource(R.string.terminal_keys_custom_text),
                    supportingText = stringResource(R.string.terminal_keys_custom_text_hint),
                    mono = true,
                    modifier = Modifier.fillMaxWidth().testTag("terminal_keys_custom_text"),
                )
            }
        },
        confirmButton = {
            OcButton(
                text = stringResource(R.string.terminal_keys_add),
                onClick = { onConfirm(BarKey.Text(label.trim(), decodeKeyText(text))) },
                enabled = valid,
                compact = true,
                modifier = Modifier.testTag("terminal_keys_custom_confirm"),
            )
        },
        dismissButton = {
            OcButton(
                text = stringResource(R.string.terminal_keys_cancel),
                onClick = onDismiss,
                variant = OcButtonVariant.Ghost,
                compact = true,
                modifier = Modifier.testTag("terminal_keys_custom_cancel"),
            )
        },
    )
}

private const val MAX_LABEL = 8
