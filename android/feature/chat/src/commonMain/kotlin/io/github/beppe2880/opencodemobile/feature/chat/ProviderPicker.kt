package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.KeyIntegration
import io.github.beppe2880.opencodemobile.core.ui.components.OcBadge
import io.github.beppe2880.opencodemobile.core.ui.components.OcButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcButtonVariant
import io.github.beppe2880.opencodemobile.core.ui.components.OcIconButton
import io.github.beppe2880.opencodemobile.core.ui.components.OcListRow
import io.github.beppe2880.opencodemobile.core.ui.components.OcSearchField
import io.github.beppe2880.opencodemobile.core.ui.components.OcSectionHeader
import io.github.beppe2880.opencodemobile.core.ui.components.OcSpinner
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_close
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_retry
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_providers
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_providers_empty
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_providers_error
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_providers_group_connected
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_providers_group_other
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_providers_group_popular
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_providers_hint
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_providers_search
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_providers_status_connected
import org.jetbrains.compose.resources.stringResource
import io.github.beppe2880.opencodemobile.core.ui.resources.Res as UiRes

// Same curated order as OpenCode's provider picker; discovery still owns availability and names.
private val popularProviders = listOf("opencode", "opencode-go", "openai", "github-copilot", "anthropic", "google")

internal enum class ProviderGroup { Connected, Popular, Other }

internal fun providerGroups(integrations: List<KeyIntegration>, query: String): Map<ProviderGroup, List<KeyIntegration>> {
    val search = query.trim()
    val matches = integrations.filter { it.name.contains(search, true) || it.id.contains(search, true) }
    return linkedMapOf(
        ProviderGroup.Connected to matches.filter { it.connected }.sortedBy { it.name.lowercase() },
        ProviderGroup.Popular to matches.filter { !it.connected && it.id in popularProviders }.sortedBy { popularProviders.indexOf(it.id) },
        ProviderGroup.Other to matches.filter { !it.connected && it.id !in popularProviders }.sortedBy { it.name.lowercase() },
    ).filterValues { it.isNotEmpty() }
}

@Composable
internal fun ProviderPicker(state: ProviderConnectionState, onEvent: (ProviderConnectionEvent) -> Unit, onClose: () -> Unit) {
    val c = OcTheme.colors
    val groups = providerGroups(state.integrations, state.query)
    val listState = rememberLazyListState()
    LaunchedEffect(state.query) { listState.scrollToItem(0) }

    Column(Modifier.fillMaxWidth().fillMaxHeight(0.8f)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = OcSpacing.lg, end = OcSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
                Text(stringResource(Res.string.chat_providers), style = MaterialTheme.typography.titleMedium)
                Text(state.serverName, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            }
            OcIconButton(Icons.Outlined.Close, stringResource(UiRes.string.oc_close), onClose, Modifier.testTag("provider_picker_close"))
        }
        Text(
            stringResource(Res.string.chat_providers_hint), style = MaterialTheme.typography.bodySmall, color = c.textMuted,
            modifier = Modifier.padding(horizontal = OcSpacing.lg, vertical = OcSpacing.sm),
        )
        OcSearchField(
            state.query, { onEvent(ProviderConnectionEvent.Search(it)) }, stringResource(Res.string.chat_providers_search),
            Modifier.padding(horizontal = OcSpacing.lg, vertical = OcSpacing.sm).testTag("provider_search"),
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).testTag("provider_picker_list"),
            contentPadding = PaddingValues(bottom = OcSpacing.lg),
        ) {
            if (state.loading) item("loading") { OcSpinner(Modifier.padding(OcSpacing.lg)) }
            if (state.failed) item("error") {
                Column(Modifier.padding(OcSpacing.lg), verticalArrangement = Arrangement.spacedBy(OcSpacing.sm)) {
                    Text(
                        stringResource(Res.string.chat_providers_error), color = c.danger.fg,
                        modifier = Modifier.testTag("provider_connection_error"),
                    )
                    OcButton(
                        stringResource(UiRes.string.oc_retry), { onEvent(ProviderConnectionEvent.Retry) },
                        modifier = Modifier.testTag("provider_retry"), variant = OcButtonVariant.Secondary,
                    )
                }
            }
            if (!state.loading && !state.failed && groups.isEmpty()) item("empty") {
                Text(stringResource(Res.string.chat_providers_empty), color = c.textMuted, modifier = Modifier.padding(OcSpacing.lg))
            }
            groups.forEach { (group, providers) ->
                item("group-$group") {
                    OcSectionHeader(
                        stringResource(
                            when (group) {
                                ProviderGroup.Connected -> Res.string.chat_providers_group_connected
                                ProviderGroup.Popular -> Res.string.chat_providers_group_popular
                                ProviderGroup.Other -> Res.string.chat_providers_group_other
                            },
                        ),
                        Modifier.testTag("provider_group_${group.name.lowercase()}").semantics { heading() },
                    )
                }
                items(providers, key = { it.id }) { provider ->
                    OcListRow(
                        title = provider.name,
                        onClick = { onEvent(ProviderConnectionEvent.Select(provider.id)) },
                        modifier = Modifier.testTag("provider_select_${provider.id}"),
                        leading = { ProviderListMark(provider.id) },
                        trailing = {
                            if (provider.connected) OcBadge(stringResource(Res.string.chat_providers_status_connected), c.success)
                        },
                    )
                }
            }
        }
    }
}

/** The provider's logo; providers without one get a generic icon in this list. */
@Composable
private fun ProviderListMark(id: String) {
    if (providerMarkResource(id) != null) {
        ProviderMark(id, id, size = 24.dp)
    } else {
        Icon(Icons.Outlined.Hub, contentDescription = null, modifier = Modifier.size(24.dp), tint = OcTheme.colors.iconMuted)
    }
}
