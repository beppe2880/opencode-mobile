package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import io.github.beppe2880.opencodemobile.core.ui.components.OcBottomSheet
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcSearchField
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.components.OcTextField
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_back
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_close
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_retry
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_providers
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_providers_connected
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_providers_empty
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_providers_error
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_providers_hint
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_providers_key
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_providers_save
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_providers_search
import org.jetbrains.compose.resources.stringResource
import io.github.beppe2880.opencodemobile.core.ui.resources.Res as UiRes

@Composable
internal fun ProviderConnectionSheet(state: ProviderConnectionState, onEvent: (ProviderConnectionEvent) -> Unit, onClose: () -> Unit) {
    val c = OcTheme.colors
    OcBottomSheet(onDismiss = onClose, testTag = "chat_providers_sheet") {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
            contentPadding = PaddingValues(horizontal = OcSpacing.lg, vertical = OcSpacing.md),
            verticalArrangement = Arrangement.spacedBy(OcSpacing.md),
        ) {
            item("header") {
                Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                    Text(state.selected?.name ?: stringResource(Res.string.chat_providers), style = MaterialTheme.typography.titleMedium)
                    Text(state.serverName, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                    Text(stringResource(Res.string.chat_providers_hint), style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                }
            }
            if (state.loading) item("loading") { OcSpinner() }
            if (state.failed) item("error") {
                Text(stringResource(Res.string.chat_providers_error), color = c.danger.fg, modifier = Modifier.testTag("provider_connection_error"))
                if (state.selected == null) OcButton(
                    stringResource(UiRes.string.oc_retry), { onEvent(ProviderConnectionEvent.Retry) },
                    modifier = Modifier.testTag("provider_retry"), variant = OcButtonVariant.Secondary,
                )
            }
            val selected = state.selected
            if (state.connected) {
                item("success") {
                    Text(stringResource(Res.string.chat_providers_connected), modifier = Modifier.testTag("provider_connection_success"))
                    OcButton(stringResource(UiRes.string.oc_close), onClose, modifier = Modifier.testTag("provider_done"))
                }
            } else if (selected != null) {
                if (!state.saving) {
                    items(selected.visibleFields(state.answers), key = { "field-${it.key}" }) { field ->
                        FormFieldInput(field, state.answers[field.key], { onEvent(ProviderConnectionEvent.Answer(field.key, it)) }, sensitive = true)
                    }
                }
                item("key") {
                    OcTextField(
                        value = state.key,
                        onValueChange = { onEvent(ProviderConnectionEvent.KeyChanged(it)) },
                        label = selected.keyLabel ?: stringResource(Res.string.chat_providers_key),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                        visualTransformation = PasswordVisualTransformation(),
                        enabled = !state.saving,
                        modifier = Modifier.testTag("provider_api_key"),
                    )
                }
                item("submit") {
                    Row(horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                        OcButton(
                            stringResource(UiRes.string.oc_back), { onEvent(ProviderConnectionEvent.Back) },
                            modifier = Modifier.testTag("provider_back"), variant = OcButtonVariant.Secondary, enabled = !state.saving,
                        )
                        OcButton(
                            stringResource(Res.string.chat_providers_save), { onEvent(ProviderConnectionEvent.Submit) },
                            enabled = state.key.isNotBlank() && selected.isComplete(state.answers), loading = state.saving,
                            modifier = Modifier.weight(1f).testTag("provider_connect"),
                        )
                    }
                }
            } else {
                item("search") {
                    OcSearchField(state.query, {
                        onEvent(ProviderConnectionEvent.Search(it))
                    }, stringResource(Res.string.chat_providers_search), Modifier.testTag("provider_search"))
                }
                val matches = state.integrations.filter { it.name.contains(state.query, true) || it.id.contains(state.query, true) }
                if (!state.loading && !state.failed && matches.isEmpty()) item("empty") {
                    Text(stringResource(Res.string.chat_providers_empty), color = c.textMuted)
                }
                items(matches, key = { it.id }) { integration ->
                    OcButton(
                        integration.name, { onEvent(ProviderConnectionEvent.Select(integration.id)) },
                        variant = OcButtonVariant.Secondary, modifier = Modifier.fillMaxWidth().testTag("provider_select_${integration.id}"),
                    )
                }
            }
        }
    }
}
