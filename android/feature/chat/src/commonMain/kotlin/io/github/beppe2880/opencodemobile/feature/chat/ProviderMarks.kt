package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.beppe2880.opencodemobile.core.ui.components.OcProviderInitial
import io.github.beppe2880.opencodemobile.core.ui.theme.OcTheme
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.provider_anthropic
import io.github.beppe2880.opencodemobile.feature.chat.resources.provider_github_copilot
import io.github.beppe2880.opencodemobile.feature.chat.resources.provider_google
import io.github.beppe2880.opencodemobile.feature.chat.resources.provider_openai
import io.github.beppe2880.opencodemobile.feature.chat.resources.provider_opencode
import io.github.beppe2880.opencodemobile.feature.chat.resources.provider_opencode_go
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * The logo of a provider, by provider id; null when there is none. Keep these ids in sync with
 * the brands `shortModelName` (core:ui) strips: it only shortens names next to one of these marks.
 */
internal fun providerMarkResource(providerId: String): DrawableResource? = when (providerId) {
    "opencode" -> Res.drawable.provider_opencode
    "opencode-go" -> Res.drawable.provider_opencode_go
    "openai" -> Res.drawable.provider_openai
    "github-copilot" -> Res.drawable.provider_github_copilot
    "anthropic" -> Res.drawable.provider_anthropic
    "google" -> Res.drawable.provider_google
    else -> null
}

/** The provider's logo tinted with the icon color, or the initial of [providerName] when it has none. Decorative. */
@Composable
internal fun ProviderMark(providerId: String, providerName: String, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    val resource = providerMarkResource(providerId)
    if (resource != null) {
        Icon(painterResource(resource), contentDescription = null, modifier = modifier.size(size), tint = OcTheme.colors.iconBase)
    } else {
        OcProviderInitial(providerName.ifBlank { providerId }, modifier, size)
    }
}
