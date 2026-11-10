package io.github.beppe2880.opencodemobile.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.Agent
import io.github.beppe2880.opencodemobile.core.domain.AgentMode
import io.github.beppe2880.opencodemobile.core.domain.ModelInfo
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.ui.resources.Res
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_back
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_models_empty
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_models_recent
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_models_search
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_models_title
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_models_variant
import io.github.beppe2880.opencodemobile.core.ui.resources.oc_models_variant_default
import io.github.beppe2880.opencodemobile.core.ui.text.formatTokens
import io.github.beppe2880.opencodemobile.core.ui.text.shortModelName
import io.github.beppe2880.opencodemobile.core.ui.text.variantLabel
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import org.jetbrains.compose.resources.stringResource

/** Agents the user can pick as primary: not hidden and not subagent-only. */
fun List<Agent>.selectablePrimary(): List<Agent> = filter { !it.hidden && it.mode != AgentMode.Subagent }

/** Horizontally scrolling agent chips with their agent color. */
@Composable
fun OcAgentChips(
    agents: List<Agent>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "agent",
) {
    val c = OcTheme.colors
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.sm),
    ) {
        agents.forEach { agent ->
            OcChip(
                text = agent.name.replaceFirstChar { it.uppercase() },
                selected = agent.id == selected,
                onClick = { onSelect(agent.id) },
                dotColor = c.agents.forAgent(agent.id),
                // A bounded width: in this horizontally scrolling row the chip's weighted label
                // would otherwise get no space at all.
                modifier = Modifier.widthIn(max = 240.dp).testTag("${testTagPrefix}_${agent.id}"),
            )
        }
    }
}

/**
 * Searchable model list grouped by provider; [recent] models are listed first.
 * Filter text is local UI state. With a [providerMark] (provider id, provider name) each row
 * starts with it and shows the short model name ([shortModelName]), plus the selected variant.
 */
@Composable
fun OcModelPicker(
    models: List<ModelInfo>,
    selected: ModelRef?,
    recent: List<ModelRef>,
    onSelect: (ModelRef) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onSelectVariant: ((String?) -> Unit)? = null,
    providerMark: (@Composable (providerId: String, providerName: String) -> Unit)? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val sections = remember(models, recent, query) { modelSections(models, recent, query) }
    val recentLabel = stringResource(Res.string.oc_models_recent)
    val c = OcTheme.colors
    val variants = selected?.let { ref -> models.firstOrNull { it.ref.key == ref.key }?.variants }.orEmpty()
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = OcSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                OcIconButton(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    stringResource(Res.string.oc_back),
                    onBack,
                    modifier = Modifier.testTag("model_picker_back_button"),
                )
            }
            Text(
                text = stringResource(Res.string.oc_models_title),
                style = MaterialTheme.typography.titleSmall,
                color = c.textBase,
                modifier = Modifier.padding(start = if (onBack == null) OcSpacing.sm else 0.dp),
            )
        }
        if (onSelectVariant != null && variants.isNotEmpty()) {
            VariantRow(variants, selected?.variant, onSelectVariant)
        }
        OcSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(Res.string.oc_models_search),
            modifier = Modifier.padding(horizontal = OcSpacing.lg, vertical = OcSpacing.sm).testTag("model_picker_search_field"),
        )
        val listState = rememberLazyListState()
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
            if (sections.isEmpty()) {
                item("empty") {
                    Text(
                        stringResource(Res.string.oc_models_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textMuted,
                        modifier = Modifier.padding(OcSpacing.lg),
                    )
                }
            }
            sections.forEach { section ->
                item(key = "h-${section.key}") {
                    OcSectionHeader(section.title ?: recentLabel)
                }
                items(section.models, key = { "${section.key}-${it.ref.key}" }) { model ->
                    val isSelected = model.ref.key == selected?.key
                    ModelRow(
                        model = model,
                        selected = isSelected,
                        variant = selected?.variant?.takeIf { isSelected },
                        mark = providerMark,
                        onClick = { onSelect(model.ref) },
                    )
                }
            }
        }
    }
}

/** Reasoning level of the selected model; null is the model's default. */
@Composable
private fun VariantRow(variants: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    val c = OcTheme.colors
    Column(Modifier.padding(top = OcSpacing.xs), verticalArrangement = Arrangement.spacedBy(OcSpacing.xs)) {
        Text(
            stringResource(Res.string.oc_models_variant),
            style = MaterialTheme.typography.labelMedium,
            color = c.textMuted,
            modifier = Modifier.padding(horizontal = OcSpacing.lg),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = OcSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
        ) {
            (listOf<String?>(null) + variants).forEach { variant ->
                val isSelected = variant == selected
                OcChip(
                    text = variant?.let(::variantLabel) ?: stringResource(Res.string.oc_models_variant_default),
                    onClick = { onSelect(variant) },
                    selected = isSelected,
                    leadingIcon = if (isSelected) Icons.Outlined.Check else null,
                    // Bounded width: OcChip's label is weighted and collapses inside a horizontal scroll.
                    modifier = Modifier.widthIn(max = 160.dp).testTag("model_variant_${variant ?: "default"}"),
                )
            }
        }
    }
}

@Composable
private fun ModelRow(
    model: ModelInfo,
    selected: Boolean,
    variant: String?,
    mark: (@Composable (providerId: String, providerName: String) -> Unit)?,
    onClick: () -> Unit,
) {
    val c = OcTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { this.selected = selected }
            .clickable(role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = OcSpacing.lg, vertical = OcSpacing.sm)
            .testTag("model_option_${model.ref.key}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.md),
    ) {
        if (mark != null) mark(model.providerId, model.providerName)
        Column(modifier = Modifier.weight(1f)) {
            if (mark == null) {
                Text(model.name, style = MaterialTheme.typography.bodyMedium, color = c.textBase, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                OcModelName(name = shortModelName(model.name, model.providerId), variant = variant?.let(::variantLabel))
            }
            Text(
                text = "${model.providerName} · ${model.ref.id} · ${formatTokens(model.contextLimit)}",
                style = OcTheme.mono.codeSmall,
                color = c.textFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) Icon(Icons.Outlined.Check, contentDescription = null, tint = c.textAccent, modifier = Modifier.size(18.dp))
    }
}

/**
 * A model [name] followed by its [variant] in the muted color, on one line. When space is short
 * the variant is ellipsized first, then the name: the name is measured first without a weight
 * and takes what it needs, the variant only gets the width that is left.
 */
@Composable
fun OcModelName(
    name: String,
    variant: String?,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    nameModifier: Modifier = Modifier,
    variantModifier: Modifier = Modifier,
    variantStyle: TextStyle = style,
) {
    val c = OcTheme.colors
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // The variant shows, ellipsized if needed, only when at least its first letters fit next to
    // the whole name: a bare "…" says nothing (screen readers get the full label anyway).
    val variantMinWidth = remember(variant, variantStyle, density) {
        variant?.let { measurer.measure(it.take(VARIANT_MIN_CHARS) + "…", variantStyle, maxLines = 1).size.width }
    }
    val gap = with(density) { OcSpacing.xs.roundToPx() }
    Layout(
        modifier = modifier,
        content = {
            Text(name, style = style, color = c.textBase, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = nameModifier)
            if (variant != null) {
                Text(variant, style = variantStyle, color = c.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = variantModifier)
            }
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0)
        val namePlaceable = measurables[0].measure(loose)
        val room = constraints.maxWidth - namePlaceable.width - gap
        val variantPlaceable = measurables.getOrNull(1)?.takeIf { variantMinWidth != null && room >= variantMinWidth }
            ?.measure(loose.copy(maxWidth = room))
        val width = namePlaceable.width + (variantPlaceable?.let { gap + it.width } ?: 0)
        val height = maxOf(namePlaceable.height, variantPlaceable?.height ?: 0)
        layout(width.coerceAtLeast(constraints.minWidth), height) {
            namePlaceable.placeRelative(0, (height - namePlaceable.height) / 2)
            variantPlaceable?.placeRelative(namePlaceable.width + gap, (height - variantPlaceable.height) / 2)
        }
    }
}

/** Letters of the variant that must fit for it to show ("Med…"). */
private const val VARIANT_MIN_CHARS = 3

private const val RECENT = "\u0000recent"

/** [title] is the provider display name; null for the localized "Recent" section. */
private data class ModelSection(val key: String, val title: String?, val models: List<ModelInfo>)

private fun modelSections(models: List<ModelInfo>, recent: List<ModelRef>, query: String): List<ModelSection> {
    val q = query.trim().lowercase()
    val visible = models.filter { it.enabled }.filter {
        q.isEmpty() || it.name.lowercase().contains(q) || it.ref.key.lowercase().contains(q) ||
            it.providerName.lowercase().contains(q)
    }
    val recentKeys = recent.map { it.key }.distinct()
    val recentModels = recentKeys.mapNotNull { key -> visible.firstOrNull { it.ref.key == key } }
    val byProvider = visible.groupBy { it.providerId }.entries.sortedBy { it.value.first().providerName.lowercase() }
    return buildList {
        if (recentModels.isNotEmpty()) add(ModelSection(RECENT, null, recentModels))
        byProvider.forEach { (provider, list) -> add(ModelSection(provider, list.first().providerName, list)) }
    }
}
