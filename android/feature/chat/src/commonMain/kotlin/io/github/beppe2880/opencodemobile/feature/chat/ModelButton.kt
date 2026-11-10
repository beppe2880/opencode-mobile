package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.domain.ModelRef
import io.github.beppe2880.opencodemobile.core.ui.components.OcModelName
import io.github.beppe2880.opencodemobile.core.ui.text.shortModelName
import io.github.beppe2880.opencodemobile.core.ui.text.variantLabel
import io.github.beppe2880.opencodemobile.core.ui.theme.OcRadius
import io.github.beppe2880.opencodemobile.core.ui.theme.OcSpacing
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_model
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_model_description
import io.github.beppe2880.opencodemobile.feature.chat.resources.chat_model_description_variant
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** What the model button shows: the [name] without the brand of the provider mark and the [variant] as the picker writes it. */
internal data class ModelButtonText(val name: String, val variant: String?)

internal fun modelButtonText(model: ModelRef, label: ModelLabel): ModelButtonText =
    ModelButtonText(shortModelName(label.name, model.providerId), model.variant?.let(::variantLabel))

/** A localized text and its arguments. */
internal data class ModelDescription(val resource: StringResource, val args: List<String>)

/**
 * What screen readers say for the model button: the full name and the provider, plus the effort
 * when a variant is picked ("Model: Claude Opus 5.5 (Anthropic), effort medium").
 */
internal fun modelDescription(model: ModelRef?, label: ModelLabel?): ModelDescription {
    if (model == null || label == null) return ModelDescription(Res.string.chat_model, emptyList())
    val variant = model.variant ?: return ModelDescription(Res.string.chat_model_description, listOf(label.name, label.provider))
    return ModelDescription(Res.string.chat_model_description_variant, listOf(label.name, label.provider, variant.lowercase()))
}

/**
 * The model of the message box, without border or background like the attach and microphone
 * buttons: the provider mark, the short model name and the variant in the muted color. Tapping
 * it opens the model picker. When space is short the variant is ellipsized first, then the name
 * ([OcModelName]); the mark keeps its size.
 */
@Composable
internal fun ModelButton(model: ModelRef?, label: ModelLabel?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = modelDescription(model, label).let { stringResource(it.resource, *it.args.toTypedArray()) }
    val text = if (model != null && label != null) modelButtonText(model, label) else null
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(OcRadius.md))
            .clickable(role = Role.Button, onClick = onClick)
            // Replaces the visible short texts for screen readers, which hear the full name.
            .semantics { contentDescription = description }
            .heightIn(min = 48.dp)
            .padding(horizontal = OcSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(OcSpacing.xs),
    ) {
        if (model != null && label != null) {
            ProviderMark(model.providerId, label.provider, Modifier.testTag("composer_model_mark"))
        }
        OcModelName(
            name = text?.name ?: stringResource(Res.string.chat_model),
            variant = text?.variant,
            modifier = Modifier.weight(1f, fill = false),
            nameModifier = Modifier.testTag("composer_model_name"),
            variantModifier = Modifier.testTag("composer_model_variant"),
            variantStyle = MaterialTheme.typography.bodySmall,
        )
    }
}
