package io.github.beppe2880.opencodemobile.feature.settings.customize

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.SttProvider
import io.github.beppe2880.opencodemobile.core.domain.TranscriptionError
import io.github.beppe2880.opencodemobile.core.domain.sttModel
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcListRow
import io.github.beppe2880.opencodemobile.core.ui.components.OcPasswordField
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.components.OcTextField
import io.github.beppe2880.opencodemobile.core.ui.components.exposeTestTags
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.settings.SettingsEvent
import io.github.beppe2880.opencodemobile.feature.settings.SettingsState
import io.github.beppe2880.opencodemobile.feature.settings.SttTest
import io.github.beppe2880.opencodemobile.feature.settings.resources.Res
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_base_url
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_base_url_hint
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_base_url_invalid
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_custom
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_key_optional
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_key_remove
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_key_save
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_key_saved
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_key_sub
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_model_apply
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_model_custom
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_model_default
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_model_load
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_model_other
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_test
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_test_address
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_test_network
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_test_no_key
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_test_passed
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_test_provider
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_test_rate_limited
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_test_running
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_test_too_large
import io.github.beppe2880.opencodemobile.feature.settings.resources.settings_voice_test_unauthorized
import org.jetbrains.compose.resources.stringResource

/** The name of a provider in Settings; brands are not translated. */
@Composable
internal fun SttProvider.displayName(): String = if (this == SttProvider.Custom) stringResource(Res.string.settings_voice_custom) else label

/** Whether [url] can be the address of a custom provider: http(s) with a host. */
internal fun isUsableBaseUrl(url: String): Boolean {
    val rest = url.trim().let {
        when {
            it.startsWith("https://", ignoreCase = true) -> it.drop(8)
            it.startsWith("http://", ignoreCase = true) -> it.drop(7)
            else -> return false
        }
    }
    return rest.substringBefore('/').substringBefore(':').isNotBlank() && rest.none { it.isWhitespace() }
}

/** The provider, from a menu, and the address of a custom one. */
@Composable
internal fun VoiceProviderControl(title: String, subtitle: String, state: SettingsState, onEvent: (SettingsEvent) -> Unit, modifier: Modifier) {
    val settings = state.settings
    Column(modifier.fillMaxWidth()) {
        MenuRow(
            title = title,
            subtitle = subtitle,
            value = settings.sttProvider.displayName(),
            tag = "settings_voice_provider",
            items = SttProvider.entries.map { provider ->
                MenuItem(provider.displayName(), "settings_voice_provider_${provider.name.lowercase()}") {
                    onEvent(SettingsEvent.SttProviderChanged(provider))
                }
            },
        )
        if (settings.sttProvider == SttProvider.Custom) {
            // Kept here while typing: the saved value comes back asynchronously.
            var url by remember { mutableStateOf(settings.sttBaseUrl) }
            OcTextField(
                value = url,
                onValueChange = {
                    url = it
                    onEvent(SettingsEvent.SttBaseUrlChanged(it))
                },
                label = stringResource(Res.string.settings_voice_base_url),
                placeholder = "https://example.com/v1",
                supportingText = stringResource(Res.string.settings_voice_base_url_hint),
                error = if (url.isNotBlank() && !isUsableBaseUrl(url)) stringResource(Res.string.settings_voice_base_url_invalid) else null,
                mono = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                modifier = Modifier
                    .padding(start = OcSpacing.lg, end = OcSpacing.lg, bottom = OcSpacing.md)
                    .testTag("settings_voice_base_url"),
            )
        }
    }
}

/** The model of the provider: its curated ones, those it listed, or any model ID typed in. */
@Composable
internal fun VoiceModelControl(title: String, subtitle: String, state: SettingsState, onEvent: (SettingsEvent) -> Unit, modifier: Modifier) {
    val settings = state.settings
    val provider = settings.sttProvider
    val current = settings.sttModel(provider)
    var typing by remember(provider) { mutableStateOf(false) }
    val models = (provider.models + state.sttListedModels[provider].orEmpty() + current).distinct()
    val defaultLabel = stringResource(Res.string.settings_voice_model_default, provider.defaultModel)
    Column(modifier.fillMaxWidth()) {
        MenuRow(
            title = title,
            subtitle = subtitle,
            value = current,
            tag = "settings_voice_model",
            loading = state.sttModelsLoading,
            items = models.mapIndexed { i, model ->
                MenuItem(if (model == provider.defaultModel) defaultLabel else model, "settings_voice_model_option_$i") {
                    typing = false
                    onEvent(SettingsEvent.SttModelChanged(if (model == provider.defaultModel) "" else model))
                }
            } + listOfNotNull(
                // Only OpenRouter lists its transcription models without a key.
                if (provider == SttProvider.OpenRouter && provider !in state.sttListedModels) {
                    MenuItem(stringResource(Res.string.settings_voice_model_load, provider.label), "settings_voice_model_load") {
                        onEvent(SettingsEvent.LoadSttModels)
                    }
                } else {
                    null
                },
                MenuItem(stringResource(Res.string.settings_voice_model_other), "settings_voice_model_other") { typing = true },
            ),
        )
        if (typing) {
            var model by remember { mutableStateOf(if (current in provider.models) "" else current) }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = OcSpacing.lg, end = OcSpacing.lg, bottom = OcSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
            ) {
                OcTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = stringResource(Res.string.settings_voice_model_custom),
                    placeholder = provider.defaultModel,
                    mono = true,
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Done),
                    modifier = Modifier.weight(1f).testTag("settings_voice_model_field"),
                )
                OcButton(
                    text = stringResource(Res.string.settings_voice_model_apply),
                    onClick = {
                        onEvent(SettingsEvent.SttModelChanged(model))
                        typing = false
                    },
                    enabled = model.isNotBlank(),
                    compact = true,
                    modifier = Modifier.testTag("settings_voice_model_apply"),
                )
            }
        }
    }
}

/**
 * The API key of the provider: typed and saved, then only "saved" is shown, never the key. Test
 * sends half a second of silence with the current settings.
 */
@Composable
internal fun VoiceKeyControl(title: String, state: SettingsState, onEvent: (SettingsEvent) -> Unit, modifier: Modifier) {
    val c = OcTheme.colors
    val provider = state.settings.sttProvider
    val saved = provider in state.sttKeys
    val name = provider.displayName()
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = OcSpacing.lg, vertical = OcSpacing.md),
        verticalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = c.textBase)
            Text(
                when {
                    saved -> stringResource(Res.string.settings_voice_key_saved, name)
                    provider == SttProvider.Custom -> stringResource(Res.string.settings_voice_key_optional)
                    else -> stringResource(Res.string.settings_voice_key_sub, name)
                },
                style = MaterialTheme.typography.bodySmall,
                color = c.textMuted,
                modifier = Modifier.testTag("settings_voice_key_status"),
            )
        }
        if (!saved) {
            // Only in memory while typed: never in saved instance state.
            var key by remember(provider) { mutableStateOf("") }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                OcPasswordField(
                    value = key,
                    onValueChange = { key = it },
                    label = title,
                    modifier = Modifier.weight(1f).testTag("settings_voice_key_field"),
                )
                OcButton(
                    text = stringResource(Res.string.settings_voice_key_save),
                    onClick = {
                        onEvent(SettingsEvent.SaveSttKey(key))
                        key = ""
                    },
                    enabled = key.isNotBlank(),
                    compact = true,
                    modifier = Modifier.testTag("settings_voice_key_save"),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
            OcButton(
                text = stringResource(if (state.sttTest == SttTest.Running) Res.string.settings_voice_test_running else Res.string.settings_voice_test),
                onClick = { onEvent(SettingsEvent.TestStt) },
                variant = OcButtonVariant.Secondary,
                compact = true,
                loading = state.sttTest == SttTest.Running,
                enabled = state.sttTest != SttTest.Running && (saved || provider == SttProvider.Custom),
                modifier = Modifier.testTag("settings_voice_test"),
            )
            if (saved) {
                OcButton(
                    text = stringResource(Res.string.settings_voice_key_remove),
                    onClick = { onEvent(SettingsEvent.RemoveSttKey) },
                    variant = OcButtonVariant.Secondary,
                    compact = true,
                    modifier = Modifier.testTag("settings_voice_key_remove"),
                )
            }
        }
        TestOutcome(state.sttTest)
    }
}

@Composable
private fun TestOutcome(test: SttTest?) {
    val c = OcTheme.colors
    val (ok, text) = when (test) {
        null, SttTest.Running -> return
        SttTest.Passed -> true to stringResource(Res.string.settings_voice_test_passed)
        is SttTest.Failed -> false to when (val error = test.error) {
            is TranscriptionError.NoKey -> stringResource(Res.string.settings_voice_test_no_key)
            is TranscriptionError.BadAddress -> stringResource(Res.string.settings_voice_test_address)
            is TranscriptionError.Unauthorized -> stringResource(Res.string.settings_voice_test_unauthorized)
            is TranscriptionError.RateLimited -> stringResource(Res.string.settings_voice_test_rate_limited)
            is TranscriptionError.TooLarge -> stringResource(Res.string.settings_voice_test_too_large)
            is TranscriptionError.Network -> stringResource(Res.string.settings_voice_test_network)
            is TranscriptionError.Provider -> stringResource(Res.string.settings_voice_test_provider, error.detail ?: "HTTP ${error.status}")
        }
    }
    Row(
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("settings_voice_test_result"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
    ) {
        Icon(
            if (ok) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = if (ok) c.success.fg else c.danger.fg,
            modifier = Modifier.size(16.dp),
        )
        Text(text, style = MaterialTheme.typography.bodySmall, color = if (ok) c.success.fg else c.danger.fg)
    }
}

internal class MenuItem(val label: String, val tag: String, val onClick: () -> Unit)

/** A setting whose value is picked from a menu under it. */
@Composable
private fun MenuRow(title: String, subtitle: String, value: String, tag: String, items: List<MenuItem>, loading: Boolean = false) {
    val c = OcTheme.colors
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OcListRow(
            title = title,
            subtitle = subtitle,
            onClick = { open = true },
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                    if (loading) OcSpinner()
                    Text(
                        value,
                        style = OcTheme.mono.codeSmall,
                        color = c.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis,
                        modifier = Modifier.widthIn(max = 180.dp),
                    )
                    Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = c.iconMuted, modifier = Modifier.size(18.dp))
                }
            },
            modifier = Modifier.testTag(tag),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = c.bgBase, modifier = Modifier.exposeTestTags()) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label, color = c.textBase) },
                    onClick = {
                        open = false
                        item.onClick()
                    },
                    modifier = Modifier.testTag(item.tag),
                )
            }
        }
    }
}
