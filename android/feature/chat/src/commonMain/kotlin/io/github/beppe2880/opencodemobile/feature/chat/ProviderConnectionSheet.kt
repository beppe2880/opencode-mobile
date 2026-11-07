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
import androidx.compose.foundation.text.selection.SelectionContainer
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
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.components.OcTextField
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_back
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_close
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_retry
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_provider_browser
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_provider_browser_waiting
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_provider_computer
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_provider_device
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_provider_secure
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_providers
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_providers_connected
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_providers_error
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_providers_hint
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_providers_key
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_providers_save
import org.jetbrains.compose.resources.stringResource
import io.github.beppe2880.opencodemobile.core.ui.resources.Res as UiRes

@Composable
internal fun ProviderConnectionSheet(state: ProviderConnectionState, onEvent: (ProviderConnectionEvent) -> Unit, onClose: () -> Unit) {
    val c = OcTheme.colors
    OcBottomSheet(onDismiss = onClose, testTag = "chat_providers_sheet") {
        if (state.selected == null && !state.connected) {
            ProviderPicker(state, onEvent, onClose)
            return@OcBottomSheet
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f),
            contentPadding = PaddingValues(horizontal = OcSpacing.lg, vertical = OcSpacing.md),
            verticalArrangement = Arrangement.spacedBy(OcSpacing.md),
        ) {
            item("header") {
                Column(verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                    Text(state.selected?.name ?: stringResource(Res.string.chat_providers), style = MaterialTheme.typography.titleMedium)
                    Text(state.serverName, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                    Text(
                        stringResource(Res.string.chat_providers_hint),
                        style = MaterialTheme.typography.bodySmall, color = c.textMuted,
                    )
                }
            }
            if (state.loading) item("loading") { OcSpinner() }
            if (state.failed) item("error") {
                Text(
                    state.browserError?.let { providerOAuthErrorText(it) } ?: stringResource(Res.string.chat_providers_error),
                    color = c.danger.fg, modifier = Modifier.testTag("provider_connection_error"),
                )
                if (state.selected == null || state.attempt != null) OcButton(
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
                val oauth = state.oauth
                if (state.attempt != null || state.browserUrl != null) {
                    item("oauth_waiting") {
                        SelectionContainer {
                            Text(
                                state.attempt?.instructions ?: stringResource(Res.string.chat_provider_browser_waiting),
                                modifier = Modifier.testTag("provider_oauth_instructions"),
                            )
                        }
                        OcButton(stringResource(Res.string.chat_provider_browser), {
                            onEvent(ProviderConnectionEvent.OpenBrowser)
                        }, modifier = Modifier.testTag("provider_oauth_browser"))
                        OcButton(stringResource(UiRes.string.oc_back), {
                            onEvent(ProviderConnectionEvent.Back)
                        }, modifier = Modifier.testTag("provider_oauth_cancel"), variant = OcButtonVariant.Secondary)
                    }
                } else {
                    if (oauth == null) {
                        items(selected.oauthMethods, key = { "method-${it.id}" }) { method ->
                            Column {
                                OcButton(
                                    method.label, {
                                        onEvent(ProviderConnectionEvent.ChooseOAuth(method.id))
                                    }, enabled = method.mobileSupported && !state.saving,
                                    modifier = Modifier.fillMaxWidth().testTag("provider_oauth_${method.id}"), variant = OcButtonVariant.Secondary,
                                )
                                if (!method.mobileSupported) Text(
                                    stringResource(if (method.requiresSecureTransport) Res.string.chat_provider_secure else Res.string.chat_provider_computer),
                                    style = MaterialTheme.typography.bodySmall, color = c.textMuted,
                                )
                            }
                        }
                    }
                    if (!state.saving) {
                        items((oauth?.form ?: selected).visibleFields(state.answers), key = { "field-${it.key}" }) { field ->
                            FormFieldInput(field, state.answers[field.key], { onEvent(ProviderConnectionEvent.Answer(field.key, it)) }, sensitive = true)
                        }
                    }
                    if (selected.hasKey && oauth == null) item("key") {
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
                            if (oauth != null) OcButton(
                                stringResource(if (oauth.browser) Res.string.chat_provider_browser else Res.string.chat_provider_device),
                                { onEvent(ProviderConnectionEvent.StartOAuth) },
                                enabled = oauth.form.isComplete(state.answers), loading = state.saving,
                                modifier = Modifier.weight(1f).testTag("provider_oauth_start"),
                            ) else if (selected.hasKey) OcButton(
                                stringResource(Res.string.chat_providers_save), { onEvent(ProviderConnectionEvent.Submit) },
                                enabled = state.key.isNotBlank() && selected.isComplete(state.answers), loading = state.saving,
                                modifier = Modifier.weight(1f).testTag("provider_connect"),
                            )
                        }
                    }
                }
            }
        }
    }
}
