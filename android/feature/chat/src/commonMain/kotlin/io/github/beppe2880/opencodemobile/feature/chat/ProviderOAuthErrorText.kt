package io.github.beppe2880.opencodemobile.feature.chat

import androidx.compose.runtime.Composable
import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthError
import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthStage
import io.github.beppe2880.opencodemobile.feature.chat.resources.Res
import io.github.beppe2880.opencodemobile.feature.chat.resources.oauth_error_callback
import io.github.beppe2880.opencodemobile.feature.chat.resources.oauth_error_exchange
import io.github.beppe2880.opencodemobile.feature.chat.resources.oauth_error_expired
import io.github.beppe2880.opencodemobile.feature.chat.resources.oauth_error_import
import io.github.beppe2880.opencodemobile.feature.chat.resources.oauth_error_prepare
import io.github.beppe2880.opencodemobile.feature.chat.resources.oauth_error_server_changed
import io.github.beppe2880.opencodemobile.feature.chat.resources.oauth_error_unknown
import io.github.beppe2880.opencodemobile.feature.chat.resources.oauth_error_validation
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun providerOAuthErrorText(error: ProviderOAuthError): String {
    val resource = when (error.stage) {
        ProviderOAuthStage.Preparation -> Res.string.oauth_error_prepare
        ProviderOAuthStage.Unknown -> Res.string.oauth_error_unknown
        ProviderOAuthStage.Callback -> Res.string.oauth_error_callback
        ProviderOAuthStage.TokenExchange -> Res.string.oauth_error_exchange
        ProviderOAuthStage.Import -> Res.string.oauth_error_import
        ProviderOAuthStage.Expired -> Res.string.oauth_error_expired
        ProviderOAuthStage.ServerChanged -> Res.string.oauth_error_server_changed
        else -> Res.string.oauth_error_validation
    }
    return stringResource(resource, error.diagnosticCode)
}
